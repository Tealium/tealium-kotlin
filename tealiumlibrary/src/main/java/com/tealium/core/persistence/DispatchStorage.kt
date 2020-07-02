package com.tealium.core.persistence

import com.tealium.core.messaging.LibrarySettingsUpdatedListener
import com.tealium.core.model.LibrarySettings
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.JsonDispatch
import org.json.JSONObject

internal class DispatchStorage(dbHelper: DatabaseHelper,
                               tableName: String)
    : LibrarySettingsUpdatedListener,
        QueueingDao<String, Dispatch> {

    private val dao = DispatchStorageDao(dbHelper, tableName)

    override fun enqueue(item: Dispatch) {
        dao.enqueue(convertToPersistentJson(item))
    }

    override fun enqueue(items: List<Dispatch>) {
        val list = items.map { convertToPersistentJson(it) }
        dao.enqueue(list)
    }

    override fun dequeue(): Dispatch? {
        return dao.dequeue()?.let {
            convertToDispatch(it)
        }
    }

    override fun dequeue(count: Int): List<Dispatch> {
        return dao.dequeue(count).map {
            convertToDispatch(it)
        }
    }

    override fun resize(size: Int) {
        dao.resize(size)
    }

    override fun get(key: String): Dispatch? {
        return dao.get(key)?.let {
            convertToDispatch(it)
        }
    }

    override fun getAll(): Map<String, Dispatch> {
        return dao.getAll().mapValues { (key, value) ->
            convertToDispatch(value)
        }
    }

    override fun insert(item: Dispatch) {
        dao.insert(
                convertToPersistentJson(item)
        )
    }

    override fun update(item: Dispatch) {
        dao.update(
                convertToPersistentJson(item)
        )
    }

    override fun delete(key: String) {
        dao.delete(key)
    }

    override fun upsert(item: Dispatch) {
        dao.upsert(
                convertToPersistentJson(item)
        )
    }

    override fun clear() = dao.clear()
    override fun keys(): List<String> = dao.keys()
    override fun count(): Int = dao.count()
    override fun contains(key: String): Boolean = dao.contains(key)
    override fun purgeExpired() = dao.purgeExpired()

    private fun convertToDispatch(json: PersistentJsonObject): Dispatch {
        return JsonDispatch(json)
    }

    private fun convertToPersistentJson(dispatch: Dispatch): PersistentJsonObject {
        val payload = dispatch.payload()
        return PersistentJsonObject(
                dispatch.id,
                JSONObject(payload),
                null,
                payload["tealium_timestamp"] as? Long
        )
    }

    override fun onLibrarySettingsUpdated(settings: LibrarySettings) {
        if (dao.maxQueueSize != settings.batching.maxQueueSize) {
            dao.resize(settings.batching.maxQueueSize)
        }
        if (dao.expiryDays != settings.batching.expiration) {
            dao.expiryDays = settings.batching.expiration
        }
    }
}