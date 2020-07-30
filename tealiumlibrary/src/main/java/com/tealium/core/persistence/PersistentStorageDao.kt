package com.tealium.core.persistence

import com.tealium.core.Logger
import com.tealium.core.persistence.SqlDataLayer.Companion.Columns.COLUMN_KEY
import com.tealium.core.persistence.SqlDataLayer.Companion.Columns.COLUMN_EXPIRY
import com.tealium.core.persistence.SqlDataLayer.Companion.Columns.COLUMN_TIMESTAMP
import com.tealium.core.persistence.SqlDataLayer.Companion.Columns.COLUMN_VALUE
import com.tealium.core.persistence.SqlDataLayer.Companion.Columns.COLUMN_TYPE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * @param K - Identifier Type
 * @param T - Type to be accessed
 */
interface KeyValueDao<K, T> {

    /**
     * Fetch and item given its [key]
     */
    fun get(key: K): T?

    /**
     * Fetch all items in the storage. Returning as a map of key/value pairs.
     */
    fun getAll(): Map<K, T>

    /**
     * Attempts to save an item in the storage, should not check if an item exists already with the
     * same key - see [upsert]
     */
    fun insert(item: T)

    /**
     * Attempts to update an existing entry in the storage, should not check if an item with the
     * given key already exists - see [upsert]
     */
    fun update(item: T)

    /**
     * Removes and item from storage given the [key].
     */
    fun delete(key: K)

    /**
     * Should check whether an item exists at the given key before choosing to [insert] or [update]
     * accordingly.
     */
    fun upsert(item: T)

    /**
     * Removes all entries from the storage.
     */
    fun clear()

    /**
     * Returns a list of keys identifying the current set of items stored.
     */
    fun keys(): List<K>

    /**
     * Returns the number of items currently stored.
     */
    fun count(): Int

    /**
     * Returns true if an item with the given [key] is currently stored, else returns false.
     */
    fun contains(key: K): Boolean

    /**
     * Removes all expired entries from storage.
     */
    fun purgeExpired()

}

/**
 * DAO for Key-Value pairs.
 * Public methods are forced onto a single thread and executed FIFO so as to guarantee database
 * consistency. Read operations (get/contains/keys etc) are run on blocking coroutines, and therefore
 * should not be called internally by one of the other non-blocking methods as they will all queue to
 * be executed on the same thread causing a deadlock.
 * Instead, there are a set of internal methods that are not coroutine specific that can be used to
 * query/update the database, but do not insist on a specific coroutine context and therefore do not
 * cause any queueing deadlock.
 *
 * @param context - required for opening the database
 * @param tableName - a table name for this implementation to read/write to.
 * @param includeExpired - whether to consider expired data in "read" requests.
 */
