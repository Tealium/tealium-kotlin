package com.tealium.core.persistence

import android.content.ContentValues
import com.tealium.core.persistence.SqlDataLayer.Companion.Columns.COLUMN_EXPIRY
import com.tealium.core.persistence.SqlDataLayer.Companion.Columns.COLUMN_KEY
import com.tealium.core.persistence.SqlDataLayer.Companion.Columns.COLUMN_TIMESTAMP
import com.tealium.core.persistence.SqlDataLayer.Companion.Columns.COLUMN_TYPE
import com.tealium.core.persistence.SqlDataLayer.Companion.Columns.COLUMN_VALUE
import org.json.JSONArray
import org.json.JSONObject

internal sealed class PersistentItem<T>(val key: String,
                                        val value: T,
                                        var expiry: Expiry? = null,
                                        var timestamp: Long? = null)
    : StringSerializer<T> {

    abstract val typeCode: Int

    fun toContentValues(): ContentValues {
        val contentValues = ContentValues()

        contentValues.put(COLUMN_KEY, key)
        contentValues.put(COLUMN_VALUE, serialize())
        contentValues.put(COLUMN_TYPE, typeCode)
        expiry?.let {
            contentValues.put(COLUMN_EXPIRY, it.expiryTime())
        }
        timestamp?.let {
            contentValues.put(COLUMN_TIMESTAMP, it)
        }
        return contentValues
    }

    companion object {

        @JvmStatic
        fun create(key: String,
                   value: String,
                   expiry: Expiry? = null,
                   timestamp: Long? = null,
                   typeCode: Int): PersistentItem<*>? {

            return when (typeCode) {
                SqlDataLayer.Companion.Type.STRING -> {
                    PersistentString(key,
                            value,
                            expiry,
                            timestamp)
                }
                SqlDataLayer.Companion.Type.BOOLEAN -> {
                    PersistentBoolean(key,
                            value,
                            expiry,
                            timestamp)
                }
                SqlDataLayer.Companion.Type.INTEGER -> {
                    PersistentInt(key,
                            value,
                            expiry,
                            timestamp)
                }
                SqlDataLayer.Companion.Type.LONG -> {
                    PersistentLong(key,
                            value,
                            expiry,
                            timestamp)
                }
                SqlDataLayer.Companion.Type.DOUBLE -> {
                    PersistentDouble(key,
                            value,
                            expiry,
                            timestamp)
                }
                SqlDataLayer.Companion.Type.STRING_ARRAY -> {
                    PersistentStringArray(key,
                            value,
                            expiry,
                            timestamp)
                }
                SqlDataLayer.Companion.Type.INTEGER_ARRAY -> {
                    PersistentIntArray(key,
                            value,
                            expiry,
                            timestamp)
                }
                SqlDataLayer.Companion.Type.BOOLEAN_ARRAY -> {
                    PersistentBooleanArray(key,
                            value,
                            expiry,
                            timestamp)
                }
                SqlDataLayer.Companion.Type.DOUBLE_ARRAY -> {
                    PersistentDoubleArray(key,
                            value,
                            expiry,
                            timestamp)
                }
                SqlDataLayer.Companion.Type.LONG_ARRAY -> {
                    PersistentLongArray(key,
                            value,
                            expiry,
                            timestamp)
                }
                SqlDataLayer.Companion.Type.JSON -> {
                    PersistentJsonObject(key,
                            value,
                            expiry,
                            timestamp)
                }
                else -> null
            }
        }
    }
}

internal class PersistentString(key: String,
                                value: String,
                                expiry: Expiry? = null,
                                timestamp: Long? = null) : PersistentItem<String>(key, value, expiry, timestamp) {

    override val typeCode: Int
        get() = SqlDataLayer.Companion.Type.STRING

    override fun serialize(): String {
        return value
    }
}

internal class PersistentInt(key: String,
                             value: Int,
                             expiry: Expiry? = null,
                             timestamp: Long? = null) : PersistentItem<Int>(key, value, expiry, timestamp) {

    constructor(key: String,
                value: String,
                expiry: Expiry? = null,
                timestamp: Long? = null) : this(key, deserialize(value), expiry, timestamp)

    override val typeCode: Int
        get() = SqlDataLayer.Companion.Type.INTEGER

    override fun serialize(): String {
        return value.toString()
    }

    companion object : StringDeserializer<Int> {
        override fun deserialize(string: String): Int {
            return string.toInt()
        }
    }
}

