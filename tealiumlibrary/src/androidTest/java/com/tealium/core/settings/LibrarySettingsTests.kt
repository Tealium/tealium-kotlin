package com.tealium.core.settings

import com.tealium.core.LogLevel
import junit.framework.Assert.assertEquals
import org.json.JSONObject
import org.junit.Test

class LibrarySettingsTests {

    @Test
    fun librarySettings_DeserializesFromJsonCorrectly() {
        val librarySettingsJson = "{\n" +
                "  \"collect_dispatcher\": false,\n" +
                "  \"tag_management_dispatcher\": true,\n" +
                "  \"batching\": {\n" +
                "    \"batch_size\": 10,\n" +
                "    \"max_queue_size\": 999,\n" +
                "    \"expiration\": \"1d\"\n" +
                "  },\n" +
                "  \"battery_saver\": true,\n" +
                "  \"wifi_only\": false,\n" +
                "  \"refresh_interval\": \"15m\",\n" +
                "  \"log_level\": \"dev\",\n" +
                "  \"disable_library\": false\n" +
                "}"
        val settings = LibrarySettings.fromJson(JSONObject(librarySettingsJson))

        assertEquals(false, settings.collectDispatcherEnabled)
        assertEquals(true, settings.tagManagementDispatcherEnabled)
        assertEquals(10, settings.batching.batchSize)
        assertEquals(999, settings.batching.maxQueueSize)
        assertEquals(24 * 60 * 60, settings.batching.expiration)
        assertEquals(true, settings.batterySaver)
        assertEquals(false, settings.wifiOnly)
        assertEquals(15 * 60, settings.refreshInterval)
        assertEquals(LogLevel.DEV, settings.logLevel)
        assertEquals(false, settings.disableLibrary)
    }

    @Test
    fun librarySettings_DeserializesFromMpsJsonCorrectly() {
        val librarySettingsJson = "{\n" +
                "  \"enable_collect\": false,\n" +
                "  \"enable_tag_management\": true,\n" +
                "  \"event_batch_size\": 10," +
                "  \"offline_dispatch_limit\": 999," +
                "  \"dispatch_expiration\": 1," +
                "  \"battery_saver\": true,\n" +
                "  \"wifi_only_sending\": false,\n" +
                "  \"minutes_between_refresh\": \"15.0\",\n" +
                "  \"override_log\": \"dev\",\n" +
                "  \"_is_enabled\": false\n" +
                "}"
        val settings = LibrarySettings.fromMobilePublishSettings(JSONObject(librarySettingsJson))

        assertEquals(false, settings.collectDispatcherEnabled)
        assertEquals(true, settings.tagManagementDispatcherEnabled)
        assertEquals(10, settings.batching.batchSize)
        assertEquals(999, settings.batching.maxQueueSize)
        assertEquals(24 * 60 * 60, settings.batching.expiration)
        assertEquals(true, settings.batterySaver)
        assertEquals(false, settings.wifiOnly)
        assertEquals(15 * 60, settings.refreshInterval)
        assertEquals(LogLevel.DEV, settings.logLevel)
        assertEquals(false, settings.disableLibrary)
    }

    @Test
    fun librarySettings_SerializesToJsonCorrectly() {
        val librarySettingsJson = JSONObject("{\n" +
                "  \"collect_dispatcher\": true,\n" +
                "  \"tag_management_dispatcher\": true,\n" +
                "  \"batching\": {\n" +
                "    \"batch_size\": 10,\n" +
                "    \"max_queue_size\": 999,\n" +
                "    \"expiration\": \"86400s\"\n" +
                "  },\n" +
                "  \"battery_saver\": true,\n" +
                "  \"wifi_only\": true,\n" +
                "  \"refresh_interval\": \"900s\",\n" +
                "  \"log_level\": \"DEV\",\n" +
                "  \"disable_library\": true\n" +
                "}")
        val librarySettings = LibrarySettings(
                collectDispatcherEnabled = true,
                tagManagementDispatcherEnabled = true,
                batching = Batching(batchSize = 10, maxQueueSize = 999, expiration = 24 * 60 * 60),
                batterySaver = true,
                wifiOnly = true,
                refreshInterval = 15 * 60,
                logLevel = LogLevel.DEV,
                disableLibrary = true
        )
        val settings = LibrarySettings.toJson(librarySettings)
        assertEquals(librarySettingsJson.toString(), settings.toString())
    }

    @Test
    fun librarySettings_SerializeDeserializeAreEqual() {
        val librarySettings = LibrarySettings(
                collectDispatcherEnabled = true,
                tagManagementDispatcherEnabled = true,
                batching = Batching(batchSize = 10, maxQueueSize = 999, expiration = 24 * 60 * 60),
                batterySaver = true,
                wifiOnly = true,
                refreshInterval = 15 * 60,
                logLevel = LogLevel.DEV,
                disableLibrary = true
        )
        val settings = LibrarySettings.toJson(librarySettings)
        val settingsFromJson = LibrarySettings.fromJson(settings)

        assertEquals(librarySettings, settingsFromJson)
    }
}