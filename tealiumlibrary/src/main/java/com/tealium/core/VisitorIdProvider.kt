package com.tealium.core

import com.tealium.core.persistence.DataLayer
import com.tealium.core.persistence.Expiry
import com.tealium.core.persistence.VisitorStorage
import com.tealium.core.persistence.sha256
import com.tealium.dispatcher.Dispatch
import com.tealium.tealiumlibrary.BuildConfig
import java.util.*

internal class VisitorIdProvider(
    private val existingVisitorId: String?,
    private val visitorIdKey: String?,
    private val visitorStorage: VisitorStorage,
    private val dataLayer: DataLayer,
    private val onVisitorIdUpdated: (String) -> Unit,
) : DataLayer.DataLayerUpdatedListener {

    constructor(
        config: TealiumConfig,
        visitorStorage: VisitorStorage,
        dataLayer: DataLayer,
        onVisitorIdUpdated: (String) -> Unit
    ) : this(
        config.existingVisitorId,
        config.visitorIdentityKey,
        visitorStorage,
        dataLayer,
        onVisitorIdUpdated
    )

    /**
     * Whether construction has completed. Declared before [currentVisitorId] so that it is
     * initialized before the [currentVisitorId] initializer can invoke the setter.
     */
    @Volatile
    private var initialized = false

    /**
     * Holds a visitor id change that happened during construction, before any listeners could be
     * subscribed. Consumed via [consumePendingVisitorIdUpdate].
     */
    @Volatile
    private var pendingVisitorIdUpdate: String? = null

    var currentVisitorId: String = getOrCreateVisitorId()
        private set(value) {
            if (field != value) {
                field = value
                visitorStorage.currentVisitorId = value
                visitorStorage.currentIdentity?.let { currentIdentity ->
                    visitorStorage.saveVisitorId(currentIdentity, currentVisitorId)
                }
                putInDataLayer(value)
                if (initialized) {
                    onVisitorIdUpdated(value)
                } else {
                    // Listeners are subscribed after this object is constructed, so notifying now
                    // would be a race; record it for the owner to deliver once subscribed.
                    pendingVisitorIdUpdate = value
                }
            }
        }

    init {
        // The DataLayer entry is authoritative for the visitor id. Events are attributed using
        // the "tealium_visitor_id" value found in the DataLayer, and amending it there is
        // documented as affecting attribution (see Tealium.visitorId). Installs that migrated
        // from the legacy library before 1.10.0 can be left with a generated id in
        // [visitorStorage] but the legacy id in the DataLayer; reconciling to the DataLayer value
        // keeps consumers of [currentVisitorId] (VisitorService, Moments API) in line with the id
        // that events have already been attributed to.
        // Reconciled before [retrieveIdentityFromDataLayer] so that any stored identity is
        // relinked to the reconciled id, and so that a genuine identity change detected in the
        // DataLayer still takes precedence and resets the visitor id afterward.
        //
        // Any visitor id change made here happens before listeners have been subscribed, so
        // [onVisitorIdUpdated] is deferred: the new id is held in [pendingVisitorIdUpdate] and the
        // owner delivers it through [consumePendingVisitorIdUpdate] once subscriptions are in
        // place. Once [initialized] is true, changes notify immediately as before.
        val dataLayerVisitorId = dataLayer.getString(Dispatch.Keys.TEALIUM_VISITOR_ID)
        when {
            dataLayerVisitorId.isNullOrEmpty() -> putInDataLayer(currentVisitorId)
            dataLayerVisitorId != currentVisitorId -> currentVisitorId = dataLayerVisitorId
        }

        retrieveIdentityFromDataLayer()

        initialized = true
    }

    /**
     * Returns any visitor id change that occurred during construction - before listeners could be
     * subscribed - and clears it. Returns null if the visitor id did not change, or if the change
     * has already been consumed.
     */
    internal fun consumePendingVisitorIdUpdate(): String? {
        return pendingVisitorIdUpdate.also { pendingVisitorIdUpdate = null }
    }

    fun resetVisitorId(): String {
        Logger.dev(BuildConfig.TAG, "Resetting current visitor id")

        val newId = generateVisitorId()
        currentVisitorId = newId
        return newId
    }

    fun clearStoredVisitorIds() {
        Logger.dev(BuildConfig.TAG, "Clearing stored visitor ids")
        visitorStorage.clear()
        resetVisitorId()

        // required to stop stitching issues
        retrieveIdentityFromDataLayer()
    }

    override fun onDataUpdated(key: String, value: Any) {
        if (key == visitorIdKey) {
            (value as? String)?.let { newIdentity ->
                if (newIdentity.isNotBlank()) {
                    changeIdentity(newIdentity)
                }
            }
        }
    }

    private fun changeIdentity(newIdentity: String) {

        // Set current identity
        val oldIdentity = visitorStorage.currentIdentity
        val hashedNewIdentity = newIdentity.sha256()

        if (hashedNewIdentity != oldIdentity) {
            Logger.dev(BuildConfig.TAG, "Identity change has been detected.")
            visitorStorage.currentIdentity = hashedNewIdentity
        }

        // check for known matching visitor id
        val knownVisitorId = visitorStorage.getVisitorId(hashedNewIdentity)
        if (knownVisitorId != null) {
            if (knownVisitorId != currentVisitorId) {
                Logger.dev(
                    BuildConfig.TAG,
                    "Identity has been seen before; setting known visitor id"
                )
                // visitor is known - update current visitor id
                currentVisitorId = knownVisitorId
            }
        } else if (oldIdentity == null) {
            Logger.dev(BuildConfig.TAG, "Identity unknown; linking to current visitor id")
            // no identity yet - link with current anonymous id
            visitorStorage.saveVisitorId(hashedNewIdentity, currentVisitorId)
        } else {
            Logger.dev(BuildConfig.TAG, "Identity unknown; resetting visitor id")
            // we have updated identity - need to update linked visitor id
            resetVisitorId()
        }
    }

    override fun onDataRemoved(keys: Set<String>) {
        // nothing to do
    }

    private fun retrieveIdentityFromDataLayer() {
        visitorIdKey?.let { idKey ->
            dataLayer.getString(idKey)?.let { dataLayerIdentity ->
                onDataUpdated(idKey, dataLayerIdentity)
            }
        }
    }

    private fun getOrCreateVisitorId(): String {
        return visitorStorage.currentVisitorId
            ?: (dataLayer.getString(Dispatch.Keys.TEALIUM_VISITOR_ID)   // previously saved visitor id
                ?: existingVisitorId                                    // known existing Id
                ?: generateVisitorId()).also {
                // notify of new visitor id
                currentVisitorId = it
            }
    }

    private fun putInDataLayer(visitorId: String) {
        // compatibility
        dataLayer.putString(Dispatch.Keys.TEALIUM_VISITOR_ID, visitorId, Expiry.FOREVER)
    }

    companion object {
        private fun generateVisitorId(uuid: UUID = UUID.randomUUID()): String {
            return uuid.toString().replace("-", "")
        }
    }
}
