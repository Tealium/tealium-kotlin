package com.tealium.core.persistence

import com.tealium.dispatcher.Dispatch

internal interface VisitorStorage {
    var currentVisitorId: String?
    var currentIdentity: String?

    fun getVisitorId(identity: String): String?
    fun saveVisitorId(identity: String, visitorId: String, shouldHash: Boolean = true)
    fun clear()
}

internal class DefaultVisitorStorage(
    private val storage: KeyValueDao<String, PersistentItem>,
    private val hashingProvider: HashingProvider = DefaultHashingProvider
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
                save(KEY_CURRENT_IDENTITY, hash(it))
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
     * Hashes the [identity] to use as a lookup key
     */
    override fun getVisitorId(identity: String): String? {
        return storage.get(hash(identity))?.value
    }

    /**
     * Hashes the [identity] to use as the key to store the [visitorId] against
     */
    override fun saveVisitorId(identity: String, visitorId: String, shouldHash: Boolean) {
        val visitorItem = PersistentItem(
            key = if (shouldHash) hash(identity) else identity,
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

    private fun hash(input: String): String {
        return input.sha256()
    }

    companion object {
        const val KEY_CURRENT_IDENTITY = "current_identity"
    }
}