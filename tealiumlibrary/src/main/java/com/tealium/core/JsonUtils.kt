package com.tealium.core

import android.os.Build
import com.tealium.tealiumlibrary.BuildConfig
import org.json.*
import java.time.*
import java.util.*

class JsonUtils {

    companion object {

        fun jsonFor(payload: Map<String, Any>): JSONObject {
            val jsonObject = JSONObject()
            payload.forEach { (key, value) ->
                val jsonValue = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) when (value) {
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
                } else when (value) {
                    is Map<*, *> -> jsonFor(value as Map<String, Any>)
                    is Collection<*> -> JSONArray(value)
                    is Array<*> -> JSONArray(value)
                    is Date -> DateUtils.formatDate(value)
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

        fun isValidJson(input: String): Boolean {
            try {
                JSONTokener(input).nextValue()
            } catch (ex: JSONException) {
                Logger.dev(BuildConfig.TAG, "Invalid JSON input: $input")
                return false
            }
            return true
        }
    }
}