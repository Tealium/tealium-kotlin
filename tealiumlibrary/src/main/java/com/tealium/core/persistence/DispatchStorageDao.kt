package com.tealium.core.persistence

import com.tealium.core.persistence.*
import com.tealium.core.persistence.SqlDataLayer.Columns.COLUMN_EXPIRY
import com.tealium.core.persistence.SqlDataLayer.Columns.COLUMN_KEY
import com.tealium.core.persistence.SqlDataLayer.Columns.COLUMN_TIMESTAMP
import com.tealium.core.persistence.SqlDataLayer.Columns.COLUMN_VALUE
import java.util.concurrent.TimeUnit

/**
 * Dispatch Storage is a specialized Key/Value store where all items are represented as stringified
 * JSON.
 * Items are pushed and popped from the queue in timestamp order, and then First-In-First-Out (FIFO)
 * order if more than one entry has the same timestamp. Defaults for Expiry time and Timestamp will
 * be generated if missing from the [PersistentItem] being pushed onto the queue.
 */
internal class DispatchStorageDao(
    private val dbHelper: DatabaseHelper,
    private val tableName: String,
    maxQueueSize: Int = -1,
    expiryDays: Int = -1,
    private val kvDao: KeyValueDao<String, PersistentItem> = PersistentStorageDao(dbHelper, tableName, false)
) : QueueingDao<String, PersistentItem>, KeyValueDao<String, PersistentItem> by kvDao {

    val db = dbHelper.writableDatabase

    /**
     * Sets the maximum number of items allowed in the queue.
     */
    var maxQueueSize: Int = maxQueueSize
        private set(value) {
            if (value >= -1) field = value
        }

    /**
     * Sets the Expiry time in days of each future dispatch.
     */
    var expiryDays: Int = expiryDays
        set(value) {
            if (value >= -1) field = value
        }

    /**
     * Inserts the [item] by pushing it onto the queue.
     */
    override fun insert(item: PersistentItem) {
        enqueue(item)
    }

    /**
     * Updates and existing item using [item].
     */
    override fun update(item: PersistentItem) {
        enqueue(item)
    }

    /**
     * Updates/Inserts the [item].
     */
    override fun upsert(item: PersistentItem) {
        enqueue(item)
    }

    /**
     * Asynchronously pushes each a single item onto the back of the queue. It will free up
     * space in the queue if required, by removing the oldest items first.
     *
     * Executed in the context of a single database thread, to ensure data consistency.
     */
    override fun enqueue(item: PersistentItem) {
//        launch(Logger.exceptionHandler) {
        createSpaceIfRequired(1)
        setItemDefaults(item)
        kvDao.upsert(item)
//        }
    }

    /**
     * Asynchronously pushes each item in [items] onto the back of the queue. It will free up
     * space in the queue if required, by removing the oldest items first.
     *
     * Executed in the context of a single database thread, to ensure data consistency.
     */
    override fun enqueue(items: List<PersistentItem>) {
//        launch(Logger.exceptionHandler) {
        createSpaceIfRequired(items.count())
        items.forEach {
            setItemDefaults(it)
            kvDao.upsert(it)
        }
//        }
    }

    /**
     * Pops the first unexpired item off the queue; it reads then deletes.
     * Executed in the context of a single database thread, to ensure data consistency.
     */
    override fun dequeue(): PersistentItem? {
//        return runBlocking(coroutineContext) {
        val items = internalPop(1)
        return if (items.count() > 0) items.first() else null
//        }
    }

    /**
     * Pops the first [count] unexpired items off the queue; it reads then deletes.
     * Executed in the context of a single database thread, to ensure data consistency.
     *
     * @param count - limits the number of items to dequeue off the list; negative numbers will
     * dequeue all currently queued items.
     */
    override fun dequeue(count: Int): List<PersistentItem> {
//        return runBlocking(coroutineContext) {
        return internalPop(count)
//        }
    }

    /**
     * Deletes entries for each item in the [items] list by its [PersistentItem.key]
     */
    private fun internalDeleteAll(items: List<PersistentItem>) {
        db.beginTransaction()
        try {
            items.forEach {
                kvDao.delete(it.key)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Pops the first [count] unexpired items off the queue; it reads then deletes.
     * @param count - limits the number of items to dequeue off the list; negative numbers will
     * dequeue all currently queued items.
     */
    private fun internalPop(count: Int): List<PersistentItem> {
        val list: MutableList<PersistentItem> = mutableListOf()

        val cursor = db.query(
            tableName,
            null,
            PersistentStorageDao.IS_NOT_EXPIRED_CLAUSE,
            arrayOf(getTimestamp().toString()),
            null,
            null,
            "$COLUMN_TIMESTAMP ASC",
            if (count > 0) "$count" else null
        ) // negatives will return all in the queue

        cursor?.let {
            if (cursor.count > 0) {
                val columnKeyIndex = it.getColumnIndex(COLUMN_KEY)
                val columnValueIndex = it.getColumnIndex(COLUMN_VALUE)
                val columnTimestampIndex = it.getColumnIndex(COLUMN_TIMESTAMP)
                val columnExpiryIndex = it.getColumnIndex(COLUMN_EXPIRY)
                while (cursor.moveToNext()) {
                    val item = PersistentItem(
                        it.getString(columnKeyIndex),
                        it.getString(columnValueIndex),
                        Expiry.fromLongValue(it.getLong(columnExpiryIndex)),
                        it.getLong(columnTimestampIndex),
                        Serialization.JSON_OBJECT
                    )
                    list.add(item)
                }
            }
        }
        internalDeleteAll(list)
        cursor.close()
        return list
    }

    /**
     * Sets the value of [maxQueueSize] and resizes the existing queue if necessary.
     * Will drop the oldest Items off the end of the queue first.
     */
    override fun resize(size: Int) {
        maxQueueSize = size
//        launch(Logger.exceptionHandler) {
        createSpaceIfRequired(0)
//        }
    }

    /**
     * If the incoming item count will cause the queue size to exceed the maximum allowed by
     * [maxQueueSize], then this will delete the oldest N items required to make enough space for
     * the incoming items
     */
    private fun createSpaceIfRequired(count: Int) {
        val spaceRequired = spaceRequired(count)
        if (spaceRequired > 0) {
            internalPop(spaceRequired)
        }
    }

    /**
     * Calculates the number of items required to be removed in order to fit [count] number of new
     * items into the queue
     */
    private fun spaceRequired(incomingCount: Int): Int {
        return if (maxQueueSize == -1) 0
        else kvDao.count() + incomingCount - maxQueueSize
    }

    /**
     * [PersistentItem] allows null values for both Expiry and Timestamp. The DispatchStore, however,
     * should never have null values for these fields, since we need to track when they were created
     * and also when they are available to purge.
     *
     * This method will set the Expiry to FOREVER when and infinite expiry is set in the publish
     * settings (-1), or will set it to [expiryDays] number of days into the future.
     *
     * If the [PersistentItem.timestamp] is not set, then it generates a new one based on the current
     * system time.
     */
    private fun setItemDefaults(item: PersistentItem) {

        item.expiry = item.expiry ?: if (expiryDays < 0) Expiry.FOREVER
        else Expiry.afterTimeUnit(expiryDays.toLong(), TimeUnit.DAYS)
        item.timestamp = item.timestamp ?: getTimestamp()
    }
}