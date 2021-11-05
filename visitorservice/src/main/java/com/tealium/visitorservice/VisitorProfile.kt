package com.tealium.visitorservice

import org.json.JSONObject

const val KEY_AUDIENCES = "audiences"
const val KEY_BADGES = "badges"
const val KEY_DATES = "dates"
const val KEY_FLAGS = "flags"
const val KEY_FLAG_LISTS = "flag_lists"
const val KEY_METRICS = "metrics"
const val KEY_METRIC_LISTS = "metric_lists"
const val KEY_METRIC_SETS = "metric_sets"
const val KEY_PROPERTIES = "properties"
const val KEY_PROPERTY_LISTS = "property_lists"
const val KEY_PROPERTY_SETS = "property_sets"
const val KEY_CURRENT_VISIT = "current_visit"
const val KEY_CREATED_AT = "creation_ts"
const val KEY_TOTAL_EVENT_COUNT = "total_event_count"
const val KEY_TOTAL_EVENT_COUNT_METRIC = "22"

/**
 * Holds all visitor-scoped attribute data relating to the current visitor identified by the [Tealium.visitorId].
 * Visit-scoped attribute data can be accessed via the [currentVisit] property.
 */
data class VisitorProfile(
    var audiences: Map<String, String>? = null,
    var badges: Map<String, Boolean>? = null,
    var dates: Map<String, Long>? = null,
    var booleans: Map<String, Boolean>? = null,
    var arraysOfBooleans: Map<String, List<Boolean>>? = null,
    var numbers: Map<String, Double>? = null,
    var arraysOfNumbers: Map<String, List<Double>>? = null,
    var tallies: Map<String, Map<String, Double>>? = null,
    var strings: Map<String, String>? = null,
    var arraysOfStrings: Map<String, List<String>>? = null,
    var setsOfStrings: Map<String, Set<String>>? = null,
    var totalEventCount: Int = 0,
    var currentVisit: CurrentVisit? = null
) {

    companion object {
        fun toJson(visitorProfile: VisitorProfile): JSONObject {
            val json = JSONObject()

            visitorProfile.audiences?.let {
                json.put(KEY_AUDIENCES, JSONObject(it))
            }

            visitorProfile.badges?.let {
                json.put(KEY_BADGES, JSONObject(it))
            }

            visitorProfile.dates?.let {
                json.put(KEY_DATES, JSONObject(it))
            }

            visitorProfile.booleans?.let {
                json.put(KEY_FLAGS, JSONObject(it))
            }

            visitorProfile.arraysOfBooleans?.let {
                json.put(KEY_FLAG_LISTS, JSONObject(it))
            }

            visitorProfile.numbers?.let {
                json.put(KEY_METRICS, JSONObject(it))
            }

            visitorProfile.arraysOfNumbers?.let {
                json.put(KEY_METRIC_LISTS, JSONObject(it))
            }

            visitorProfile.tallies?.let {
                json.put(KEY_METRIC_SETS, JSONObject(it))
            }

            visitorProfile.strings?.let {
                json.put(KEY_PROPERTIES, JSONObject(it))
            }

            visitorProfile.arraysOfStrings?.let {
                json.put(KEY_PROPERTY_LISTS, JSONObject(it))
            }

            visitorProfile.setsOfStrings?.let {
                json.put(KEY_PROPERTY_SETS, JSONObject(it))
            }

            visitorProfile.currentVisit?.let {
                json.put(KEY_CURRENT_VISIT, CurrentVisit.toJson(it))
            }

            return json
        }

        fun fromJson(json: JSONObject): VisitorProfile {
            val visitorProfile = VisitorProfile().apply {
                audiences = json.optJSONObject(KEY_AUDIENCES)?.asStrings()
                badges = json.optJSONObject(KEY_BADGES)?.asBooleans()
                dates = json.optJSONObject(KEY_DATES)?.asDates()
                booleans = json.optJSONObject(KEY_FLAGS)?.asBooleans()
                arraysOfBooleans =
                    json.optJSONObject(KEY_FLAG_LISTS)?.asArraysOfBooleans()
                numbers = json.optJSONObject(KEY_METRICS)?.asNumbers()
                arraysOfNumbers =
                    json.optJSONObject(KEY_METRIC_LISTS)?.asArraysOfNumbers()
                tallies = json.optJSONObject(KEY_METRIC_SETS)?.asTallies()
                strings = json.optJSONObject(KEY_PROPERTIES)?.asStrings()
                arraysOfStrings = json.optJSONObject(KEY_PROPERTY_LISTS)
                    ?.asArraysOfStrings()
                setsOfStrings = json.optJSONObject(KEY_PROPERTY_SETS)?.asSetsOfStrings()
                currentVisit = json.optJSONObject(KEY_CURRENT_VISIT)?.let {
                    CurrentVisit.fromJson(it)
                }
            }

            return visitorProfile
        }

        @Deprecated("No longer in use - type conversions handled by JSONObject", level = DeprecationLevel.WARNING)
        fun toDouble(value: Any): Double? {
            return when (value as? Number) {
                null -> value as? Double
                else -> value.toDouble()
            }
        }
    }
}