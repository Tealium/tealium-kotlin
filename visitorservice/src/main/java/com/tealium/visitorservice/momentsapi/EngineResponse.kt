package com.tealium.visitorservice.momentsapi

import com.tealium.core.JsonUtils
import com.tealium.visitorservice.asListOfStrings
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception

const val KEY_AUDIENCES = "audiences"
const val KEY_BADGES = "badges"
const val KEY_PROPERTIES = "properties"


data class EngineResponse(
    val attributes: Map<String, Any>? = null,
    val badges: List<String>? = null,
    val audiences: List<String>? = null,
    val strings: Map<String, String>? = null,
    val booleans: Map<String, Boolean>? = null,
    val dates: Map<String, Long>? = null,
    val numbers: Map<String, Double>? = null
) {

    companion object {
        fun toJson(engineResponse: EngineResponse): JSONObject {
            val json = JSONObject()

            engineResponse.attributes?.let {
                json.put(KEY_PROPERTIES, JsonUtils.jsonFor(it))
            }

            engineResponse.badges?.let {
                json.put(KEY_BADGES, JSONArray(it))
            }

            engineResponse.audiences?.let {
                json.put(KEY_AUDIENCES, JSONArray(it))
            }

            return json
        }

        fun fromJson(json: JSONObject): EngineResponse {
            val atr = json.optJSONObject(KEY_PROPERTIES)?.let { JsonUtils.mapFor(it) }
            val strings = mutableMapOf<String, String>()
            val booleans = mutableMapOf<String, Boolean>()
            val dates = mutableMapOf<String, Long>()
            val numbers = mutableMapOf<String, Double>()

            atr?.let { attributes ->
                for ((key, value) in attributes) {
                    when (value) {
                        is String -> strings[key] = value
                        is Boolean -> booleans[key] = value
                        is Int -> {
                            if (isValidTimestamp(value)) {
                                dates[key] = value.toLong()
                            } else {
                                numbers[key] = value.toDouble()
                            }
                        }

                        is Double -> {
                            if (isValidTimestamp(value.toInt())) {
                                dates[key] = value.toLong()
                            } else {
                                numbers[key] = value
                            }
                        }

                        is Long -> {
                            if (isValidTimestamp(value)) {
                                dates[key] = value
                            }
                        }

                        else -> {
                            // throw error, refuse to decode, should be a failed response
                            throw Exception("Unsupported attribute type for EngineResponse")
                        }
                    }
                }
            }

            return EngineResponse(
                attributes = atr,
                badges = json.optJSONArray(KEY_BADGES)?.asListOfStrings(),
                audiences = json.optJSONArray(KEY_AUDIENCES)?.asListOfStrings(),
                strings = if (strings.isNotEmpty()) strings.toMap() else null,
                booleans = if (booleans.isNotEmpty()) booleans.toMap() else null,
                dates = if (dates.isNotEmpty()) dates.toMap() else null,
                numbers = if (numbers.isNotEmpty()) numbers.toMap() else null
            )
        }

        private fun isValidTimestamp(timestamp: Long): Boolean {
            if (timestamp.toString().length != 13) return false

            if (timestamp < 0) return false

            val maxTimestamp = Long.MAX_VALUE / 1000

            return timestamp <= maxTimestamp
        }

        private fun isValidTimestamp(timestamp: Int): Boolean {
            // check if conversion results in Not a Number
            if (timestamp.toLong().toDouble().isNaN()) return false

            return isValidTimestamp(timestamp.toLong())
        }


    }
}

