package com.tealium.momentsapi

import org.json.JSONArray
import org.json.JSONObject

data class EngineResponse(
    val badges: List<String>? = null,
    val audiences: List<String>? = null,
    val strings: Map<String, String>? = null,
    val booleans: Map<String, Boolean>? = null,
    val dates: Map<String, Long>? = null,
    val numbers: Map<String, Double>? = null
) {

    companion object {

        const val KEY_AUDIENCES = "audiences"
        const val KEY_BADGES = "badges"

        const val KEY_PROPERTIES = "properties"
        const val KEY_METRICS = "metrics"
        const val KEY_DATES = "dates"
        const val KEY_FLAGS = "flags"

        fun fromJson(json: JSONObject): EngineResponse {
            return EngineResponse(
                badges = json.optJSONArray(KEY_BADGES)?.asListOfStrings(),
                audiences = json.optJSONArray(KEY_AUDIENCES)?.asListOfStrings(),
                strings = json.optJSONObject(KEY_PROPERTIES)?.asStrings(),
                booleans = json.optJSONObject(KEY_FLAGS)?.asBooleans(),
                dates = json.optJSONObject(KEY_DATES)?.asDates(),
                numbers = json.optJSONObject(KEY_METRICS)?.asNumbers()
            )
        }

        fun toJson(engineResponse: EngineResponse): JSONObject {
            val json = JSONObject()

            engineResponse.audiences?.let {
                json.put(KEY_AUDIENCES, JSONArray(it))
            }

            engineResponse.badges?.let {
                json.put(KEY_BADGES, JSONArray(it))
            }

            engineResponse.strings?.let {
                json.put(KEY_PROPERTIES, JSONObject(it))
            }

            engineResponse.numbers?.let {
                json.put(KEY_METRICS, JSONObject(it))
            }

            engineResponse.dates?.let {
                json.put(KEY_DATES, JSONObject(it))
            }

            engineResponse.booleans?.let {
                json.put(KEY_FLAGS, JSONObject(it))
            }

            return json
        }
    }
}
