package com.tealium.visitorservice

import com.tealium.core.JsonUtils
import org.json.JSONArray
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
        var setsOfStrings: Map<String, Set<String>>? = null) {

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
                dates = json.optJSONObject(KEY_DATES)?.let {
                    JsonUtils.mapFor(it)
                            .entries
                            .associate { entry ->
                                entry.key to entry.value as Long
                            }
                }
                booleans = json.optJSONObject(KEY_FLAGS)?.let {
                    JsonUtils.mapFor(it)
                            .entries
                            .associate { entry ->
                                entry.key to entry.value as Boolean
                            }
                }
                arraysOfBooleans = json.optJSONObject(KEY_FLAG_LISTS)?.let {
                    JsonUtils.mapFor(it)
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
                numbers = json.optJSONObject(KEY_METRICS)?.let {
                    JsonUtils.mapFor(it)
                            .entries
                            .associate { entry ->
                                entry.key to VisitorProfile.toDouble(entry.value)
                            }
                }
                arraysOfNumbers = json.optJSONObject(KEY_METRIC_LISTS)?.let {
                    JsonUtils.mapFor(it)
                            .entries
                            .associate { entry ->
                                val values = entry.value as JSONArray
                                val doubles = ArrayList<Double>()
                                for (i in 0 until values.length()) {
                                    doubles.add(VisitorProfile.toDouble(values.get(i)))
                                }
                                entry.key to doubles
                            }
                }
                tallies = json.optJSONObject(KEY_METRIC_SETS)?.let {
                    JsonUtils.mapFor(it)
                            .entries
                            .associate { entry ->
                                val values = entry.value as JSONObject
                                val tallyValues = JsonUtils.mapFor(values)
                                        .entries
                                        .associate { tally ->
                                            tally.key to VisitorProfile.toDouble(tally.value)
                                        }
                                entry.key to tallyValues
                            }
                }
                strings = json.optJSONObject(KEY_PROPERTIES)?.let {
                    JsonUtils.mapFor(it)
                            .entries
                            .associate { entry ->
                                entry.key to entry.value as String
                            }
                }
                arraysOfStrings = json.optJSONObject(KEY_PROPERTY_LISTS)?.let {
                    JsonUtils.mapFor(it)
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
                setsOfStrings = json.optJSONObject(KEY_PROPERTY_SETS)?.let {
                    JsonUtils.mapFor(it)
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
            }
        }
    }
}