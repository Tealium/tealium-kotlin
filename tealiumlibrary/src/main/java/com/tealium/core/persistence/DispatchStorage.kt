package com.tealium.core.persistence

import com.tealium.core.messaging.LibrarySettingsUpdatedListener
import com.tealium.core.settings.LibrarySettings
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.JsonDispatch
import org.json.JSONObject

internal class DispatchStorage(dbHelper: DatabaseHelper,
                               tableName: String)
    : LibrarySettingsUpdatedListener,
    QueueingDao<String, Dispatch> {

    private val dao = DispatchStorageDao(dbHelper, tableName)

    override fun enqueue(item: Dispatch) {
        dao.enqueue(convertToPersistentItem(item))
    }

    override fun enqueue(items: List<Dispatch>) {
        val list = items.map { convertToPersistentItem(it) }
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
        return dao.getAll().mapValues { (_, value) ->
            convertToDispatch(value)
        }
    }

    override fun insert(item: Dispatch) {
        dao.insert(
            convertToPersistentItem(item)
        )
    }

    override fun update(item: Dispatch) {
        dao.update(
            convertToPersistentItem(item)
        )
    }

    override fun delete(key: String) {
        dao.delete(key)
    }

    override fun upsert(item: Dispatch) {
        dao.upsert(
            convertToPersistentItem(item)
        )
    }

    override fun clear() = dao.clear()
    override fun keys(): List<String> = dao.keys()
    override fun count(): Int = dao.count()
    override fun contains(key: String): Boolean = dao.contains(key)
    override fun purgeExpired() = dao.purgeExpired()

    private fun convertToDispatch(json: PersistentItem): Dispatch {
        return JsonDispatch(json)
    }

    private fun convertToPersistentItem(dispatch: Dispatch): PersistentItem {
        val payload = dispatch.payload()
        return PersistentItem(
            dispatch.id,
            Serdes.jsonObjectSerde().serializer.serialize(JSONObject(payload)),
            null,
            dispatch.timestamp,
            Serialization.JSON_OBJECT
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