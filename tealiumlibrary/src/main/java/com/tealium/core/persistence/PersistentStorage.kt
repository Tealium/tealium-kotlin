package com.tealium.core.persistence

import com.tealium.core.messaging.NewSessionListener
import kotlinx.coroutines.CoroutineScope
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

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
                        private val tableName: String,
                        private val volatileData: MutableMap<String, PersistentItem<*>> = ConcurrentHashMap())
    : SqlDataLayer,
        NewSessionListener,
        CoroutineScope by dbHelper.scope {

    override val name: String = "DataLayer"
    override var enabled: Boolean = true

    private val dao = PersistentStorageDao<PersistentItem<*>>(dbHelper, tableName, false).also { it.purgeExpired() }

    override fun putString(key: String, value: String, expiry: Expiry?) {
        put(PersistentString(key, value, expiry))
    }

    override fun putInt(key: String, value: Int, expiry: Expiry?) {
        put(PersistentInt(key, value, expiry))
    }

    override fun putLong(key: String, value: Long, expiry: Expiry?) {
        put(PersistentLong(key, value, expiry))
    }

    override fun putDouble(key: String, value: Double, expiry: Expiry?) {
        put(PersistentDouble(key, value, expiry))
    }

    override fun putBoolean(key: String, value: Boolean, expiry: Expiry?) {
        put(PersistentBoolean(key, value, expiry))
    }

    override fun putStringArray(key: String, value: Array<String>, expiry: Expiry?) {
        put(PersistentStringArray(key, value, expiry))
    }

    override fun putIntArray(key: String, value: Array<Int>, expiry: Expiry?) {
        put(PersistentIntArray(key, value, expiry))
    }

    override fun putLongArray(key: String, value: Array<Long>, expiry: Expiry?) {
        put(PersistentLongArray(key, value, expiry))
    }

    override fun putDoubleArray(key: String, value: Array<Double>, expiry: Expiry?) {
        put(PersistentDoubleArray(key, value, expiry))
    }

    override fun putBooleanArray(key: String, value: Array<Boolean>, expiry: Expiry?) {
        put(PersistentBooleanArray(key, value, expiry))
    }

    override fun putJsonObject(key: String, value: JSONObject, expiry: Expiry?) {
        put(PersistentJsonObject(key, value, expiry))
    }

    private fun put(item: PersistentItem<*>) {
        item.value?.let {
            if (item.expiry == Expiry.UNTIL_RESTART) {
                volatileData[item.key] = item
                dao.delete(item.key)
            } else {
                dao.upsert(item)
                volatileData.remove(item.key);
            }
        }
    }

    override fun getString(key: String): String? {
        val item = getItem(key)
        return (item as? PersistentString)?.value
    }

    override fun getInt(key: String): Int? {
        val item = getItem(key)
        return (item as? PersistentInt)?.value
    }

    override fun getLong(key: String): Long? {
        val item = getItem(key)
        return (item as? PersistentLong)?.value
    }

    override fun getDouble(key: String): Double? {
        val item = getItem(key)
        return (item as? PersistentDouble)?.value
    }

    override fun getBoolean(key: String): Boolean? {
        val item = getItem(key)
        return (item as? PersistentBoolean)?.value
    }

    override fun getStringArray(key: String): Array<String>? {
        val item = getItem(key)
        return (item as? PersistentStringArray)?.value
    }

    override fun getIntArray(key: String): Array<Int>? {
        val item = getItem(key)
        return (item as? PersistentIntArray)?.value
    }

    override fun getLongArray(key: String): Array<Long>? {
        val item = getItem(key)
        return (item as? PersistentLongArray)?.value
    }

    override fun getDoubleArray(key: String): Array<Double>? {
        val item = getItem(key)
        return (item as? PersistentDoubleArray)?.value
    }

    override fun getBooleanArray(key: String): Array<Boolean>? {
        val item = getItem(key)
        return (item as? PersistentBooleanArray)?.value
    }

    override fun getJsonObject(key: String): JSONObject? {
        val item = getItem(key)
        return (item as? PersistentJsonObject)?.value
    }

    override fun get(key: String): Any? {
        val item = getItem(key)
        return item?.value
    }

    private fun getItem(key: String) : PersistentItem<*>? {
        return volatileData[key] ?: dao.get(key)
    }

    override fun all(): Map<String, Any> {
        return dao.getAll().plus(volatileData).mapValues {
            it.value.value!!
        }
    }

    override fun remove(key: String) {
        val item = volatileData.remove(key)
        // if it was stored in memory, no need to empty it from storage
        if (item == null) {
            dao.delete(key)
        }
    }

    override fun clear() {
        volatileData.clear()
        dao.clear()
    }

    override fun contains(key: String): Boolean {
        return volatileData.containsKey(key) || dao.contains(key)
    }

    override fun keys(): List<String> {
        return volatileData.keys.union(dao.keys()).toList();
    }

    override fun count(): Int {
        return volatileData.size + dao.count()
    }

    override fun getExpiry(key: String): Expiry? {
        return volatileData[key]?.expiry ?: dao.get(key)?.expiry
    }

    override fun onNewSession(sessionId: Long) {
        dao.getAll().filter {
            it.value.expiry == Expiry.SESSION
        }.forEach {
            dao.delete(it.key)
        }
    }
}