package com.tealium.remotecommanddispatcher

import com.tealium.core.JsonUtils
import org.json.JSONObject

data class RemoteCommandConfig(var apiConfig: Map<String, Any>? = null,
                               var mappings: Map<String, String>? = null,
                               var apiCommands: Map<String, String>? = null) {
    companion object {
        fun fromJson(jsonObject: JSONObject): RemoteCommandConfig {
            val remoteCommandConfig = RemoteCommandConfig()

            jsonObject.optJSONObject(Settings.CONFIG)?.let {
                remoteCommandConfig.apiConfig = JsonUtils.mapFor(it)
            }

            jsonObject.optJSONObject(Settings.MAPPINGS)?.let {
                remoteCommandConfig.mappings = JsonUtils.mapFor(it)
                        .entries
                        .associate { entry ->
                            entry.key to entry.value as String
                        }
            }

            jsonObject.optJSONObject(Settings.COMMANDS)?.let {
                remoteCommandConfig.apiCommands = JsonUtils.mapFor(it)
                        .entries
                        .associate { entry ->
                            entry.key to entry.value as String
                        }
            }

            return remoteCommandConfig
        }

        fun toJson(remoteCommandConfig: RemoteCommandConfig): JSONObject {
            val json = JSONObject()
            remoteCommandConfig.apiConfig?.let {
                json.put(Settings.CONFIG, JsonUtils.jsonFor(it))
            }
            remoteCommandConfig.mappings?.let {
                json.put(Settings.MAPPINGS, JsonUtils.jsonFor(it))
            }
            remoteCommandConfig.apiCommands?.let {
                json.put(Settings.COMMANDS, JsonUtils.jsonFor(it))
            }

            return json
        }
    }
}