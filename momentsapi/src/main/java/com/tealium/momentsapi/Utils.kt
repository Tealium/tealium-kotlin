package com.tealium.momentsapi

import com.tealium.core.Logger
import org.json.JSONArray
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