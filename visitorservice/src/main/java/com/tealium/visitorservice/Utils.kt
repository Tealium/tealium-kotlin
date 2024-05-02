package com.tealium.visitorservice

import com.tealium.core.Logger
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception

internal fun JSONObject.asStrings(): Map<String, String> {
    val map = mutableMapOf<String, String>()
    this.keys().forEach { key ->
        try {
            this.getString(key).let { value ->
                map[key] = value
            }
        } catch (ex: Exception) {
            Logger.dev(BuildConfig.TAG, "Failed to parse String: ${ex.message}")
        }
    }
    return map
}

internal fun JSONObject.asNumbers(): Map<String, Double> {
    val map = mutableMapOf<String, Double>()
    this.keys().forEach { key ->
        try {
            this.getDouble(key).let { value ->
                map[key] = value
            }
        } catch (ex: Exception) {
            Logger.dev(BuildConfig.TAG, "Failed to parse Double: ${ex.message}")
        }
    }
    return map
}

internal fun JSONObject.asBooleans(): Map<String, Boolean> {
    val map = mutableMapOf<String, Boolean>()
    this.keys().forEach { key ->
        try {
            this.getBoolean(key).let { value ->
                map[key] = value
            }
        } catch (ex: Exception) {
            Logger.dev(BuildConfig.TAG, "Failed to parse Boolean: ${ex.message}")
        }
    }
    return map
}

internal fun JSONObject.asDates(): Map<String, Long> {
    val map = mutableMapOf<String, Long>()
    this.keys().forEach { key ->
        try {
            this.getLong(key).let { value ->
                map[key] = value
            }
        } catch (ex: Exception) {
            Logger.dev(BuildConfig.TAG, "Failed to parse Long: ${ex.message}")
        }
    }
    return map
}

internal fun JSONObject.asArraysOfStrings(): Map<String, List<String>> {
    val map = mutableMapOf<String, List<String>>()
    this.keys().forEach { key ->
        this.optJSONArray(key)?.let { array ->
            val strings = ArrayList<String>()
            for (i in 0 until array.length()) {
                try {
                    strings.add(array.getString(i))
                } catch (ex: Exception) {
                    Logger.dev(BuildConfig.TAG, "Failed to parse String for List: ${ex.message}")
                }
            }
            map[key] = strings
        }
    }
    return map
}

internal fun JSONObject.asArraysOfNumbers(): Map<String, List<Double>> {
    val map = mutableMapOf<String, List<Double>>()
    this.keys().forEach { key ->
        this.optJSONArray(key)?.let { array ->
            val numbers = ArrayList<Double>()
            for (i in 0 until array.length()) {
                try {
                    numbers.add(array.getDouble(i))
                } catch (ex: Exception) {
                    Logger.dev(BuildConfig.TAG, "Failed to parse Double for List: ${ex.message}")
                }
            }
            map[key] = numbers
        }
    }
    return map
}

internal fun JSONObject.asArraysOfBooleans(): Map<String, List<Boolean>> {
    val map = mutableMapOf<String, List<Boolean>>()
    this.keys().forEach { key ->
        this.optJSONArray(key)?.let { array ->
            val booleans = ArrayList<Boolean>()
            for (i in 0 until array.length()) {
                try {
                    booleans.add(array.getBoolean(i))
                } catch (ex: Exception) {
                    Logger.dev(BuildConfig.TAG, "Failed to parse Boolean for List: ${ex.message}")
                }
            }
            map[key] = booleans
        }
    }
    return map
}

internal fun JSONObject.asSetsOfStrings(): Map<String, Set<String>> {
    val map = mutableMapOf<String, Set<String>>()
    this.keys().forEach { key ->
        this.optJSONArray(key)?.let { array ->
            val strings = ArrayList<String>()
            for (i in 0 until array.length()) {
                try {
                    strings.add(array.getString(i))
                } catch (ex: Exception) {
                    Logger.dev(BuildConfig.TAG, "Failed to parse String for Set: ${ex.message}")
                }
            }
            map[key] = strings.toSet()
        }
    }
    return map
}

internal fun JSONObject.asTallies(): Map<String, Map<String, Double>> {
    val map = mutableMapOf<String, Map<String, Double>>()
    this.keys().forEach { key ->
        this.optJSONObject(key)?.let { tallyObj ->
            val tallyValues: MutableMap<String, Double> = mutableMapOf()
            tallyObj.keys().forEach { tallyKey ->
                try {
                    tallyObj.getDouble(tallyKey).let { tallyValue ->
                        tallyValues[tallyKey] = tallyValue
                    }
                } catch (ex: Exception) {
                    Logger.dev(BuildConfig.TAG, "Failed to parse Double for Tally: ${ex.message}")
                }
            }
            map[key] = tallyValues
        }
    }
    return map
}