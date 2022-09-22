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

    var currentVisitorId: String = getOrCreateVisitorId()
        private set(value) {
            if (field != value) {
                field = value
                visitorStorage.currentVisitorId = value
                visitorStorage.currentIdentity?.let { currentIdentity ->
                    // currentIdentity is already hashed
                    visitorStorage.saveVisitorId(currentIdentity, currentVisitorId, false)
                }
                putInDataLayer(value)
                onVisitorIdUpdated(value)
            }
        }

    init {
        retrieveIdentityFromDataLayer()
        if (dataLayer.getString(Dispatch.Keys.TEALIUM_VISITOR_ID) == null) {
            putInDataLayer(currentVisitorId)
        }
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
        if (newIdentity.sha256() == oldIdentity) {
            return
        }
        Logger.dev(BuildConfig.TAG, "Identity change has been detected.")
        visitorStorage.currentIdentity = newIdentity

        // check for known matching visitor id
        val knownVisitorId = visitorStorage.getVisitorId(newIdentity)
        if (knownVisitorId != null && knownVisitorId != currentVisitorId) {
            Logger.dev(BuildConfig.TAG, "Identity has been seen before; setting known visitor id")
            // visitor is know - update current visitor id
            currentVisitorId = knownVisitorId
        } else if (oldIdentity == null) {
            Logger.dev(BuildConfig.TAG, "Identity unknown; linking to current visitor id")
            // no identity yet - link with current anonymous id
            visitorStorage.saveVisitorId(newIdentity, currentVisitorId)
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