internal open class PersistentStorageDao<T : PersistentItem<*>>(private val dbHelper: DatabaseHelper,
                                                                private val tableName: String,
                                                                private val shouldIncludeExpired: Boolean = false)
    : KeyValueDao<String, T>,
        CoroutineScope by dbHelper.scope {

    private val db = dbHelper.writableDatabase

    internal val IS_NOT_EXPIRED_CLAUSE =
            "($COLUMN_EXPIRY < 0 OR $COLUMN_EXPIRY > ?)"

    internal val IS_EXPIRED_CLAUSE =
            "($COLUMN_EXPIRY >= 0 AND $COLUMN_EXPIRY < ?)"

    override fun getAll(): Map<String, T> {
        return runBlocking(coroutineContext) {
            internalGetAll()
        }
    }

    override fun get(key: String): T? {
        return runBlocking(coroutineContext) {
            internalGet(key)
        }
    }

    override fun insert(item: T) {
        launch(Logger.exceptionHandler) {
            internalInsert(item)
        }
    }

    override fun update(item: T) {
        launch(Logger.exceptionHandler) {
            internalUpdate(item)
        }
    }

    override fun upsert(item: T) {
        launch(Logger.exceptionHandler) {
            internalUpsert(item)
        }
    }

    override fun delete(key: String) {
        launch(Logger.exceptionHandler) {
            internalDelete(key)
        }
    }

    override fun clear() {
        launch(Logger.exceptionHandler) {
            internalClear()
        }
    }

    override fun keys(): List<String> {
        return runBlocking(coroutineContext) {
            internalKeys()
        }
    }

    override fun count(): Int {
        return runBlocking(coroutineContext) {
            internalCount()
        }
    }

    override fun contains(key: String): Boolean {
        return runBlocking(coroutineContext) {
            internalContains(key)
        }
    }

    override fun purgeExpired() {
        launch(Logger.exceptionHandler) {
            internalPurge()
        }
    }

    internal fun internalGetAll(includeExpired: Boolean = shouldIncludeExpired): Map<String, T> {
        val map = mutableMapOf<String, T>()
        val selection = if (includeExpired) null else IS_NOT_EXPIRED_CLAUSE
        val selectionArgs = if (includeExpired) null else arrayOf(getTimestamp().toString())
        val cursor = db.query(tableName,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null)

        cursor?.let {
            if (cursor.count > 0) {
                val columnKeyIndex = it.getColumnIndex(COLUMN_KEY)
                val columnValueIndex = it.getColumnIndex(COLUMN_VALUE)
                val columnTypeIndex = it.getColumnIndex(COLUMN_TYPE)
                val columnTimestampIndex = it.getColumnIndex(COLUMN_TIMESTAMP)
                val columnExpiryIndex = it.getColumnIndex(COLUMN_EXPIRY)

                while (cursor.moveToNext()) {
                    @Suppress("UNCHECKED_CAST")
                    val persistentItem = PersistentItem.create(it.getString(columnKeyIndex),
                            it.getString(columnValueIndex),
                            Expiry.fromLongValue(it.getLong(columnExpiryIndex)),
                            if (it.isNull(columnTimestampIndex)) null else it.getLong(columnTimestampIndex),
                            it.getInt(columnTypeIndex)) as? T
                    persistentItem?.apply {
                        map[persistentItem.key] = persistentItem
                    }
                }
            }
        }

        cursor.close()
        return map
    }

    protected fun internalGet(key: String, includeExpired: Boolean = shouldIncludeExpired): T? {
        val selection = if (includeExpired) "$COLUMN_KEY = ?" else "$COLUMN_KEY = ? AND $IS_NOT_EXPIRED_CLAUSE"
        val selectionArgs = if (includeExpired) arrayOf(key) else arrayOf(key, getTimestamp().toString())

        val cursor = db.query(tableName,
                arrayOf(COLUMN_VALUE, COLUMN_TYPE, COLUMN_EXPIRY, COLUMN_TIMESTAMP),
                selection,
                selectionArgs,
                null,
                null,
                null)

        return cursor?.let {
            var persistentItem: T? = null
            if (it.count > 0) {
                val columnValueIndex = it.getColumnIndex(COLUMN_VALUE)
                val columnTypeIndex = it.getColumnIndex(COLUMN_TYPE)
                val columnTimestampIndex = it.getColumnIndex(COLUMN_TIMESTAMP)
                val columnExpiryIndex = it.getColumnIndex(COLUMN_EXPIRY)
                it.moveToFirst()

                @Suppress("UNCHECKED_CAST")
                persistentItem =  PersistentItem.create(key,
                        it.getString(columnValueIndex),
                        Expiry.fromLongValue(it.getLong(columnExpiryIndex)),
                        if (it.isNull(columnTimestampIndex)) null else it.getLong(columnTimestampIndex),
                        it.getInt(columnTypeIndex)) as? T?
            }
            it.close()
            persistentItem
        }
    }

    internal fun internalInsert(item: T) {
        db.insert(tableName, null, item.toContentValues())
    }

    internal fun internalUpdate(item: T) {
        db.update(tableName, item.toContentValues(),
                "$COLUMN_KEY = ?",
                arrayOf(item.key))
    }

    internal fun internalUpsert(item: T) {
        val oldItem = internalGet(item.key, true)
        if (oldItem != null) {
            if (item.expiry == null && Expiry.isExpired(oldItem.expiry)) {
                item.expiry = Expiry.SESSION
            }
            internalUpdate(item)
        } else {
            item.expiry = item.expiry ?: Expiry.SESSION
            internalInsert(item)
        }
    }

    internal fun internalKeys(includeExpired: Boolean = shouldIncludeExpired): List<String> {
        val selection = if (includeExpired) null else IS_NOT_EXPIRED_CLAUSE
        val selectionArgs = if (includeExpired) null else arrayOf(getTimestamp().toString())

        val keys = mutableListOf<String>()
        val cursor = db.query(tableName,
                arrayOf(COLUMN_KEY),
                selection,
                selectionArgs,
                null,
                null,
                null,
                null)
        cursor?.let {
            val columnIndex = it.getColumnIndex(COLUMN_KEY)

            while (it.moveToNext()) {
                keys.add(it.getString(columnIndex))
            }
            cursor.close()
        }
        return keys
    }

    internal fun internalContains(key: String, includeExpired: Boolean = shouldIncludeExpired): Boolean {
        val selection = if (includeExpired) "$COLUMN_KEY = ?" else "$COLUMN_KEY = ? AND $IS_NOT_EXPIRED_CLAUSE"
        val selectionArgs = if (includeExpired) arrayOf(key) else arrayOf(key, getTimestamp().toString())

        val cursor = db.query(tableName,
                arrayOf(COLUMN_KEY),
                selection,
                selectionArgs,
                null,
                null,
                null,
                null)

        return cursor?.let {
            val count = it.count
            cursor.close()
            count > 0
        } ?: false
    }

    internal fun internalCount(includeExpired: Boolean = shouldIncludeExpired): Int {
        val selection = if (includeExpired) "" else "WHERE $IS_NOT_EXPIRED_CLAUSE"
        val selectionArgs = if (includeExpired) null else arrayOf(getTimestamp().toString())

        val cursor = db.rawQuery("SELECT COUNT(*) from $tableName $selection",
                selectionArgs)

        return cursor?.let {
            it.moveToFirst()
            val count = it.getInt(0)
            cursor.close()
            count
        } ?: 0
    }

    internal fun internalDelete(key: String) {
        db.delete(tableName,
                "$COLUMN_KEY = ?",
                arrayOf(key))
    }

    internal fun internalClear() {
        db.delete(tableName,
                null,
                null)
    }

    internal fun internalPurge(timestamp: Long = getTimestamp()) {
        db.delete(tableName,
                IS_EXPIRED_CLAUSE,
                arrayOf(timestamp.toString()))
    }
}