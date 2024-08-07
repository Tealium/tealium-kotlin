package com.tealium.remotecommanddispatcher

import com.tealium.core.JsonUtils
import com.tealium.core.network.ResourceEntity
import org.json.JSONException
import org.json.JSONObject

data class Delimiters(
    val keysEqualityDelimiter: String = Settings.DEFAULT_EQUALITY_DELIMITER,
    val keysSeparationDelimiter: String = Settings.DEFAULT_SEPARATION_DELIMITER
)

data class RemoteCommandConfig(
    var apiConfig: Map<String, Any>? = null,
    var mappings: Map<String, String>? = null,
    var apiCommands: Map<String, String>? = null,
    var statics: Map<String, Any>? = null,
    var delimiters: Delimiters = Delimiters(),
    val etag: String? = null
) {
    companion object {
        fun fromResourceEntity(entity: ResourceEntity): RemoteCommandConfig? {
            val (response, etag) = entity
            if (response == null) return null

            val jsonObject = try {
                JSONObject(response)
            } catch (ignored: JSONException) {
                return null
            }

            jsonObject.put(Settings.ETAG, etag)

            return fromJson(jsonObject)
        }

        fun fromJson(jsonObject: JSONObject): RemoteCommandConfig {
            val configJson = jsonObject.optJSONObject(Settings.CONFIG)
            val delimiters = configJson?.let {
                var separatorDelimiter: String? = null
                var equalityDelimiter: String? = null
                if (it.has(Settings.KEYS_EQUALITY_DELIMITER)) {
                    equalityDelimiter = it.getString(Settings.KEYS_EQUALITY_DELIMITER)
                    it.remove(Settings.KEYS_EQUALITY_DELIMITER)
                }
                if (it.has(Settings.KEYS_SEPARATION_DELIMITER)) {
                    separatorDelimiter = it.getString(Settings.KEYS_SEPARATION_DELIMITER)
                    it.remove(Settings.KEYS_SEPARATION_DELIMITER)
                }

                Delimiters(
                    equalityDelimiter ?: Settings.DEFAULT_EQUALITY_DELIMITER,
                    separatorDelimiter ?: Settings.DEFAULT_SEPARATION_DELIMITER
                )
            }

            val apiConfig = configJson?.let { JsonUtils.mapFor(it) }

            val mappings = jsonObject.optJSONObject(Settings.MAPPINGS)?.let {
                JsonUtils.mapFor(it)
                    .entries
                    .associate { entry ->
                        entry.key to entry.value as String
                    }
            }

            val commands = jsonObject.optJSONObject(Settings.COMMANDS)?.let {
                JsonUtils.mapFor(it)
                    .entries
                    .associate { entry ->
                        entry.key to entry.value as String
                    }
            }

            val statics = jsonObject.optJSONObject(Settings.STATICS)?.let {
                JsonUtils.mapFor(it)
                    .entries
                    .associate { entry ->
                        entry.key to JsonUtils.mapFor(entry.value as JSONObject)
                    }
            }

            val etag: String? = try {
                jsonObject.getString(Settings.ETAG)
            } catch (ignored: JSONException) {
                null
            }

            return RemoteCommandConfig(
                apiConfig = apiConfig,
                mappings = mappings,
                apiCommands = commands,
                statics = statics,
                delimiters = delimiters ?: Delimiters(),
                etag = etag
            )
        }

        fun toJson(remoteCommandConfig: RemoteCommandConfig): JSONObject {
            val json = JSONObject()

            remoteCommandConfig.apiConfig?.let {
                val map = it.toMutableMap()

                if (remoteCommandConfig.delimiters.keysEqualityDelimiter != Settings.DEFAULT_EQUALITY_DELIMITER) {
                    map[Settings.KEYS_EQUALITY_DELIMITER] =
                        remoteCommandConfig.delimiters.keysEqualityDelimiter
                }

                if (remoteCommandConfig.delimiters.keysSeparationDelimiter != Settings.DEFAULT_SEPARATION_DELIMITER) {
                    map[Settings.KEYS_SEPARATION_DELIMITER] =
                        remoteCommandConfig.delimiters.keysSeparationDelimiter
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
            remoteCommandConfig.etag?.let {
                json.put(Settings.ETAG, it)
            }

            return json
        }
    }
}