internal class PersistentLong(key: String,
                              value: Long,
                              expiry: Expiry? = null,
                              timestamp: Long? = null) : PersistentItem<Long>(key, value, expiry, timestamp) {

    constructor(key: String,
                value: String,
                expiry: Expiry? = null,
                timestamp: Long? = null) : this(key, deserialize(value), expiry, timestamp)

    override val typeCode: Int
        get() = SqlDataLayer.Companion.Type.LONG

    override fun serialize(): String {
        return value.toString()
    }

    companion object : StringDeserializer<Long> {
        override fun deserialize(string: String): Long {
            return string.toLong()
        }
    }
}

internal class PersistentDouble(key: String,
                                value: Double,
                                expiry: Expiry? = null,
                                timestamp: Long? = null) : PersistentItem<Double>(key, value, expiry, timestamp) {

    constructor(key: String,
                value: String,
                expiry: Expiry? = null,
                timestamp: Long? = null) : this(key, deserialize(value), expiry, timestamp)

    override val typeCode: Int
        get() = SqlDataLayer.Companion.Type.DOUBLE

    override fun serialize(): String {
        return value.toString()
    }

    companion object : StringDeserializer<Double> {
        override fun deserialize(string: String): Double {
            return string.toDouble()
        }
    }
}

internal class PersistentBoolean(key: String,
                                 value: Boolean,
                                 expiry: Expiry? = null,
                                 timestamp: Long? = null) : PersistentItem<Boolean>(key, value, expiry, timestamp) {

    constructor(key: String,
                value: String,
                expiry: Expiry? = null,
                timestamp: Long? = null) : this(key, deserialize(value), expiry, timestamp)

    override val typeCode: Int
        get() = SqlDataLayer.Companion.Type.BOOLEAN

    override fun serialize(): String {
        return (if (value) 1 else 0).toString()
    }

    companion object : StringDeserializer<Boolean> {
        override fun deserialize(string: String): Boolean {
            return string.toInt() > 0
        }
    }
}

internal class PersistentJsonObject(key: String,
                                    value: JSONObject,
                                    expiry: Expiry? = null,
                                    timestamp: Long? = null) : PersistentItem<JSONObject>(key, value, expiry, timestamp) {

    constructor(key: String,
                value: String,
                expiry: Expiry? = null,
                timestamp: Long? = null) : this(key, deserialize(value), expiry, timestamp)

    override val typeCode: Int
        get() = SqlDataLayer.Companion.Type.JSON

    override fun serialize(): String {
        return value.toString()
    }

    companion object : StringDeserializer<JSONObject> {
        override fun deserialize(string: String): JSONObject {
            return JSONObject(string)
        }
    }
}

internal class PersistentStringArray(key: String,
                                     value: Array<String>,
                                     expiry: Expiry? = null,
                                     timestamp: Long? = null) : PersistentItem<Array<String>>(key, value, expiry, timestamp) {

    constructor(key: String,
                value: String,
                expiry: Expiry? = null,
                timestamp: Long? = null) : this(key, deserialize(value), expiry, timestamp)

    override val typeCode: Int
        get() = SqlDataLayer.Companion.Type.STRING_ARRAY

    override fun serialize(): String {
        return JSONArray(value).toString()
    }

    companion object : StringDeserializer<Array<String>> {
        override fun deserialize(string: String): Array<String> {
            val jsonArray = JSONArray(string)
            val values = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                values.add(jsonArray[i].toString())
            }
            return values.toTypedArray()
        }
    }
}

