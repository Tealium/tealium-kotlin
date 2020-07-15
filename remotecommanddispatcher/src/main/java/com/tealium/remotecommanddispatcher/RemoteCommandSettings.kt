package com.tealium.remotecommanddispatcher

import android.os.SystemClock
import com.tealium.core.JsonUtils
import org.json.JSONObject

data class RemoteCommandSettings(var apiConfig: Map<String, Any>? = null,
                            var mappings: Map<String, String>? = null,
                            var apiCommands: Map<String, String>? = null,
                            var lastFetchTimestamp: Long = Long.MIN_VALUE) {
    companion object {
        fun fromJson(jsonObject: JSONObject): RemoteCommandSettings {
            val remoteCommandSettings = RemoteCommandSettings()

            jsonObject.optJSONObject(Settings.CONFIG)?.let {
                remoteCommandSettings.apiConfig = JsonUtils.mapFor(it)
            }
            jsonObject.optJSONObject(Settings.MAPPINGS)?.let {
                remoteCommandSettings.mappings = JsonUtils.mapFor(it)
                        .entries
                        .associate { entry ->
                            entry.key to entry.value as String
                        }
            }
            jsonObject.optJSONObject(Settings.COMMANDS)?.let {
                remoteCommandSettings.apiCommands = JsonUtils.mapFor(it)
                        .entries
                        .associate { entry ->
                            entry.key to entry.value as String
                        }
            }

            remoteCommandSettings.lastFetchTimestamp = SystemClock.currentThreadTimeMillis()

            return remoteCommandSettings
        }
    }
}