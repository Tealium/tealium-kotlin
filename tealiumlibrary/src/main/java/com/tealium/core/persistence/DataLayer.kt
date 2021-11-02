package com.tealium.core.persistence

import com.tealium.core.Collector
import org.json.JSONArray
import org.json.JSONObject

interface DataLayer : Collector {

    /**
     * Stores a string [value] referenced by the [key], with an optional expiry time.
     *
     * @param key string value identifying the [value]
     * @param value string value to be stored
     * @param expiry optional expiry time - the default is left to the implementation
     */
    fun putString(key: String, value: String, expiry: Expiry? = null)

    /**
     * Stores an Int [value] referenced by the [key], with an optional expiry time.
     *
     * @param key string value identifying the [value]
     * @param value Int value to be stored
     * @param expiry optional expiry time - the default is left to the implementation
     */
    fun putInt(key: String, value: Int, expiry: Expiry? = null)

    /**
     * Stores a Long [value] referenced by the [key], with an optional expiry time.
     *
     * @param key string value identifying the [value]
     * @param value Long value to be stored
     * @param expiry optional expiry time - the default is left to the implementation
     */
    fun putLong(key: String, value: Long, expiry: Expiry? = null)

    /**
     * Stores a Double [value] referenced by the [key], with an optional expiry time.
     *
     * @param key string value identifying the [value]
     * @param value Double value to be stored
     * @param expiry optional expiry time - the default is left to the implementation
     */
    fun putDouble(key: String, value: Double, expiry: Expiry? = null)

    /**
     * Stores a Boolean [value] referenced by the [key], with an optional expiry time.
     *
     * @param key string value identifying the [value]
     * @param value Boolean value to be stored
     * @param expiry optional expiry time - the default is left to the implementation
     */
    fun putBoolean(key: String, value: Boolean, expiry: Expiry? = null)

    /**
     * Stores a String array [value] referenced by the [key], with an optional expiry time.
     *
     * @param key string value identifying the [value]
     * @param value String Array value to be stored
     * @param expiry optional expiry time - the default is left to the implementation
     */
    fun putStringArray(key: String, value: Array<String>, expiry: Expiry? = null)

    /**
     * Stores an Int array [value] referenced by the [key], with an optional expiry time.
     *
     * @param key string value identifying the [value]
     * @param value Int Array value to be stored
     * @param expiry optional expiry time - the default is left to the implementation
     */
    fun putIntArray(key: String, value: Array<Int>, expiry: Expiry? = null)

    /**
     * Stores a Long array [value] referenced by the [key], with an optional expiry time.
     *
     * @param key string value identifying the [value]
     * @param value Long Array value to be stored
     * @param expiry optional expiry time - the default is left to the implementation
     */
    fun putLongArray(key: String, value: Array<Long>, expiry: Expiry? = null)

    /**
     * Stores a Double array [value] referenced by the [key], with an optional expiry time.
     *
     * @param key string value identifying the [value]
     * @param value Double Array value to be stored
     * @param expiry optional expiry time - the default is left to the implementation
     */
    fun putDoubleArray(key: String, value: Array<Double>, expiry: Expiry? = null)

    /**
     * Stores a Boolean array [value] referenced by the [key], with an optional expiry time.
     *
     * @param key string value identifying the [value]
     * @param value Boolean Array value to be stored
     * @param expiry optional expiry time - the default is left to the implementation
     */
    fun putBooleanArray(key: String, value: Array<Boolean>, expiry: Expiry? = null)

    /**
     * Stores a JSONObject [value] referenced by the [key], with an optional expiry time.
     *
     * @param key string value identifying the [value]
     * @param value JSONObject value to be stored
     * @param expiry optional expiry time - the default is left to the implementation
     */
    fun putJsonObject(key: String, value: JSONObject, expiry: Expiry? = null)

    /**
     * Stores a JSONObject [value] referenced by the [key], with an optional expiry time.
     *
     * @param key string value identifying the [value]
     * @param value JSONArray value to be stored
     * @param expiry optional expiry time - the default is left to the implementation
     */
    fun putJsonArray(key: String, value: JSONArray, expiry: Expiry? = null)

