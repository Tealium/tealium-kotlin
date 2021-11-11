package com.tealium.visitorservice

import org.json.JSONObject

/**
 * Holds all visit-scoped attribute data relating to the current visitor identified by the [Tealium.visitorId].
 */
data class CurrentVisit(
    var createdAt: Long = System.currentTimeMillis(),
    var totalEventCount: Int = 0,
    var dates: Map<String, Long>? = null,
    var booleans: Map<String, Boolean>? = null,
    var arraysOfBooleans: Map<String, List<Boolean>>? = null,
    var numbers: Map<String, Double>? = null,
    var arraysOfNumbers: Map<String, List<Double>>? = null,
    var tallies: Map<String, Map<String, Double>>? = null,
    var strings: Map<String, String>? = null,
    var arraysOfStrings: Map<String, List<String>>? = null,
    var setsOfStrings: Map<String, Set<String>>? = null
) {

    companion object {

        fun toJson(currentVisit: CurrentVisit): JSONObject {
            val json = JSONObject()

            currentVisit.dates?.let {
                json.put(KEY_DATES, JSONObject(it))
            }

            currentVisit.booleans?.let {
                json.put(KEY_FLAGS, JSONObject(it))
            }

            currentVisit.arraysOfBooleans?.let {
                json.put(KEY_FLAG_LISTS, JSONObject(it))
            }

            currentVisit.numbers?.let {
                json.put(KEY_METRICS, JSONObject(it))
            }

            currentVisit.arraysOfNumbers?.let {
                json.put(KEY_METRIC_LISTS, JSONObject(it))
            }

            currentVisit.tallies?.let {
                json.put(KEY_METRIC_SETS, JSONObject(it))
            }

            currentVisit.strings?.let {
                json.put(KEY_PROPERTIES, JSONObject(it))
            }

            currentVisit.arraysOfStrings?.let {
                json.put(KEY_PROPERTY_LISTS, JSONObject(it))
            }

            currentVisit.setsOfStrings?.let {
                json.put(KEY_PROPERTY_SETS, JSONObject(it))
            }

            return json
        }

        fun fromJson(json: JSONObject): CurrentVisit {
            return CurrentVisit().apply {
                createdAt = json.optLong(KEY_CREATED_AT)
                totalEventCount = json.optInt(KEY_TOTAL_EVENT_COUNT)
                dates = json.optJSONObject(KEY_DATES)?.asDates()
                booleans = json.optJSONObject(KEY_FLAGS)?.asBooleans()
                arraysOfBooleans = json.optJSONObject(KEY_FLAG_LISTS)?.asArraysOfBooleans()
                numbers = json.optJSONObject(KEY_METRICS)?.asNumbers()
                arraysOfNumbers = json.optJSONObject(KEY_METRIC_LISTS)?.asArraysOfNumbers()
                tallies = json.optJSONObject(KEY_METRIC_SETS)?.asTallies()
                strings = json.optJSONObject(KEY_PROPERTIES)?.asStrings()
                arraysOfStrings = json.optJSONObject(KEY_PROPERTY_LISTS)?.asArraysOfStrings()
                setsOfStrings = json.optJSONObject(KEY_PROPERTY_SETS)?.asSetsOfStrings()
            }
        }
    }
}