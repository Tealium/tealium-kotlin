package com.tealium.core.settings

import android.app.Application
import android.os.Build
import android.util.Log
import com.tealium.core.Environment
import com.tealium.core.Loader
import com.tealium.core.TealiumConfig
import com.tealium.core.messaging.EventRouter
import com.tealium.core.network.NetworkClient
import com.tealium.core.network.ResourceEntity
import com.tealium.core.network.ResponseStatus
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class LibrarySettingsManagerTest {

    @MockK
    lateinit var mockNetworkClient: NetworkClient

    @RelaxedMockK
    lateinit var mockEventRouter: EventRouter

    @MockK
    lateinit var mockLoader: Loader

    @MockK
    lateinit var mockScope: CoroutineScope

    @MockK
    lateinit var mockFile: File

    @MockK
    lateinit var context: Application
    lateinit var config: TealiumConfig

    val defaultLibrarySettings = LibrarySettings()
    val defaultJsonLibrarySettings = "{\n" +
            "  \"collect_dispatcher\": false,\n" +
            "  \"tag_management_dispatcher\": false,\n" +
            "  \"batching\": {\n" +
            "    \"batch_size\": 10,\n" +
            "    \"max_queue_size\": 100,\n" +
            "    \"expiration\": \"1d\"\n" +
            "  },\n" +
            "  \"battery_saver\": false,\n" +
            "  \"wifi_only\": false,\n" +
            "  \"refresh_interval\": \"15m\",\n" +
            "  \"log_level\": \"dev\",\n" +
            "  \"disable_library\": false\n" +
            "}"

    val backgroundScope = CoroutineScope(Dispatchers.IO)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { context.filesDir } returns mockFile
        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        mockNetworkClient = mockk()

        config = TealiumConfig(context, "test", "profile", Environment.QA)
    }

    @Test
    fun librarySettingsNotNull() {
        every { mockLoader.loadFromAsset(any()) } returns null
        val librarySettingsManager = LibrarySettingsManager(
            config,
            mockNetworkClient,
            mockLoader,
            eventRouter = mockEventRouter,
            backgroundScope = mockScope
        )
        assertNotNull(librarySettingsManager.librarySettings)
    }

    @Test
    fun librarySettingsDefaultsMatchDefaultLibrarySettings() {
        every { mockLoader.loadFromAsset(any()) } returns null
        val librarySettingsManager = LibrarySettingsManager(
            config,
            mockNetworkClient,
            mockLoader,
            eventRouter = mockEventRouter,
            backgroundScope = mockScope
        )
        assertEquals(defaultLibrarySettings, librarySettingsManager.librarySettings)
    }

    @Test
    fun librarySettingsInitialShouldLoadFromAsset() {
        config.useRemoteLibrarySettings = false
        every { mockLoader.loadFromAsset(any()) } returns null
        LibrarySettingsManager(
            config,
            mockNetworkClient,
            mockLoader,
            eventRouter = mockEventRouter,
            backgroundScope = mockScope
        )

        verify {
            mockLoader.loadFromAsset(any())
        }
    }

    @Test
    fun librarySettingsInitialShouldLoadFromCache() = runBlocking {
        config.useRemoteLibrarySettings = true
        every { mockLoader.loadFromFile(any()) } returns null
        coEvery { mockNetworkClient.getResourceEntity(any()) } returns null
        LibrarySettingsManager(
            config,
            mockNetworkClient,
            mockLoader,
            eventRouter = mockEventRouter,
            backgroundScope = backgroundScope
        )

        coVerify {
            mockLoader.loadFromFile(any())
        }
    }

    @Test
    fun librarySettingsInitialShouldLoadFromOverriddenDefaultWhenNoAsset() {
        val defaultOverride = LibrarySettings(
            collectDispatcherEnabled = true,
            tagManagementDispatcherEnabled = true,
            batterySaver = true
        )

        config.useRemoteLibrarySettings = false
        config.overrideDefaultLibrarySettings = defaultOverride

        every { mockLoader.loadFromAsset(any()) } returns null
        val librarySettingsManager = LibrarySettingsManager(
            config,
            mockNetworkClient,
            mockLoader,
            eventRouter = mockEventRouter,
            backgroundScope = mockScope
        )

        verify {
            mockLoader.loadFromAsset(any())
        }

        assertNotEquals(LibrarySettings(), librarySettingsManager.librarySettings)
        assertEquals(defaultOverride, librarySettingsManager.librarySettings)
    }

    @Test
    fun librarySettingsInitialShouldLoadFromOverriddenDefaultWhenNoCache() = runBlocking {
        val defaultOverride = LibrarySettings(
            collectDispatcherEnabled = true,
            tagManagementDispatcherEnabled = true,
            batterySaver = true
        )

        config.useRemoteLibrarySettings = true
        config.overrideDefaultLibrarySettings = defaultOverride

        every { mockLoader.loadFromFile(any()) } returns null
        coEvery { mockNetworkClient.getResourceEntity(any()) } returns null
        val librarySettingsManager = LibrarySettingsManager(
            config,
            mockNetworkClient,
            mockLoader,
            eventRouter = mockEventRouter,
            backgroundScope = backgroundScope
        )

        coVerify {
            mockLoader.loadFromFile(any())
        }

        assertNotEquals(defaultLibrarySettings, librarySettingsManager.librarySettings)
        assertEquals(defaultOverride, librarySettingsManager.librarySettings)
    }

    @Test
    fun assetSettingsOverrideDefaultsWhenPresent() = runBlocking {
        val jsonLibrarySettings = "{\n" +
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
        config.useRemoteLibrarySettings = false
        every { mockLoader.loadFromAsset(any()) } returns jsonLibrarySettings
        val librarySettingsManager = LibrarySettingsManager(
            config,
            mockNetworkClient,
            mockLoader,
            eventRouter = mockEventRouter,
            backgroundScope = backgroundScope
        )

        assertNotEquals(defaultLibrarySettings, librarySettingsManager.librarySettings)
        assertEquals(false, librarySettingsManager.librarySettings.collectDispatcherEnabled)
        assertEquals(true, librarySettingsManager.librarySettings.tagManagementDispatcherEnabled)
        assertEquals(999, librarySettingsManager.librarySettings.batching.maxQueueSize)
        assertEquals(true, librarySettingsManager.librarySettings.batterySaver)
    }

    @Test
    fun remoteSettingsOverrideDefaultsWhenPresent() = runBlocking {
        val jsonLibrarySettings = "{\n" +
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
        config.useRemoteLibrarySettings = true
        every { mockLoader.loadFromFile(any()) } returns jsonLibrarySettings
        coEvery { mockNetworkClient.getResourceEntity(any(), any()) } returns null
        val librarySettingsManager = LibrarySettingsManager(
            config,
            mockNetworkClient,
            mockLoader,
            eventRouter = mockEventRouter,
            backgroundScope = backgroundScope
        )

        assertNotEquals(defaultLibrarySettings, librarySettingsManager.librarySettings)
        assertEquals(false, librarySettingsManager.librarySettings.collectDispatcherEnabled)
        assertEquals(true, librarySettingsManager.librarySettings.tagManagementDispatcherEnabled)
        assertEquals(999, librarySettingsManager.librarySettings.batching.maxQueueSize)
        assertEquals(true, librarySettingsManager.librarySettings.batterySaver)
    }

    @Test
    fun remoteJsonSettingsOverrideCacheWhenPresent() = runBlocking {
        val jsonLibrarySettings = "{\n" +
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
        val resource = ResourceEntity(jsonLibrarySettings, status = ResponseStatus.Success)
        config.useRemoteLibrarySettings = true
        config.overrideLibrarySettingsUrl = "tealium-settings.json"
        every { mockLoader.loadFromFile(any()) } returns defaultJsonLibrarySettings
        coEvery { mockNetworkClient.getResourceEntity(any(), any()) } returns resource
        val librarySettingsManager = LibrarySettingsManager(
            config,
            mockNetworkClient,
            mockLoader,
            eventRouter = mockEventRouter,
            backgroundScope = backgroundScope
        )

        delay(1000)
        verify(exactly = 1, timeout = 3000) {
            mockEventRouter.onLibrarySettingsUpdated(any())
        }

        assertEquals(false, librarySettingsManager.librarySettings.collectDispatcherEnabled)
        assertEquals(true, librarySettingsManager.librarySettings.tagManagementDispatcherEnabled)
        assertEquals(999, librarySettingsManager.librarySettings.batching.maxQueueSize)
        assertEquals(true, librarySettingsManager.librarySettings.batterySaver)
    }

    @Test
    fun remoteJsonSettingsDoesNotCrashWhenMalformedAndReturnsDefault() = runBlocking {
        val malformedJson = "{\n" +
                "  \"collect dispatcher\": true,\n" +
                "  \"tag management_dispatcher\": true,\n" +
                "  \"batching\": {\n" +
                "    \"batch_size\": 10,\n" +
                "    \"max_queue_size\": 100,\n" +
                "    \"expiration\" " +
                "}"
        val malFormedResource = ResourceEntity(malformedJson)
        config.useRemoteLibrarySettings = true
        config.overrideLibrarySettingsUrl = "tealium-settings.json"
        every { mockLoader.loadFromFile(any()) } returns null
        coEvery { mockNetworkClient.getResourceEntity(any(), any()) } returns malFormedResource
        val librarySettingsManager = LibrarySettingsManager(
            config,
            mockNetworkClient,
            mockLoader,
            eventRouter = mockEventRouter,
            backgroundScope = backgroundScope
        )

        verify(exactly = 0, timeout = 1500) {
            mockEventRouter.onLibrarySettingsUpdated(any())
        }

        assertEquals(defaultLibrarySettings, librarySettingsManager.librarySettings)
        assertEquals(false, librarySettingsManager.librarySettings.collectDispatcherEnabled)
        assertEquals(false, librarySettingsManager.librarySettings.tagManagementDispatcherEnabled)
        assertEquals(100, librarySettingsManager.librarySettings.batching.maxQueueSize)
        assertEquals(false, librarySettingsManager.librarySettings.batterySaver)
    }

    @Test
    fun cachedJsonSettingsDoesNotCrashWhenMalformedAndReturnsDefault() = runBlocking {
        val malformedJson = "{\n" +
                "  \"collect dispatcher\": true,\n" +
                "  \"tag management_dispatcher\": true,\n" +
                "  \"batching\": {\n" +
                "    \"batch_size\": 10,\n" +
                "    \"max_queue_size\": 100,\n" +
                "    \"expiration\" " +
                "}"
        val malFormedResource = ResourceEntity(malformedJson)
        config.useRemoteLibrarySettings = true
        config.overrideLibrarySettingsUrl = "tealium-settings.json"
        every { mockLoader.loadFromFile(any()) } returns malformedJson
        coEvery { mockNetworkClient.getResourceEntity(any(), any()) } returns malFormedResource
        val librarySettingsManager = LibrarySettingsManager(
            config,
            mockNetworkClient,
            mockLoader,
            eventRouter = mockEventRouter,
            backgroundScope = backgroundScope
        )

        verify(exactly = 0, timeout = 1500) {
            mockEventRouter.onLibrarySettingsUpdated(any())
        }

        assertEquals(defaultLibrarySettings, librarySettingsManager.librarySettings)
        assertEquals(false, librarySettingsManager.librarySettings.collectDispatcherEnabled)
        assertEquals(false, librarySettingsManager.librarySettings.tagManagementDispatcherEnabled)
        assertEquals(100, librarySettingsManager.librarySettings.batching.maxQueueSize)
        assertEquals(false, librarySettingsManager.librarySettings.batterySaver)
    }

    @Test
    fun remoteHtmlSettingsOverrideCacheWhenPresent() = runBlocking {
        val htmlLibrarySettings = """<!DOCTYPE html>
                <!--tealium tag management - mobile.webview ut4.0.202006041357, Copyright 2020 Tealium.com Inc. All Rights Reserved.-->
                <html>
                <head><title>Tealium Mobile Webview</title></head>
                <body>
                <script type="text/javascript">var utag_cfg_ovrd={noview:true};var mps = {"4":{"_is_enabled":"true","battery_saver":"true","dispatch_expiration":"-1","event_batch_size":"1","ivar_tracking":"true","mobile_companion":"true","offline_dispatch_limit":"-1","ui_auto_tracking":"true","wifi_only_sending":"false"},"5":{"_is_enabled":"true","battery_saver":"true","dispatch_expiration":"-1","enable_collect":"true","enable_s2s_legacy":"false","enable_tag_management":"true","event_batch_size":"1","minutes_between_refresh":"15.0","offline_dispatch_limit":"100","override_log":"dev","wifi_only_sending":"false"},"_firstpublish":"true"}</script>
                <script type="text/javascript">
                    if (("" + location.search).indexOf("sdk_session_count=true") > -1) {
                      window.utag_cfg_ovrd = window.utag_cfg_ovrd || {};
                      window.utag_cfg_ovrd.no_session_count = true;
                    } 
                </script>
                <script type="text/javascript" src="//tags.tiqcdn.com/utag/tealium/test/dev/utag.js"></script>
                </body>
                </html>"""
        val resource = ResourceEntity(htmlLibrarySettings, status = ResponseStatus.Success)
        config.useRemoteLibrarySettings = true
        config.overrideLibrarySettingsUrl = "mobile.html"
        every { mockLoader.loadFromFile(any()) } returns defaultJsonLibrarySettings
        coEvery { mockNetworkClient.getResourceEntity(any(), any()) } returns resource
        val librarySettingsManager = LibrarySettingsManager(
            config,
            mockNetworkClient,
            mockLoader,
            eventRouter = mockEventRouter,
            backgroundScope = backgroundScope
        )

        coVerify(exactly = 1, timeout = 1500) {
            mockEventRouter.onLibrarySettingsUpdated(any())
        }

        assertEquals(true, librarySettingsManager.librarySettings.collectDispatcherEnabled)
        assertEquals(true, librarySettingsManager.librarySettings.tagManagementDispatcherEnabled)
        assertEquals(100, librarySettingsManager.librarySettings.batching.maxQueueSize)
        assertEquals(true, librarySettingsManager.librarySettings.batterySaver)
    }

    @Test
    fun localSettingsDoesNotOverrideDefaultsOnMalformedJson() = runBlocking {
        val malformedJson = "{\n" +
                "  \"collect dispatcher\": true,\n" +
                "  \"tag management_dispatcher\": true,\n" +
                "  \"batching\": {\n" +
                "    \"batch_size\": 10,\n" +
                "    \"max_queue_size\": 100,\n" +
                "    \"expiration\": \"1d\"\n" +
                "  },\n" +
                "  \"battery_saver\": false,\n" +
                "  \"wifi_only\": false,\n" +
                "  \"refresh_interval\": \"15m\",\n" +
                "  \"log_level\": \"dev\",\n" +
                "  \"disable_library\": false\n" +
                "}"
        config.useRemoteLibrarySettings = false
        every { mockLoader.loadFromAsset(any<String>()) } returns malformedJson
        val librarySettingsManager = LibrarySettingsManager(
            config,
            mockNetworkClient,
            mockLoader,
            eventRouter = mockEventRouter,
            backgroundScope = backgroundScope
        )
        val default = librarySettingsManager.librarySettings

        assertEquals(default, librarySettingsManager.librarySettings)

        coVerify(timeout = 500, exactly = 0) {
            mockEventRouter.onLibrarySettingsUpdated(any())
        }
    }

    @Test
    fun fetchRemoteLibrarySettings_AdheresTo_RefreshInterval() = runBlocking {
        config.useRemoteLibrarySettings = true
        every { mockLoader.loadFromAsset(any()) } returns null
        every { mockLoader.loadFromFile(any()) } returns null
        coEvery { mockNetworkClient.getResourceEntity(any(), any()) } returns ResourceEntity(
            null,
            null,
            ResponseStatus.Non200Response(304)
        )
        val librarySettingsManager = LibrarySettingsManager(
            config,
            mockNetworkClient,
            mockLoader,
            eventRouter = mockEventRouter,
            backgroundScope = backgroundScope
        )
        // these should be ignored
        librarySettingsManager.fetchLibrarySettings()
        librarySettingsManager.fetchLibrarySettings()

        coVerify(timeout = 5000, exactly = 1) {
            mockNetworkClient.getResourceEntity(any(), any())
        }
    }

    @Test
    fun fetchRemoteLibrarySettings_Updates_RefreshInterval() = runBlocking {
        config.useRemoteLibrarySettings = true
        config.overrideLibrarySettingsUrl = "test.com/settings.json"
        every { mockLoader.loadFromAsset(any()) } returns null
        every { mockLoader.loadFromFile(any()) } returns null
        coEvery { mockNetworkClient.getResourceEntity(any(), any()) } returns ResourceEntity(
            "{\n" +
                    "  \"collect_dispatcher\": false,\n" +
                    "  \"tag_management_dispatcher\": false,\n" +
                    "  \"batching\": {\n" +
                    "    \"batch_size\": 10,\n" +
                    "    \"max_queue_size\": 100,\n" +
                    "    \"expiration\": \"1d\"\n" +
                    "  },\n" +
                    "  \"battery_saver\": false,\n" +
                    "  \"wifi_only\": false,\n" +
                    "  \"refresh_interval\": \"25m\",\n" + // 25 minutes
                    "  \"log_level\": \"dev\",\n" +
                    "  \"disable_library\": false\n" +
                    "}",
            null,
            ResponseStatus.Success
        )
        val librarySettingsManager = LibrarySettingsManager(
            config,
            mockNetworkClient,
            mockLoader,
            eventRouter = mockEventRouter,
            backgroundScope = backgroundScope
        )

        delay(500)
        assertEquals(25, librarySettingsManager.resourceRetriever.refreshInterval)
    }
}