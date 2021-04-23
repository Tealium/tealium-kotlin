package com.tealium.dispatcher

import com.tealium.core.JsonUtils
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
        return JsonUtils.jsonFor(payload()).toString()
    }

    fun encode(jsonStringer: JSONStringer, key: String, value: Any) {
        (value as? Any)?.let {
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