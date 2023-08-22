package com.tealium.core.persistence

import com.tealium.core.Logger
import com.tealium.core.messaging.EventRouter
import com.tealium.core.messaging.NewSessionListener
import com.tealium.tealiumlibrary.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import kotlin.Exception

/**
 *  Key Value storage backed by a SQLite database.
 *
 *  Any stored values should be retrieved in the same type - there is no type coercion performed. i.e.
 *  if you store using [putInt] then you should retrieve using [getInt] and so on.
 *
 *  @param context required for opening the Database
 *  @param tableName table name of the underlying SQL table. Is expected to have already been created
 *                      in the [DatabaseHelper].
 */
internal class PersistentStorage(
    dbHelper: DatabaseHelper,
    private val tableName: String,
    private val volatileData: MutableMap<String, Any> = ConcurrentHashMap(),
    private val eventRouter: EventRouter,
    private val backgroundScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val dao: KeyValueDao<String, PersistentItem> = PersistentStorageDao(
        dbHelper,
        tableName,
        false,
        onDataUpdated = { k, v ->
            backgroundScope.launch {
                try {
                    val value = v.deserialize()
                    eventRouter.onDataUpdated(k, value ?: v.value)
                } catch (e: Exception) {
                    Logger.dev(
                        BuildConfig.TAG,
                        "Exception handling onDataUpdated($k, $v): ${e.message}"
                    )
                }
            }
        },
        onDataRemoved = { keys ->
            backgroundScope.launch {
                try {
                    eventRouter.onDataRemoved(keys)
                } catch (e: Exception) {
                    Logger.dev(
                        BuildConfig.TAG,
                        "Exception handling onDataRemoved($keys): ${e.message}"
                    )
                }
            }
        },
    ).also { it.purgeExpired() },
    private var sessionId: Long = 1L,
) : DataLayer,
    NewSessionListener {

    override val name: String = "DataLayer"
    override var enabled: Boolean = true

    override fun putString(key: String, value: String, expiry: Expiry?) =
        put(key, value, Serdes.stringSerde().serializer, expiry, type = Serialization.STRING)

    override fun putInt(key: String, value: Int, expiry: Expiry?) =
        put(key, value, Serdes.intSerde().serializer, expiry, type = Serialization.INT)

    override fun putLong(key: String, value: Long, expiry: Expiry?) =
        put(key, value, Serdes.longSerde().serializer, expiry, type = Serialization.LONG)

    override fun putDouble(key: String, value: Double, expiry: Expiry?) =
        put(key, value, Serdes.doubleSerde().serializer, expiry, type = Serialization.DOUBLE)

    override fun putBoolean(key: String, value: Boolean, expiry: Expiry?) =
        put(key, value, Serdes.booleanSerde().serializer, expiry, type = Serialization.BOOLEAN)

    override fun putStringArray(key: String, value: Array<String>, expiry: Expiry?) = put(
        key,
        value,
        Serdes.stringArraySerde().serializer,
        expiry,
        type = Serialization.STRING_ARRAY
    )

    override fun putIntArray(key: String, value: Array<Int>, expiry: Expiry?) =
        put(key, value, Serdes.intArraySerde().serializer, expiry, type = Serialization.INT_ARRAY)

    override fun putLongArray(key: String, value: Array<Long>, expiry: Expiry?) =
        put(key, value, Serdes.longArraySerde().serializer, expiry, type = Serialization.LONG_ARRAY)

    override fun putDoubleArray(key: String, value: Array<Double>, expiry: Expiry?) = put(
        key,
        value,
        Serdes.doubleArraySerde().serializer,
        expiry,
        type = Serialization.DOUBLE_ARRAY
    )

    override fun putBooleanArray(key: String, value: Array<Boolean>, expiry: Expiry?) = put(
        key,
        value,
        Serdes.booleanArraySerde().serializer,
        expiry,
        type = Serialization.BOOLEAN_ARRAY
    )

    override fun putJsonObject(key: String, value: JSONObject, expiry: Expiry?) = put(
        key,
        value,
        Serdes.jsonObjectSerde().serializer,
        expiry,
        type = Serialization.JSON_OBJECT
    )

    override fun putJsonArray(key: String, value: JSONArray, expiry: Expiry?) = put(
        key,
        value,
        Serdes.jsonArraySerde().serializer,
        expiry,
        type = Serialization.JSON_ARRAY
    )

    private fun <T> put(
        key: String,
        value: T,
        serializer: Serializer<T>,
        expiry: Expiry?,
        type: Serialization
    ) {
        if (expiry == Expiry.UNTIL_RESTART) {
            volatileData[key] = value as Any
            dao.delete(key)
            notifyUpdated(key, value as Any)
        } else {
            dao.upsert(PersistentItem(key, serializer.serialize(value), expiry, type = type))
            volatileData.remove(key);
        }
    }

    override fun getString(key: String): String? {
        return volatileData[key]?.let { it as? String } ?: getItem(
            key,
            Serdes.stringSerde().deserializer
        )
    }

    override fun getInt(key: String): Int? {
        return volatileData[key]?.let { it as? Int } ?: getItem(key, Serdes.intSerde().deserializer)
    }

    override fun getLong(key: String): Long? {
        return volatileData[key]?.let { it as? Long } ?: getItem(
            key,
            Serdes.longSerde().deserializer
        )
    }

    override fun getDouble(key: String): Double? {
        return volatileData[key]?.let { it as? Double } ?: getItem(
            key,
            Serdes.doubleSerde().deserializer
        )
    }

    override fun getBoolean(key: String): Boolean? {
        return volatileData[key]?.let { it as? Boolean } ?: getItem(
            key,
            Serdes.booleanSerde().deserializer
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun getStringArray(key: String): Array<String>? {
        return volatileData[key]?.let { it as? Array<String> } ?: getItem(
            key,
            Serdes.stringArraySerde().deserializer
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun getIntArray(key: String): Array<Int>? {
        return volatileData[key]?.let { it as? Array<Int> } ?: getItem(
            key,
            Serdes.intArraySerde().deserializer
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun getLongArray(key: String): Array<Long>? {
        return volatileData[key]?.let { it as? Array<Long> } ?: getItem(
            key,
            Serdes.longArraySerde().deserializer
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun getDoubleArray(key: String): Array<Double>? {
        return volatileData[key]?.let { it as? Array<Double> } ?: getItem(
            key,
            Serdes.doubleArraySerde().deserializer
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun getBooleanArray(key: String): Array<Boolean>? {
        return volatileData[key]?.let { it as? Array<Boolean> } ?: getItem(
            key,
            Serdes.booleanArraySerde().deserializer
        )
    }

    override fun getJsonObject(key: String): JSONObject? {
        return volatileData[key]?.let { it as? JSONObject } ?: getItem(
            key,
            Serdes.jsonObjectSerde().deserializer
        )
    }

    override fun getJsonArray(key: String): JSONArray? {
        return volatileData[key]?.let { it as? JSONArray } ?: getItem(
            key,
            Serdes.jsonArraySerde().deserializer
        )
    }

    override fun get(key: String): Any? {
        return volatileData[key] ?: dao.get(key)?.deserialize()
    }

    override fun all(): Map<String, Any> {
        return dao.getAll().mapValues {
            it.value.deserialize() ?: it.value.value
        }.plus(volatileData).also {
            backgroundScope.launch {
                // expire any stored data
                dao.purgeExpired()
            }
        }
    }

    private fun <T> getItem(key: String, deserializer: Deserializer<T>): T? {
        return dao.get(key)
            ?.let {
                try {
                    deserializer.deserialize(it.value)
                } catch (e: Exception) {
                    Logger.dev(BuildConfig.TAG, "Exception deserializing ${it.value}")
                    null
                }
            }
    }

    override fun remove(key: String) {
        val item = volatileData.remove(key)
        // if it was stored in memory, no need to empty it from storage
        if (item == null) {
            dao.delete(key)
        } else {
            notifyRemoved(key)
        }
    }

    override fun clear() {
        val keys = volatileData.keys.toSet()

        volatileData.clear()
        dao.clear()

        notifyRemoved(keys)
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
        return if (volatileData.containsKey(key)) Expiry.UNTIL_RESTART else dao.get(key)?.expiry
    }

    private fun notifyUpdated(key: String, value: Any) {
        backgroundScope.launch {
            try {
                eventRouter.onDataUpdated(key, value)
            } catch (e: Exception) {
                Logger.dev(
                    BuildConfig.TAG,
                    "Exception handling onDataUpdated($key, $value): ${e.message}"
                )
            }
        }
    }

    private fun notifyRemoved(key: String) {
        notifyRemoved(setOf(key))
    }

    private fun notifyRemoved(keys: Set<String>) {
        backgroundScope.launch {
            try {
                eventRouter.onDataRemoved(keys)
            } catch (e: Exception) {
                Logger.dev(BuildConfig.TAG, "Exception handling onDataRemoved($keys): ${e.message}")
            }
        }
    }

    override fun subscribe(listener: DataLayer.DataLayerUpdatedListener) {
        eventRouter.subscribe(listener)
    }

    override fun unsubscribe(listener: DataLayer.DataLayerUpdatedListener) {
        eventRouter.unsubscribe(listener)
    }

    override fun onNewSession(sessionId: Long) {
        if (this.sessionId == sessionId) {
            // skip duplicate sessionId
            return
        }

        clearSessionData(sessionId)
    }

    fun clearSessionData(sessionId: Long) {
        (dao as? PersistentStorageDao)?.onNewSession(sessionId) ?: run {
            dao.getAll().filter {
                it.value.expiry == Expiry.SESSION
            }.forEach {
                dao.delete(it.key)
            }
        }
    }
}