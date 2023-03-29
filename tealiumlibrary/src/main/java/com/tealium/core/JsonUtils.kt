package com.tealium.core

import android.os.Build
import com.tealium.tealiumlibrary.BuildConfig
import org.json.*
import java.time.*
import java.util.*

class JsonUtils {

    companion object {

        /**
         * Converts a [Map] to its [JSONObject] equivalent.
         *
         * Various types are supported during the conversion.
         * Keys containing [Map]s are converted to [JSONObject]s
         * [Collection]s and [Array]s are converted to [JSONArray]s
         * Dates and newer date representations are formatted consistently using [DateUtils]
         * formatters where the Android version supports them.
         *
         * @param payload - [Map] to convert to a [JSONObject]
         * @return [JSONObject] representation of the [payload]
         */
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

        /**
         * Converts a JSONObject into a MutableMap representation
         *
         * @param json [JSONObject] to convert to a [Map]
         * @return a [MutableMap] containing the Key-Value pairs extracted from [json]
         */
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

        /**
         * Checks whether a String contains a valid JSON Object
         *
         * @param input String representation of a JSON Object
         * @return true if [input] is valid json; else false
         */
        fun isValidJson(input: String): Boolean {
            try {
                JSONTokener(input).nextValue()
            } catch (ex: JSONException) {
                Logger.dev(BuildConfig.TAG, "Invalid JSON input: $input")
                return false
            }
            return true
        }

        /**
         * Attempts to parse the String as a JSON.
         *
         * @param jsonString String representation of a JSON Object
         * @return Valid JSONObject if [jsonString] is valid json; else null
         */
        fun tryParse(jsonString: String) : JSONObject? {
            return try {
                JSONObject(jsonString)
            } catch(ignore: JSONException) {
                null
            }
        }
    }
}