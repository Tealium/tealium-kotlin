package com.tealium.core.settings

import com.tealium.core.LogLevel
import org.json.JSONObject

private const val KEY_COLLECT_DISPATCHER = "collect_dispatcher"
private const val KEY_TAG_MANAGEMENT_DISPATCHER = "tag_management_dispatcher"
private const val KEY_BATCHING = "batching"
private const val KEY_BATCH_SIZE = "batch_size"
private const val KEY_MAX_QUEUE_SIZE = "max_queue_size"
private const val KEY_EXPIRATION = "expiration"
private const val KEY_BATTERY_SAVER = "battery_saver"
private const val KEY_WIFI_ONLY = "wifi_only"
private const val KEY_REFRESH_INTERVAL = "refresh_interval"
private const val KEY_LOG_LEVEL = "log_level"
private const val KEY_DISABLE_LIBRARY = "disable_library"
private const val KEY_ETAG = "etag"

private const val MPS_KEY_COLLECT_DISPATCHER = "enable_collect"
private const val MPS_KEY_TAG_MANAGEMENT_DISPATCHER = "enable_tag_management"
private const val MPS_KEY_BATCH_SIZE = "event_batch_size"
private const val MPS_KEY_INTERVAL = "minutes_between_refresh"
private const val MPS_KEY_MAX_QUEUE_SIZE = "offline_dispatch_limit"
private const val MPS_KEY_EXPIRATION = "dispatch_expiration"
private const val MPS_KEY_BATTERY_SAVER = "battery_saver"
private const val MPS_KEY_WIFI_ONLY = "wifi_only_sending"
private const val MPS_KEY_LOG_LEVEL = "override_log"
private const val MPS_KEY_DISABLE_LIBRARY = "_is_enabled"
private const val MPS_KEY_ETAG = "etag"

/**
 * These settings describe the options that can be remotely configured, either via a JSON file, or
 * through the Mobile Publish Settings available in Tealium IQ and the Mobile.html
 * @see com.tealium.core.TealiumConfig.overrideLibrarySettingsUrl
 * @see com.tealium.core.settings.Batching
 *
 * @param collectDispatcherEnabled Whether or not events should be sent using Collect Dispatcher (default: true)
 * @param tagManagementDispatcherEnabled Whether or not events should be sent using TagManagement Dispatcher (default: true)
 * @param batching Configuration for batch size, if required, as well as queue limits and Dispatch time-to-live
 * @param batterySaver set to true to restrict the sending of events in low battery scenarios, otherwise false (default: false)
 * @param wifiOnly set to true to only send events in when the device has WiFi capabilities, otherwise false (default: false)
 * @param refreshInterval the time, in seconds, before the [LibrarySettings] are fetched again (default 900s; 15 minutes)
 * @param disableLibrary set to true to disable all modules, and effectively disable all processing of new events (default: false)
 * @param logLevel sets the LogLevel to consider when writing out Tealium log messages
 */
