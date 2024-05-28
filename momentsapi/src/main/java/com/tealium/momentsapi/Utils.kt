package com.tealium.momentsapi

import com.tealium.core.Logger
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception

internal fun JSONArray.asListOfStrings(): List<String> {
    val list = ArrayList<String>()
    for (i in 0 until length()) {
        try {
            val element = getString(i)
            list.add(element)
        } catch (ex: Exception) {
            Logger.dev(BuildConfig.TAG, "Failed to parse String: ${ex.message}")
        }
    }

    return list.toList()
}

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