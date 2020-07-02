package com.tealium.core.model

import com.tealium.core.JsonUtils
import com.tealium.core.LibrarySettingsExtractor
import org.json.JSONObject

const val KEY_MODULES = "modules"
const val KEY_COLLECT_DISPATCHER = "collect_dispatcher"
const val KEY_TAG_MANAGEMENT_DISPATCHER = "tag_management_dispatcher"
const val KEY_BATCHING = "batching"
const val KEY_BATCH_SIZE = "batch_size"
const val KEY_INTERVAL = "interval"
const val KEY_MAX_QUEUE_SIZE = "max_queue_size"
const val KEY_EXPIRATION = "expiration"
const val KEY_BATTERY_SAVER = "battery_saver"
const val KEY_WIFI_ONLY = "wifi_only"
const val KEY_REFRESH_INTERVAL = "refresh_interval"
const val KEY_LOG_LEVEL = "log_level"
const val KEY_DISABLE_LIBRARY = "disable_library"
const val OPTIONAL_CONFIG_FLAGS = "optional_config_flags"


data class LibrarySettings(
        var modules: MutableMap<String, Any> = mutableMapOf(),
        var collectDispatcherEnabled: Boolean = false,
        var tagManagementDispatcherEnabled: Boolean = false,
        var batching: Batching = Batching(),
        var batterySaver: Boolean = false,
        var wifiOnly: Boolean = false,
        var refreshInterval: Int = 900,
        var disableLibrary: Boolean = false,
        var logLevel: String = "dev",    // todo: need to parse log level
        var optionalConfigFlags: MutableMap<String, Any> = mutableMapOf(),
        var url: String? = null) {

    companion object {
        fun fromJson(json: JSONObject): LibrarySettings {
            var librarySettings = LibrarySettings()

            json.optJSONObject(KEY_MODULES)?.let {
                librarySettings.modules = JsonUtils.mapFor(it)

                val collectDispatcherEnabled = it.optBoolean(KEY_COLLECT_DISPATCHER)
                librarySettings.collectDispatcherEnabled = if (collectDispatcherEnabled) collectDispatcherEnabled else false

                val tagManagementDispatcherEnabled = it.optBoolean(KEY_TAG_MANAGEMENT_DISPATCHER)
                librarySettings.tagManagementDispatcherEnabled = if (tagManagementDispatcherEnabled) tagManagementDispatcherEnabled else false
            }

            val collectDispatcherEnabled = json.optBoolean(KEY_COLLECT_DISPATCHER)
            librarySettings.collectDispatcherEnabled = if (collectDispatcherEnabled) collectDispatcherEnabled else false

            val tagManagementDispatcherEnabled = json.optBoolean(KEY_TAG_MANAGEMENT_DISPATCHER)
            librarySettings.tagManagementDispatcherEnabled = if (tagManagementDispatcherEnabled) tagManagementDispatcherEnabled else false

            json.optJSONObject(KEY_BATCHING)?.let {
                librarySettings.batching = Batching.fromJson(it)
            }

            val batterySaver = json.optBoolean(KEY_BATTERY_SAVER)
            librarySettings.batterySaver = if (batterySaver) batterySaver else false

            val wifiOnly = json.optBoolean(KEY_WIFI_ONLY)
            librarySettings.wifiOnly = if (wifiOnly) wifiOnly else false

            val librarySettingsIntervalString = json.optString(KEY_REFRESH_INTERVAL)
            librarySettings.refreshInterval = LibrarySettingsExtractor.timeConverter(librarySettingsIntervalString)

            val libraryEnabled = json.optBoolean(KEY_DISABLE_LIBRARY)
            librarySettings.disableLibrary = if (libraryEnabled) !libraryEnabled else false

            json.optJSONObject(OPTIONAL_CONFIG_FLAGS)?.let {
                librarySettings.optionalConfigFlags = JsonUtils.mapFor(it)
            }

            return librarySettings
        }
    }
}

data class Batching(var batchSize: Int = 1,
                    var interval: Int = 100,
                    var maxQueueSize: Int = 100,
                    var expiration: Int = 86400) {

    companion object {

        const val maxBatchSize = 10

        fun fromJson(json: JSONObject): Batching {
            return Batching().apply {
                var remoteBatchSize = json.optInt(KEY_BATCH_SIZE)
                if (remoteBatchSize > maxBatchSize) {
                    remoteBatchSize = maxBatchSize
                }
                batchSize = remoteBatchSize

                val intervalString = json.optString(KEY_INTERVAL)
                interval = LibrarySettingsExtractor.timeConverter(intervalString)

                maxQueueSize = json.optInt(KEY_MAX_QUEUE_SIZE)

                val expirationString = json.optString(KEY_EXPIRATION)
                expiration = LibrarySettingsExtractor.timeConverter(expirationString)
            }
        }
    }

}