package com.tealium.core.persistence

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import com.tealium.core.Logger
import com.tealium.core.messaging.NewSessionListener
import com.tealium.core.persistence.SqlDataLayer.Columns.COLUMN_EXPIRY
import com.tealium.core.persistence.SqlDataLayer.Columns.COLUMN_KEY
import com.tealium.core.persistence.SqlDataLayer.Columns.COLUMN_TIMESTAMP
import com.tealium.core.persistence.SqlDataLayer.Columns.COLUMN_TYPE
import com.tealium.core.persistence.SqlDataLayer.Columns.COLUMN_VALUE
import com.tealium.tealiumlibrary.BuildConfig
import java.util.*

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
internal open class PersistentStorageDao(
    private val dbHelper: DatabaseHelper,
    private val tableName: String,
    private val shouldIncludeExpired: Boolean = false,
    private val onDataUpdated: ((String, PersistentItem) -> Unit)? = null,
    private val onDataRemoved: ((Set<String>) -> Unit)? = null
) : KeyValueDao<String, PersistentItem>, NewSessionListener {

    private val db: SQLiteDatabase? = dbHelper.db

    override fun getAll(): Map<String, PersistentItem> {
        return getAll(
            selection = if (shouldIncludeExpired) null else IS_NOT_EXPIRED_CLAUSE,
            selectionArgs = if (shouldIncludeExpired) null else arrayOf(getTimestamp().toString())
        )
    }

    private fun getAll(
        selection: String?,
        selectionArgs: Array<String>?
    ): Map<String, PersistentItem> {
        val map = mutableMapOf<String, PersistentItem>()

        if (db == null) {
            return map
        }

        val cursor = db.query(
            tableName,
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        cursor?.let {
            if (cursor.count > 0) {
                val columnKeyIndex = it.getColumnIndex(COLUMN_KEY)
                val columnValueIndex = it.getColumnIndex(COLUMN_VALUE)
                val columnTypeIndex = it.getColumnIndex(COLUMN_TYPE)
                val columnTimestampIndex = it.getColumnIndex(COLUMN_TIMESTAMP)
                val columnExpiryIndex = it.getColumnIndex(COLUMN_EXPIRY)

                while (cursor.moveToNext()) {
                    val persistentItem = PersistentItem(it.getString(columnKeyIndex),
                        it.getString(columnValueIndex),
                        Expiry.fromLongValue(it.getLong(columnExpiryIndex)),
                        if (it.isNull(columnTimestampIndex)) null else it.getLong(
                            columnTimestampIndex
                        ),
                        Serialization.values()
                            .find { ser -> ser.code == it.getInt(columnTypeIndex) }
                            ?: Serialization.STRING)
                    persistentItem.apply {
                        map[persistentItem.key] = persistentItem
                    }
                }
            }
        }

        cursor.close()
        return map
    }

    private fun getExpired(timestamp: Long = getTimestamp()): Map<String, PersistentItem> {
        return getAll(
            selection = IS_EXPIRED_CLAUSE,
            selectionArgs = arrayOf(timestamp.toString())
        )
    }

    override fun get(key: String): PersistentItem? {
        val selection =
            if (shouldIncludeExpired) "$COLUMN_KEY = ?" else "$COLUMN_KEY = ? AND $IS_NOT_EXPIRED_CLAUSE"
        val selectionArgs =
            if (shouldIncludeExpired) arrayOf(key) else arrayOf(key, getTimestamp().toString())

        if (db == null) {
            return null
        }

        val cursor = db.query(
            tableName,
            arrayOf(COLUMN_VALUE, COLUMN_TYPE, COLUMN_EXPIRY, COLUMN_TIMESTAMP),
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        return cursor?.let {
            var persistentItem: PersistentItem? = null
            if (it.count > 0) {
                val columnValueIndex = it.getColumnIndex(COLUMN_VALUE)
                val columnTypeIndex = it.getColumnIndex(COLUMN_TYPE)
                val columnTimestampIndex = it.getColumnIndex(COLUMN_TIMESTAMP)
                val columnExpiryIndex = it.getColumnIndex(COLUMN_EXPIRY)
                it.moveToFirst()

                persistentItem = PersistentItem(key,
                    it.getString(columnValueIndex),
                    Expiry.fromLongValue(it.getLong(columnExpiryIndex)),
                    if (it.isNull(columnTimestampIndex)) null else it.getLong(columnTimestampIndex),
                    Serialization.values().find { ser -> ser.code == it.getInt(columnTypeIndex) }
                        ?: Serialization.STRING)
            }
            it.close()
            persistentItem
        }
    }

    override fun insert(item: PersistentItem) {
        dbHelper.onDbReady {
            db?.beginTransaction()
            try {
                val inserted =
                    db?.insertWithOnConflict(tableName, null, item.toContentValues(), CONFLICT_REPLACE)

                inserted?.let {
                    if (it > 0) {
                        onDataUpdated?.invoke(item.key, item)
                    }
                }

                db?.setTransactionSuccessful()
            } catch (e: Exception) {
                Logger.dev(BuildConfig.TAG, "Error while trying to insert item")
            } finally {
                db?.endTransaction()
            }
        }
    }

    override fun update(item: PersistentItem) {
        dbHelper.onDbReady {
            db?.beginTransaction()
            try {
                val updated = db?.update(
                    tableName, item.toContentValues(),
                    "$COLUMN_KEY = ?",
                    arrayOf(item.key)
                )
                updated?.let {
                    if (it > 0) {
                        onDataUpdated?.invoke(item.key, item)
                    }
                }
                db?.setTransactionSuccessful()
            } catch (e: Exception) {
                Logger.dev(BuildConfig.TAG, "Error while trying to update item")
            } finally {
                db?.endTransaction()
            }
        }
    }

    override fun upsert(item: PersistentItem) {
        val oldItem = get(item.key)
        if (oldItem != null) {
            if (item.expiry == null && Expiry.isExpired(oldItem.expiry)) {
                item.expiry = Expiry.SESSION
            }
            update(item)
        } else {
            item.expiry = item.expiry ?: Expiry.SESSION
            insert(item)
        }
    }

    override fun delete(key: String) {

        dbHelper.onDbReady {
            db?.beginTransaction()
            try {
                val deleted = db?.delete(
                    tableName,
                    "$COLUMN_KEY = ?",
                    arrayOf(key)
                )
                deleted?.let {
                    if (it > 0) {
                        onDataRemoved?.invoke(setOf(key))
                    }
                }

                db?.setTransactionSuccessful()
            } catch (e: Exception) {
                Logger.dev(BuildConfig.TAG, "Error while trying to delete key: $key")
            } finally {
                db?.endTransaction()
            }
        }
    }

    private fun delete(keys: Set<String>) {
        dbHelper.onDbReady {
            db?.beginTransaction()
            try {
                db?.delete(
                    tableName,
                    "$COLUMN_KEY IN (${keys.joinToString(", ") { "?" }})",
                    keys.toTypedArray()
                )

                db?.setTransactionSuccessful()
            } catch (e: Exception) {
                Logger.dev(BuildConfig.TAG, "Error while trying to delete keys")
            } finally {
                db?.endTransaction()
            }
        }
    }

    override fun clear() {
        dbHelper.onDbReady {
            db?.beginTransaction()
            try {
                val keys = keys()
                db?.delete(
                    tableName,
                    null,
                    null
                )
                onDataRemoved?.invoke(keys.toSet())

                db?.setTransactionSuccessful()
            } catch (e: Exception) {
                Logger.dev(BuildConfig.TAG, "Error while trying to clear database")
            } finally {
                db?.endTransaction()
            }
        }
    }

    override fun keys(): List<String> {
        val selection = if (shouldIncludeExpired) null else IS_NOT_EXPIRED_CLAUSE
        val selectionArgs = if (shouldIncludeExpired) null else arrayOf(getTimestamp().toString())

        val keys = mutableListOf<String>()

        if (db == null) {
            return keys
        }

        val cursor = db.query(
            tableName,
            arrayOf(COLUMN_KEY),
            selection,
            selectionArgs,
            null,
            null,
            null,
            null
        )
        cursor?.let {
            val columnIndex = it.getColumnIndex(COLUMN_KEY)

            while (it.moveToNext()) {
                keys.add(it.getString(columnIndex))
            }
            cursor.close()
        }
        return keys
    }

    override fun count(): Int {
        val selection = if (shouldIncludeExpired) "" else "WHERE $IS_NOT_EXPIRED_CLAUSE"
        val selectionArgs = if (shouldIncludeExpired) null else arrayOf(getTimestamp().toString())

        if (db == null) {
            return 0
        }

        val cursor = db.rawQuery(
            "SELECT COUNT(*) from $tableName $selection",
            selectionArgs
        )

        return cursor?.let {
            it.moveToFirst()
            val count = it.getInt(0)
            cursor.close()
            count
        } ?: 0
    }

    override fun contains(key: String): Boolean {
        val selection =
            if (shouldIncludeExpired) "$COLUMN_KEY = ?" else "$COLUMN_KEY = ? AND $IS_NOT_EXPIRED_CLAUSE"
        val selectionArgs =
            if (shouldIncludeExpired) arrayOf(key) else arrayOf(key, getTimestamp().toString())

        if (db == null) {
            return false
        }

        val cursor = db.query(
            tableName,
            arrayOf(COLUMN_KEY),
            selection,
            selectionArgs,
            null,
            null,
            null,
            null
        )

        return cursor?.let {
            val count = it.count
            cursor.close()
            count > 0
        } ?: false
    }

    override fun purgeExpired() {
        dbHelper.onDbReady {
            db?.beginTransaction()
            try {
                val timestamp = getTimestamp()
                val expired = getExpired(timestamp)

                if (expired.isNotEmpty()) {
                    db?.delete(
                        tableName,
                        IS_EXPIRED_CLAUSE,
                        arrayOf(timestamp.toString())
                    )
                    onDataRemoved?.invoke(expired.map { it.key }.toSet())
                }

                db?.setTransactionSuccessful()
            } catch (e: Exception) {
                Logger.dev(BuildConfig.TAG, "Error while trying to purge expired data")
            } finally {
                db?.endTransaction()
            }
        }
    }

    override fun onNewSession(sessionId: Long) {
        dbHelper.onDbReady {
            db?.beginTransaction()
            try {
                val selection = "$COLUMN_EXPIRY = ?"
                val selectionArgs = arrayOf(Expiry.SESSION.expiryTime().toString())
                val sessionItems = getAll(
                    selection = selection,
                    selectionArgs = selectionArgs
                )
                if (sessionItems.isNotEmpty()) {
                    db?.delete(
                        tableName,
                        selection,
                        selectionArgs
                    )
                    onDataRemoved?.invoke(sessionItems.map { it.key }.toSet())
                }

                db?.setTransactionSuccessful()
            } catch (e: Exception) {
                Logger.dev(BuildConfig.TAG, "Error while trying to update session data")
            } finally {
                db?.endTransaction()
            }
        }
    }

    companion object {
        internal val IS_NOT_EXPIRED_CLAUSE =
            "($COLUMN_EXPIRY < 0 OR $COLUMN_EXPIRY > ?)"

        internal val IS_EXPIRED_CLAUSE =
            "($COLUMN_EXPIRY >= 0 AND $COLUMN_EXPIRY < ?)"
    }
}