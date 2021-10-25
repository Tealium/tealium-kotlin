package com.tealium.visitorservice

import com.tealium.core.JsonUtils
import org.json.JSONArray
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
        var currentVisit: CurrentVisit? = null) {

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
            val visitorProfile = VisitorProfile()

            json.optJSONObject(KEY_AUDIENCES)?.let {
                visitorProfile.audiences = JsonUtils.mapFor(it)
                        .entries
                        .associate { entry ->
                            entry.key to entry.value as String
                        }
            }

            json.optJSONObject(KEY_BADGES)?.let {
                visitorProfile.badges = JsonUtils.mapFor(it)
                        .entries
                        .associate { entry ->
                            entry.key to entry.value as Boolean
                        }
            }

            json.optJSONObject(KEY_DATES)?.let {
                visitorProfile.dates = JsonUtils.mapFor(it)
                        .entries
                        .associate { entry ->
                            entry.key to entry.value as Long
                        }
            }

            json.optJSONObject(KEY_FLAGS)?.let {
                visitorProfile.booleans = JsonUtils.mapFor(it)
                        .entries
                        .associate { entry ->
                            entry.key to entry.value as Boolean
                        }
            }

            json.optJSONObject(KEY_FLAG_LISTS)?.let {
                visitorProfile.arraysOfBooleans = JsonUtils.mapFor(it)
                        .entries
                        .associate { entry ->
                            val values = entry.value as JSONArray
                            val booleans = ArrayList<Boolean>()
                            for (i in 0 until values.length()) {
                                booleans.add(values.get(i) as Boolean)
                            }
                            entry.key to booleans
                        }
            }

            json.optJSONObject(KEY_METRICS)?.let {
                visitorProfile.numbers = JsonUtils.mapFor(it)
                        .entries
                        .associate { entry ->
                            entry.key to toDouble(entry.value)
                        }.mapNotNull { (k, v) -> v?.let { k to it }  }.toMap()
                        .also { metrics ->
                            if (metrics.containsKey(KEY_TOTAL_EVENT_COUNT_METRIC)) {
                                visitorProfile.totalEventCount = metrics.getValue(KEY_TOTAL_EVENT_COUNT_METRIC).toInt()
                            }
                        }
            }

            json.optJSONObject(KEY_METRIC_LISTS)?.let {
                visitorProfile.arraysOfNumbers = JsonUtils.mapFor(it)
                        .entries
                        .associate { entry ->
                            val values = entry.value as JSONArray
                            val doubles = ArrayList<Double>()
                            for (i in 0 until values.length()) {
                                toDouble(values.get(i))?.let {
                                    doubles.add(it)
                                }
                            }
                            entry.key to doubles
                        }
            }

            json.optJSONObject(KEY_METRIC_SETS)?.let {
                visitorProfile.tallies = JsonUtils.mapFor(it)
                        .entries
                        .associate { entry ->
                            val values = entry.value as JSONObject
                            val tallyValues: Map<String, Double> = JsonUtils.mapFor(values)
                                    .entries
                                    .associate { tally ->
                                        tally.key to toDouble(tally.value)
                                    }.mapNotNull { (k, v) -> v?.let { k to it }  }.toMap()
                            entry.key to tallyValues
                        }
            }

            json.optJSONObject(KEY_PROPERTIES)?.let {
                visitorProfile.strings = JsonUtils.mapFor(it)
                        .entries
                        .associate { entry ->
                            entry.key to entry.value as String
                        }
            }

            json.optJSONObject(KEY_PROPERTY_LISTS)?.let {
                visitorProfile.arraysOfStrings = JsonUtils.mapFor(it)
                        .entries
                        .associate { entry ->
                            val values = entry.value as JSONArray
                            val strings = ArrayList<String>()
                            for (i in 0 until values.length()) {
                                strings.add(values.get(i) as String)
                            }
                            entry.key to strings
                        }
            }

            json.optJSONObject(KEY_PROPERTY_SETS)?.let {
                visitorProfile.setsOfStrings = JsonUtils.mapFor(it)
                        .entries
                        .associate { entry ->
                            val values = entry.value as JSONArray
                            val strings = mutableSetOf<String>()
                            for (i in 0 until values.length()) {
                                strings.add(values.get(i) as String)
                            }
                            entry.key to strings
                        }
            }

            json.optJSONObject(KEY_CURRENT_VISIT)?.let {
                visitorProfile.currentVisit = CurrentVisit.fromJson(it)
            }

            return visitorProfile
        }

        fun toDouble(value: Any): Double? {
            return when (value as? Number) {
                null -> value as? Double
                else -> value.toDouble()
            }
        }
    }
}