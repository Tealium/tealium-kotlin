package com.tealium.core.persistence

import com.tealium.core.JsonUtils
import com.tealium.core.messaging.LibrarySettingsUpdatedListener
import com.tealium.core.settings.LibrarySettings
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.JsonDispatch
import java.util.concurrent.ConcurrentLinkedQueue

internal class DispatchStorage(
    dbHelper: DatabaseHelper,
    tableName: String,
    val queue: ConcurrentLinkedQueue<Dispatch> = ConcurrentLinkedQueue<Dispatch>(),
    private val dao: DispatchStorageDao = DispatchStorageDao(dbHelper, tableName)
) : LibrarySettingsUpdatedListener,
    QueueingDao<String, Dispatch> {

    override fun enqueue(item: Dispatch) {
        dao.enqueue(convertToPersistentItem(item))
        val db = dao.db
        if (db == null || db.isReadOnly) {
            queue.add(item)
        }
    }

    override fun enqueue(items: List<Dispatch>) {
        val list = items.map { convertToPersistentItem(it) }
        dao.enqueue(list)

        val db = dao.db
        if (db == null || db.isReadOnly) {
            queue.addAll(items)
        }
    }

    override fun dequeue(): Dispatch? {
        if (queue.isNotEmpty()) {
            val dispatch = queue.poll()
            dispatch?.let {
                dao.delete(it.id)
            }
            return dispatch
        }

        return dao.dequeue()?.let {
            convertToDispatch(it)
        }
    }

    /**
     * Pops the first [count] items off the queue; it reads then deletes.
     * @param count limits the number of items to dequeue off the list; negative numbers will
     * dequeue all currently queued items.
     */
    override fun dequeue(count: Int): List<Dispatch> {
        if (queue.isNotEmpty()) {
            val list = mutableListOf<Dispatch>()
            return if (count > 0) {
                for (i in 0 until count) {
                    queue.poll()?.let {
                        dao.delete(it.id)
                        list.add(it)
                    }
                }
                list
            } else {
                queue.forEach {
                    dao.delete(it.id)
                    list.add(it)
                }
                queue.clear()
                list
            }
        }

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

    internal fun convertToDispatch(json: PersistentItem): Dispatch {
        return JsonDispatch(json)
    }

    internal fun convertToPersistentItem(dispatch: Dispatch): PersistentItem {
        val payload = dispatch.payload()
        return PersistentItem(
            dispatch.id,
            Serdes.jsonObjectSerde().serializer.serialize(JsonUtils.jsonFor(payload)),
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