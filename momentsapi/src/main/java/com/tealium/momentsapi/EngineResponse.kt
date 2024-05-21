package com.tealium.momentsapi

import org.json.JSONObject

data class EngineResponse(
    val audiences: Map<String, String>? = null,
    val badges: Map<String, Boolean>? = null,
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

        fun toJson(engineResponse: EngineResponse): JSONObject {
            val json = JSONObject()

            engineResponse.audiences?.let {
                json.put(KEY_AUDIENCES, JSONObject(it))
            }

            engineResponse.badges?.let {
                json.put(KEY_BADGES, JSONObject(it))
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

        fun fromJson(json: JSONObject): EngineResponse {
            return EngineResponse(
                badges = json.optJSONObject(KEY_BADGES)?.asBooleans(),
                audiences = json.optJSONObject(KEY_AUDIENCES)?.asStrings(),
                strings = json.optJSONObject(KEY_PROPERTIES)?.asStrings(),
                booleans = json.optJSONObject(KEY_FLAGS)?.asBooleans(),
                dates = json.optJSONObject(KEY_DATES)?.asDates(),
                numbers = json.optJSONObject(KEY_METRICS)?.asNumbers()
            )
        }
    }
}