internal class PersistentIntArray(key: String,
                                  value: Array<Int>,
                                  expiry: Expiry? = null,
                                  timestamp: Long? = null) : PersistentItem<Array<Int>>(key, value, expiry, timestamp) {

    constructor(key: String,
                value: String,
                expiry: Expiry? = null,
                timestamp: Long? = null) : this(key, deserialize(value), expiry, timestamp)

    override val typeCode: Int
        get() = SqlDataLayer.Companion.Type.INTEGER_ARRAY

    override fun serialize(): String {
        return JSONArray(value).toString()
    }

    companion object : StringDeserializer<Array<Int>> {
        override fun deserialize(string: String): Array<Int> {
            val jsonArray = JSONArray(string)
            val values = mutableListOf<Int>()
            for (i in 0 until jsonArray.length()) {
                when (jsonArray[i]) {
                    is Int -> values.add(jsonArray[i] as Int)
                    is String -> values.add(jsonArray[i].toString().toInt())
                }
            }
            return values.toTypedArray()
        }
    }
}

internal class PersistentLongArray(key: String,
                                   value: Array<Long>,
                                   expiry: Expiry? = null,
                                   timestamp: Long? = null) : PersistentItem<Array<Long>>(key, value, expiry, timestamp) {

    constructor(key: String,
                value: String,
                expiry: Expiry? = null,
                timestamp: Long? = null) : this(key, deserialize(value), expiry, timestamp)

    override val typeCode: Int
        get() = SqlDataLayer.Companion.Type.LONG_ARRAY

    override fun serialize(): String {
        return JSONArray(value).toString()
    }

    companion object : StringDeserializer<Array<Long>> {
        override fun deserialize(string: String): Array<Long> {
            val jsonArray = JSONArray(string)
            val values = mutableListOf<Long>()
            for (i in 0 until jsonArray.length()) {
                when (jsonArray[i]) {
                    is Long -> values.add(jsonArray[i] as Long)
                    is Int -> values.add((jsonArray[i] as Int).toLong())
                    is String -> values.add(jsonArray[i].toString().toLong())
                }
            }
            return values.toTypedArray()
        }
    }
}

internal class PersistentDoubleArray(key: String,
                                     value: Array<Double>,
                                     expiry: Expiry? = null,
                                     timestamp: Long? = null) : PersistentItem<Array<Double>>(key, value, expiry, timestamp) {

    constructor(key: String,
                value: String,
                expiry: Expiry? = null,
                timestamp: Long? = null) : this(key, deserialize(value), expiry, timestamp)

    override val typeCode: Int
        get() = SqlDataLayer.Companion.Type.DOUBLE_ARRAY

    override fun serialize(): String {
        return JSONArray(value).toString()
    }

    companion object : StringDeserializer<Array<Double>> {
        override fun deserialize(string: String): Array<Double> {
            val jsonArray = JSONArray(string)
            val values = mutableListOf<Double>()
            for (i in 0 until jsonArray.length()) {
                when (jsonArray[i]) {
                    is Double -> values.add(jsonArray[i] as Double)
                    is Long -> values.add((jsonArray[i] as Long).toDouble())
                    is Int -> values.add((jsonArray[i] as Int).toDouble())
                    is String -> values.add(jsonArray[i].toString().toDouble())
                }
            }
            return values.toTypedArray()
        }
    }
}

internal class PersistentBooleanArray(key: String,
                                      value: Array<Boolean>,
                                      expiry: Expiry? = null,
                                      timestamp: Long? = null) : PersistentItem<Array<Boolean>>(key, value, expiry, timestamp) {

    constructor(key: String,
                value: String,
                expiry: Expiry? = null,
                timestamp: Long? = null) : this(key, deserialize(value), expiry, timestamp)

    override val typeCode: Int
        get() = SqlDataLayer.Companion.Type.BOOLEAN_ARRAY

    override fun serialize(): String {
        return JSONArray(value).toString()
    }

    companion object : StringDeserializer<Array<Boolean>> {
        override fun deserialize(string: String): Array<Boolean> {
            val jsonArray = JSONArray(string)
            val values = mutableListOf<Boolean>()
            for (i in 0 until jsonArray.length()) {
                when (jsonArray[i]) {
                    is Boolean -> values.add(jsonArray[i] as Boolean)
                    is Int -> values.add((jsonArray[i] as Int) > 0)
                    is String -> values.add(jsonArray[i].toString().toInt() > 0)
                }
            }
            return values.toTypedArray()
        }
    }
}