data class LibrarySettings(
        var collectDispatcherEnabled: Boolean = true,
        var tagManagementDispatcherEnabled: Boolean = true,
        var batching: Batching = Batching(),
        var batterySaver: Boolean = false,
        var wifiOnly: Boolean = false,
        var refreshInterval: Int = 900,
        var disableLibrary: Boolean = false,
        var logLevel: LogLevel = LogLevel.PROD,
        var etag: String? = null
) {

    companion object {
        fun toJson(librarySettings: LibrarySettings) : JSONObject {
            val json = JSONObject()

            json.put(KEY_COLLECT_DISPATCHER, librarySettings.collectDispatcherEnabled)
            json.put(KEY_TAG_MANAGEMENT_DISPATCHER, librarySettings.tagManagementDispatcherEnabled)
            json.put(KEY_BATCHING, Batching.toJson(librarySettings.batching))
            json.put(KEY_BATTERY_SAVER, librarySettings.batterySaver)
            json.put(KEY_WIFI_ONLY, librarySettings.wifiOnly)
            json.put(KEY_REFRESH_INTERVAL, "${librarySettings.refreshInterval}s")
            json.put(KEY_LOG_LEVEL, librarySettings.logLevel.name)
            json.put(KEY_DISABLE_LIBRARY, librarySettings.disableLibrary)
            json.put(KEY_ETAG, librarySettings.etag)

            return json
        }

        fun fromJson(json: JSONObject): LibrarySettings {
            val librarySettings = LibrarySettings()

            librarySettings.collectDispatcherEnabled = json.optBoolean(KEY_COLLECT_DISPATCHER, false)
            librarySettings.tagManagementDispatcherEnabled = json.optBoolean(KEY_TAG_MANAGEMENT_DISPATCHER, false)

            json.optJSONObject(KEY_BATCHING)?.let {
                librarySettings.batching = Batching.fromJson(it)
            }

            librarySettings.batterySaver = json.optBoolean(KEY_BATTERY_SAVER, false)
            librarySettings.wifiOnly = json.optBoolean(KEY_WIFI_ONLY, false)

            val logLevel = json.optString(KEY_LOG_LEVEL, "")
            librarySettings.logLevel = LogLevel.fromString(logLevel)

            val librarySettingsIntervalString = json.optString(KEY_REFRESH_INTERVAL)
            librarySettings.refreshInterval = LibrarySettingsExtractor.timeConverter(librarySettingsIntervalString)
            librarySettings.disableLibrary = json.optBoolean(KEY_DISABLE_LIBRARY, false)
            val tag = json.optString(KEY_ETAG)
            librarySettings.etag = if (!tag.isNullOrEmpty()) tag else null

            return librarySettings
        }

        fun fromMobilePublishSettings(json: JSONObject): LibrarySettings {
            val librarySettings = LibrarySettings()

            // MPS known dispatchers
            librarySettings.collectDispatcherEnabled = json.optBoolean(MPS_KEY_COLLECT_DISPATCHER, false)
            librarySettings.tagManagementDispatcherEnabled = json.optBoolean(MPS_KEY_TAG_MANAGEMENT_DISPATCHER, false)

            val batchSize = json.optInt(MPS_KEY_BATCH_SIZE, 1)
            val expiration = LibrarySettingsExtractor.timeConverter("${json.optInt(MPS_KEY_EXPIRATION)}d")
            val queueSize = json.optInt(MPS_KEY_MAX_QUEUE_SIZE)
            librarySettings.batching = Batching(batchSize = batchSize,
                    expiration = expiration,
                    maxQueueSize = queueSize)

            librarySettings.batterySaver = json.optBoolean(MPS_KEY_BATTERY_SAVER)
            librarySettings.wifiOnly = json.optBoolean(MPS_KEY_WIFI_ONLY, false)

            val logLevel = json.optString(MPS_KEY_LOG_LEVEL, "")
            librarySettings.logLevel = LogLevel.fromString(logLevel)

            val librarySettingsInterval = LibrarySettingsExtractor.timeConverter("${json.optString(MPS_KEY_INTERVAL)}m")
            librarySettings.refreshInterval = librarySettingsInterval
            librarySettings.disableLibrary = !json.optBoolean(MPS_KEY_DISABLE_LIBRARY, false)
            librarySettings.etag = json.optString(MPS_KEY_ETAG)

            return librarySettings
        }
    }
}

/**
 * Configures the available batching and queueing options.
 *
 * @param batchSize The number of dispatches to wait for before sending in a single payload. (default: 1 - i.e. no batching)
 * There is a limit of 10 for this value.
 * @param maxQueueSize The maximum number of events to persist on disk while sending is not possible.
 * Dispatches are evicted in an oldest-first manner if this limit is exceeded. (default: 100)
 * @param expiration The time-to-live, in seconds, for dispatches that remain on disk while sending is not possible. (default: 86400)
 */
data class Batching(var batchSize: Int = 1,
                    var maxQueueSize: Int = 100,
                    var expiration: Int = 86400) {

    init {
        batchSize = sanitizeBatchSize(batchSize)
    }

    companion object {

        const val maxBatchSize = 10

        fun toJson(batching: Batching): JSONObject {
            val json = JSONObject()
            json.put(KEY_BATCH_SIZE, batching.batchSize)
            json.put(KEY_MAX_QUEUE_SIZE, batching.maxQueueSize)
            json.put(KEY_EXPIRATION, "${batching.expiration}s")
            return json
        }

        fun fromJson(json: JSONObject): Batching {
            return Batching().apply {
                batchSize = sanitizeBatchSize(json.optInt(KEY_BATCH_SIZE))

                maxQueueSize = json.optInt(KEY_MAX_QUEUE_SIZE)

                val expirationString = json.optString(KEY_EXPIRATION)
                expiration = LibrarySettingsExtractor.timeConverter(expirationString)
            }
        }

        private fun sanitizeBatchSize(batchSize: Int): Int {
            return if (batchSize > maxBatchSize) {
                maxBatchSize
            } else if (batchSize <= 0) {
                1
            } else batchSize
        }
    }
}