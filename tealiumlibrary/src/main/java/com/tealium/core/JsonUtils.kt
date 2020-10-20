package com.tealium.core

import org.json.JSONArray
import org.json.JSONObject

class JsonUtils {

    companion object {

        fun jsonFor(payload: Map<String, Any>): JSONObject {
            val jsonObject = JSONObject()
            payload.forEach { (key, value) ->
                val jsonValue = when (value) {
                    is Map<*, *> -> jsonFor(value as Map<String, Any>)
                    is Collection<*> -> JSONArray(value)
                    is Array<*> -> JSONArray(value)
                    else -> value
                }
                jsonObject.put(key, jsonValue)
            }

            return jsonObject
        }

        fun mapFor(json: JSONObject): MutableMap<String, Any> {
            val temp = mutableMapOf<String, Any>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = json.get(key)
                temp[key] = value
            }

            return temp
        }
    }
}