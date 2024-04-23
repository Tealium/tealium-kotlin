package com.tealium.visitorservice.momentsapi

import com.tealium.core.JsonUtils
import com.tealium.visitorservice.asStringList
import org.json.JSONArray
import org.json.JSONObject

const val KEY_AUDIENCES = "audiences"
const val KEY_BADGES = "badges"
const val KEY_PROPERTIES = "properties"


data class EngineResponse(
    val properties: Map<String, Any>? = null,
    val badges: List<String>? = null,
    val audiences: List<String>? = null
) {

    companion object {
        fun toJson(engineResponse: EngineResponse): JSONObject {
            val json = JSONObject()

            engineResponse.properties?.let {
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
            return EngineResponse(
                properties = json.optJSONObject(KEY_PROPERTIES)?.let { JsonUtils.mapFor(it) },
                badges = json.optJSONArray(KEY_BADGES)?.asStringList(),
                audiences = json.optJSONArray(KEY_AUDIENCES)?.asStringList()
            )
        }
    }
}