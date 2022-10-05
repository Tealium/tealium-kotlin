package com.tealium.core.persistence

import com.tealium.dispatcher.Dispatch

internal interface VisitorStorage {
    var currentVisitorId: String?
    var currentIdentity: String?

    fun getVisitorId(identity: String): String?
    fun saveVisitorId(identity: String, visitorId: String)
    fun clear()
}

internal class DefaultVisitorStorage(
    private val storage: KeyValueDao<String, PersistentItem>,
): VisitorStorage {

    internal constructor(
        dbHelper: DatabaseHelper,
    ) : this(PersistentStorageDao(dbHelper, "visitors"))

    override var currentVisitorId: String?
        get() = storage.get(Dispatch.Keys.TEALIUM_VISITOR_ID)?.value
        set(value) {
            value?.let {
                save(Dispatch.Keys.TEALIUM_VISITOR_ID, it)
            }
        }

    override var currentIdentity: String?
        get() = storage.get(KEY_CURRENT_IDENTITY)?.value
        set(value) {
            value?.let {
                save(KEY_CURRENT_IDENTITY, it)
            }
        }

    private fun save(key: String, value: String) {
        storage.upsert(
            PersistentItem(
                key = key,
                value = value,
                expiry = Expiry.FOREVER,
                type = Serialization.STRING
            )
        )
    }

    /**
     * Uses the [identity] as a lookup key
     */
    override fun getVisitorId(identity: String): String? {
        return storage.get(identity)?.value
    }

    /**
     * Uses the [identity] as the key to store the [visitorId] against
     */
    override fun saveVisitorId(identity: String, visitorId: String) {
        val visitorItem = PersistentItem(
            key = identity,
            value = visitorId,
            expiry = Expiry.FOREVER,
            type = Serialization.STRING
        )
        storage.upsert(visitorItem)
    }

    /**
     * Removes all stored identifiers
     */
    override fun clear() {
        // Should be fine to clear the whole lot since the calling method
        // is expected to reset to a new visitor id and possibly the current identity anyway
        storage.clear()
    }

    companion object {
        const val KEY_CURRENT_IDENTITY = "current_identity"
    }
}