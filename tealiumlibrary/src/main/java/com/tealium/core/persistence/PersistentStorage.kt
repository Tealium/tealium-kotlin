package com.tealium.core.persistence

import com.tealium.core.messaging.NewSessionListener
import kotlinx.coroutines.CoroutineScope
import org.json.JSONObject

/**
 *  Key Value storage backed by a SQLite database.
 *
 *  All *put* type methods are launched onto a separate single threaded executor so as not to be blocking,
 *  but also FIFO. All *get* type methods, including count/contains, are executed in the same Coroutine Context
 *  as the *put* methods, but *are* still blocking so would be advisable not to be called on the Main
 *  thread.
 *
 *  Any stored values should be retrieved in the same type - there is no type coercion performed. i.e.
 *  if you store using [putInt] then you should retrieve using [getInt] and so on.
 *
 *  @param context required for opening the Database
 *  @param tableName table name of the underlying SQL table. Is expected to have already been created
 *                      in the [DatabaseHelper].
 */
internal class PersistentStorage(dbHelper: DatabaseHelper,
                        private val tableName: String)
    : SqlDataLayer,
        NewSessionListener,
        CoroutineScope by dbHelper.scope {

    override val name: String = "DATALAYER"
    override var enabled: Boolean = true

    private val dao = PersistentStorageDao<PersistentItem<*>>(dbHelper, tableName, false).also { it.purgeExpired() }

    override fun putString(key: String, value: String, expiry: Expiry?) {
        dao.upsert(PersistentString(key, value, expiry))
    }

    override fun putInt(key: String, value: Int, expiry: Expiry?) {
        dao.upsert(PersistentInt(key, value, expiry))
    }

    override fun putLong(key: String, value: Long, expiry: Expiry?) {
        dao.upsert(PersistentLong(key, value, expiry))
    }

    override fun putDouble(key: String, value: Double, expiry: Expiry?) {
        dao.upsert(PersistentDouble(key, value, expiry))
    }

    override fun putBoolean(key: String, value: Boolean, expiry: Expiry?) {
        dao.upsert(PersistentBoolean(key, value, expiry))
    }

    override fun putStringArray(key: String, value: Array<String>, expiry: Expiry?) {
        dao.upsert(PersistentStringArray(key, value, expiry))
    }

    override fun putIntArray(key: String, value: Array<Int>, expiry: Expiry?) {
        dao.upsert(PersistentIntArray(key, value, expiry))
    }

    override fun putLongArray(key: String, value: Array<Long>, expiry: Expiry?) {
        dao.upsert(PersistentLongArray(key, value, expiry))
    }

    override fun putDoubleArray(key: String, value: Array<Double>, expiry: Expiry?) {
        dao.upsert(PersistentDoubleArray(key, value, expiry))
    }

    override fun putBooleanArray(key: String, value: Array<Boolean>, expiry: Expiry?) {
        dao.upsert(PersistentBooleanArray(key, value, expiry))
    }

    override fun putJsonObject(key: String, value: JSONObject, expiry: Expiry?) {
        dao.upsert(PersistentJsonObject(key, value, expiry))
    }

    override fun getString(key: String): String? {
        val item = dao.get(key)
        return (item as? PersistentString)?.value
    }

    override fun getInt(key: String): Int? {
        val item = dao.get(key)
        return (item as? PersistentInt)?.value
    }

    override fun getLong(key: String): Long? {
        val item = dao.get(key)
        return (item as? PersistentLong)?.value
    }

    override fun getDouble(key: String): Double? {
        val item = dao.get(key)
        return (item as? PersistentDouble)?.value
    }

    override fun getBoolean(key: String): Boolean? {
        val item = dao.get(key)
        return (item as? PersistentBoolean)?.value
    }

    override fun getStringArray(key: String): Array<String>? {
        val item = dao.get(key)
        return (item as? PersistentStringArray)?.value
    }

    override fun getIntArray(key: String): Array<Int>? {
        val item = dao.get(key)
        return (item as? PersistentIntArray)?.value
    }

    override fun getLongArray(key: String): Array<Long>? {
        val item = dao.get(key)
        return (item as? PersistentLongArray)?.value
    }

    override fun getDoubleArray(key: String): Array<Double>? {
        val item = dao.get(key)
        return (item as? PersistentDoubleArray)?.value
    }

    override fun getBooleanArray(key: String): Array<Boolean>? {
        val item = dao.get(key)
        return (item as? PersistentBooleanArray)?.value
    }

    override fun getJsonObject(key: String): JSONObject? {
        val item = dao.get(key)
        return (item as? PersistentJsonObject)?.value
    }

    override fun get(key: String): Any? {
        val item = dao.get(key)
        return item?.value
    }

    override fun all(): Map<String, Any> {
        return dao.getAll().mapValues {
            it.value.value!!
        }
    }

    override fun remove(key: String) {
        dao.delete(key)
    }

    override fun clear() {
        dao.clear()
    }

    override fun contains(key: String): Boolean {
        return dao.contains(key)
    }

    override fun keys(): List<String> {
        return dao.keys()
    }

    override fun count(): Int {
        return dao.count()
    }

    override fun getExpiry(key: String): Expiry? {
        return dao.get(key)?.expiry
    }

    override fun onNewSession(sessionId: Long) {
        dao.getAll().filter {
            it.value.expiry == Expiry.SESSION
        }.forEach {
            dao.delete(it.key)
        }
    }
}