    /**
     * Retrieves a String value for the given key.
     *
     * @return String value associated with the key, else null where the key doesn't exist or if
     *  the value stored for this key is not a String.
     */
    fun getString(key: String): String?

    /**
     * Retrieves an Int value for the given key.
     *
     * @return Int value associated with the key, else [null] where the key doesn't exist or if
     *  the value stored for this key is not an Int.
     */
    fun getInt(key: String): Int?

    /**
     * Retrieves a Long value for the given key.
     *
     * @return Long value associated with the key, else null where the key doesn't exist or if
     *  the value stored for this key is not a Long.
     */
    fun getLong(key: String): Long?

    /**
     * Retrieves a Double value for the given key.
     *
     * @return Double value associated with the key, else null where the key doesn't exist or if
     *  the value stored for this key is not a Long.
     */
    fun getDouble(key: String): Double?

    /**
     * Retrieves a Boolean value for the given key.
     *
     * @return Boolean value associated with the key, else null where the key doesn't exist or if
     *  the value stored for this key is not a Boolean.
     */
    fun getBoolean(key: String): Boolean?

    /**
     * Retrieves a String Array value for the given key.
     *
     * @return String Array value associated with the key, else null where the key doesn't exist or if
     *  the value stored for this key is not a String Array.
     */
    fun getStringArray(key: String): Array<String>?

    /**
     * Retrieves an Int Array value for the given key.
     *
     * @return Int Array value associated with the key, else null where the key doesn't exist or if
     *  the value stored for this key is not a Int Array.
     */
    fun getIntArray(key: String): Array<Int>?

    /**
     * Retrieves a Long Array value for the given key.
     *
     * @return Long Array value associated with the key, else null where the key doesn't exist or if
     *  the value stored for this key is not a Long Array.
     */
    fun getLongArray(key: String): Array<Long>?

    /**
     * Retrieves a Double Array value for the given key.
     *
     * @return Double Array value associated with the key, else null where the key doesn't exist or if
     *  the value stored for this key is not a Double Array.
     */
    fun getDoubleArray(key: String): Array<Double>?

    /**
     * Retrieves a Boolean Array value for the given key.
     *
     * @return Boolean Array value associated with the key, else null where the key doesn't exist or if
     *  the value stored for this key is not a Boolean Array.
     */
    fun getBooleanArray(key: String): Array<Boolean>?

    /**
     * Retrieves a JSONObject value for the given key.
     *
     * @return JSONObject value associated with the key, else null where the key doesn't exist or if
     *  the value stored for this key is not a JSONObject.
     */
    fun getJsonObject(key: String): JSONObject?

    /**
     * Retrieves a JSONObject value for the given key.
     *
     * @return JSONObject value associated with the key, else null where the key doesn't exist or if
     *  the value stored for this key is not a JSONObject.
     */
    fun getJsonArray(key: String): JSONArray?

    /**
     * Retrieves a value for the given key.
     *
     * @return Value associated with the key, else null where the key doesn't exist.
     */
    fun get(key: String): Any?

    /**
     * Retrieves a map of all key/value pairs stored
     *
     * @return a map of all key/value pairs, else an empty map.
     */
    fun all(): Map<String, Any>

    /**
     * Removes the value for the given key.
     */
    fun remove(key: String)

    /**
     * Removes all values from the storage.
     */
    fun clear()

    /**
     * @return true if the storage contains an entry for the given key.
     */
    fun contains(key: String): Boolean

    /**
     * @return a list of keys for all the items in the storage.
     */
    fun keys(): List<String>

    /**
     * @return a count of all entries in the storage
     */
    fun count(): Int

    /**
     * @return the expiry of a given key, else null if key doesn't exist.
     */
    fun getExpiry(key: String): Expiry?

    override suspend fun collect(): Map<String, Any> {
        return all()
    }
}
