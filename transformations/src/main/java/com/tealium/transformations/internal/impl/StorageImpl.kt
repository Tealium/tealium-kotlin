package com.tealium.transformations.internal.impl

import com.tealium.core.persistence.DataLayer
import com.tealium.core.persistence.Expiry
import com.tealium.transformations.internal.Storage
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class StorageImpl(
    private val dataLayer: DataLayer
) : Storage {
    override fun save(key: String, data: Any, expiry: String?) {
        // TODO - parse Expiry from string
        when (data) {
            // Limited data types supported by QuickJS
            is String -> {
                try {
                    if (data.startsWith("{")) {
                        val json = JSONObject(data)
                        dataLayer.putJsonObject(key, json, Expiry.SESSION)
                    } else if (data.startsWith("[")) {
                        val json = JSONArray(data)
                        dataLayer.putJsonArray(key, json, Expiry.SESSION)
                    } else {
                        dataLayer.putString(key, data, Expiry.SESSION)
                    }
                } catch (ex: JSONException) {

                }
            }
            is Int -> dataLayer.putInt(key, data, Expiry.SESSION)
            is Double -> dataLayer.putDouble(key, data, Expiry.SESSION)
            is Boolean -> dataLayer.putBoolean(key, data, Expiry.SESSION)
        }
    }

    override fun read(key: String): Any? {
        val data = dataLayer.get(key)
        return when (data) {
            // Limited data types supported by QuickJS
            is String, is Int, is Double, is Boolean  -> {
                data
            }
            null -> null
            else -> {
                data.toString()
            }
        }
    }
}