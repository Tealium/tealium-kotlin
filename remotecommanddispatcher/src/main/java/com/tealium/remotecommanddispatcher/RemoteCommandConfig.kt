package com.tealium.remotecommanddispatcher

import com.tealium.core.JsonUtils
import org.json.JSONObject

data class Delimiters(
    var keysEqualityDelimiter: String = Settings.DEFAULT_EQUALITY_DELIMITER,
    var keysSeparationDelimiter: String = Settings.DEFAULT_SEPARATION_DELIMITER
)

data class RemoteCommandConfig(
    var apiConfig: Map<String, Any>? = null,
    var mappings: Map<String, String>? = null,
    var apiCommands: Map<String, String>? = null,
    var statics: Map<String, Any>? = null,
    var delimiters: Delimiters = Delimiters()
) {
    companion object {
        fun fromJson(jsonObject: JSONObject): RemoteCommandConfig {
            val remoteCommandConfig = RemoteCommandConfig()

            jsonObject.optJSONObject(Settings.CONFIG)?.let {
                if (it.has(Settings.KEYS_EQUALITY_DELIMITER)) {
                    remoteCommandConfig.delimiters.keysEqualityDelimiter = it.getString(Settings.KEYS_EQUALITY_DELIMITER)
                    it.remove(Settings.KEYS_EQUALITY_DELIMITER)
                }
                if (it.has(Settings.KEYS_SEPARATION_DELIMITER)) {
                    remoteCommandConfig.delimiters.keysSeparationDelimiter = it.getString(Settings.KEYS_SEPARATION_DELIMITER)
                    it.remove(Settings.KEYS_SEPARATION_DELIMITER)
                }

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

            jsonObject.optJSONObject(Settings.STATICS)?.let {
                remoteCommandConfig.statics = JsonUtils.mapFor(it)
                    .entries
                    .associate { entry ->
                        entry.key to JsonUtils.mapFor(entry.value as JSONObject)
                    }
            }

            return remoteCommandConfig
        }

        fun toJson(remoteCommandConfig: RemoteCommandConfig): JSONObject {
            val json = JSONObject()

            remoteCommandConfig.apiConfig?.let {
                val map = it.toMutableMap()

                if (remoteCommandConfig.delimiters.keysEqualityDelimiter != Settings.DEFAULT_EQUALITY_DELIMITER) {
                    map[Settings.KEYS_EQUALITY_DELIMITER] = remoteCommandConfig.delimiters.keysEqualityDelimiter
                }

                if (remoteCommandConfig.delimiters.keysSeparationDelimiter != Settings.DEFAULT_SEPARATION_DELIMITER) {
                    map[Settings.KEYS_SEPARATION_DELIMITER] = remoteCommandConfig.delimiters.keysSeparationDelimiter
                }

                json.put(Settings.CONFIG, JsonUtils.jsonFor(map.toMap()))
            }
            remoteCommandConfig.mappings?.let {
                json.put(Settings.MAPPINGS, JsonUtils.jsonFor(it))
            }
            remoteCommandConfig.apiCommands?.let {
                json.put(Settings.COMMANDS, JsonUtils.jsonFor(it))
            }
            remoteCommandConfig.statics?.let {
                val obj = JSONObject()
                it.forEach { (key, value) ->
                    obj.put(key, JsonUtils.jsonFor(value as Map<String, Any>))
                }
                json.put(Settings.STATICS, obj)
            }

            return json
        }
    }
}