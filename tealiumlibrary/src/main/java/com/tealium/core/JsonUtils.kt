package com.tealium.core

import android.annotation.TargetApi
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.time.*
import java.util.*

class JsonUtils {

    companion object {

        @TargetApi(Build.VERSION_CODES.O)
        fun jsonFor(payload: Map<String, Any>): JSONObject {
            val jsonObject = JSONObject()
            payload.forEach { (key, value) ->
                val jsonValue = when (value) {
                    is Map<*, *> -> jsonFor(value as Map<String, Any>)
                    is Collection<*> -> JSONArray(value)
                    is Array<*> -> JSONArray(value)
                    is Date -> DateUtils.formatDate(value)
                    is ZonedDateTime -> DateUtils.formatZonedDateTime(value)
                    is LocalDateTime -> DateUtils.formatLocalDateTime(value)
                    is LocalDate -> DateUtils.formatLocalDate(value)
                    is LocalTime -> DateUtils.formatLocalTime(value)
                    is Instant -> DateUtils.formatInstant(value)
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