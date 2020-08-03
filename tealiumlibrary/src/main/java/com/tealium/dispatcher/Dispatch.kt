package com.tealium.dispatcher

import org.json.JSONStringer

interface Dispatch {

    val id: String
    var timestamp: Long?

    fun payload(): Map<String, Any>

    fun addAll(data: Map<String, Any>)

    operator fun get(key: String): Any? {
        return payload()[key]
    }

    fun toJsonString(): String {
        val jsonStringer = JSONStringer()
        jsonStringer.`object`()
        payload().entries.forEach { entry ->
            when (entry.value) {
                is String -> encode(jsonStringer, entry.key, entry.value)
                is Int -> encode(jsonStringer, entry.key, entry.value)
                is Float -> encode(jsonStringer, entry.key, entry.value)
                is Double -> encode(jsonStringer, entry.key, entry.value)
                is Array<*> -> encodeCollection(jsonStringer, entry.key, entry.value)
                is List<*> -> encodeCollection(jsonStringer, entry.key, entry.value)
                else -> encodeString(jsonStringer, entry.key, entry.value)
            }
        }
        jsonStringer.endObject()
        return jsonStringer.toString()
    }

    fun encode(jsonStringer: JSONStringer, key: String, value: Any) {
        (value as? Object)?.let {
            jsonStringer.key(key)
            jsonStringer.value(it)
        }
    }

    fun encodeCollection(jsonStringer: JSONStringer, key: String, value: Any) {
        jsonStringer.key(key)
        jsonStringer.array()
        (value as? Array<*>)?.forEach {
            jsonStringer.value(it)
        }
        (value as? List<*>)?.forEach {
            jsonStringer.value(it)
        }
        jsonStringer.endArray()
    }

    fun encodeString(jsonStringer: JSONStringer, key: String, value: Any) {
        jsonStringer.key(key)
        jsonStringer.value(value.toString())
    }
}