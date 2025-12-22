package com.tealium.core.settings

import android.app.Application
import android.os.Build
import com.tealium.core.Environment
import com.tealium.core.Loader
import com.tealium.core.TealiumConfig
import com.tealium.core.messaging.EventRouter
import com.tealium.core.network.NetworkClient
import com.tealium.core.network.ResourceEntity
import com.tealium.core.network.ResponseStatus
import io.mockk.MockKAnnotations
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    lateinit var mockFile: File

    @MockK
    lateinit var context: Application
    lateinit var config: TealiumConfig

    val defaultLibrarySettings = LibrarySettings()
    val defaultJsonLibrarySettings = """{
          "collect_dispatcher": false,
          "tag_management_dispatcher": false,
          "batching": {
            "batch_size": 10,
            "max_queue_size": 100,
            "expiration": "1d"
          },
          "battery_saver": false,
          "wifi_only": false,
          "refresh_interval": "15m",
          "log_level": "dev",
          "disable_library": false
        }""".trimIndent()

    val malformedJson = """{
          "collect dispatcher": true,
          "tag management_dispatcher"
        """.trimIndent()

    val backgroundScope = CoroutineScope(Dispatchers.IO)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { context.filesDir } returns mockFile
        mockNetworkClient = mockk()

        config = TealiumConfig(context, "test", "profile", Environment.QA)
    }

    @Test
    fun librarySettings_Should_Not_Be_Null() {
        mockStorage(cache = null, asset = null)
        val librarySettingsManager = createLibrarySettingsManager()

        assertNotNull(librarySettingsManager.librarySettings)
    }

    @Test
    fun librarySettings_Defaults_Should_Match_Default_LibrarySettings() {
        mockStorage(cache = null, asset = null)
        val librarySettingsManager = createLibrarySettingsManager()

        assertEquals(defaultLibrarySettings, librarySettingsManager.librarySettings)
    }

    @Test
    fun librarySettings_Should_Load_Initial_From_Asset_If_Available_And_Not_Using_Remote_Settings() {
        config.useRemoteLibrarySettings = false
        mockStorage(cache = null, asset = null)

        createLibrarySettingsManager()

        verify {
            mockLoader.loadFromAsset(any())
        }
    }

    @Test
    fun librarySettings_Should_Load_Initial_From_Cache_If_Available_And_Using_Remote_Settings() =
        runBlocking {
            config.useRemoteLibrarySettings = true
            mockStorage(cache = null, asset = null)
            mockNullNetworkResponse()

            createLibrarySettingsManager()

            coVerify {
                mockLoader.loadFromFile(any())
            }
        }

    @Test
    fun librarySettings_Should_Load_Initial_From_Overridden_Default_When_No_Asset() {
        val defaultOverride = LibrarySettings(
            collectDispatcherEnabled = true,
            tagManagementDispatcherEnabled = true,
            batterySaver = true
        )
        config.useRemoteLibrarySettings = false
        config.overrideDefaultLibrarySettings = defaultOverride
        mockStorage(cache = null, asset = null)

        val librarySettingsManager = createLibrarySettingsManager()

        verify {
            mockLoader.loadFromAsset(any())
        }

        assertNotEquals(LibrarySettings(), librarySettingsManager.librarySettings)
        assertEquals(defaultOverride, librarySettingsManager.librarySettings)
    }

    @Test
    fun librarySettings_Should_Load_Initial_From_Overridden_Default_When_No_Cache() = runBlocking {
        val defaultOverride = LibrarySettings(
            collectDispatcherEnabled = true,
            tagManagementDispatcherEnabled = true,
            batterySaver = true
        )
        config.useRemoteLibrarySettings = true
        config.overrideDefaultLibrarySettings = defaultOverride
        mockStorage(cache = null, asset = null)
        mockNullNetworkResponse()

        val librarySettingsManager = createLibrarySettingsManager()

        coVerify {
            mockLoader.loadFromFile(any())
        }

        assertNotEquals(defaultLibrarySettings, librarySettingsManager.librarySettings)
        assertEquals(defaultOverride, librarySettingsManager.librarySettings)
    }

    @Test
    fun settings_From_Asset_Override_Defaults_When_Present() = runBlocking {
        val jsonLibrarySettings = LibrarySettings(
            collectDispatcherEnabled = false,
            tagManagementDispatcherEnabled = true,
            batching = Batching(maxQueueSize = 999),
            batterySaver = true
        ).toJsonString()
        config.useRemoteLibrarySettings = false
        mockStorage(cache = null, asset = jsonLibrarySettings)

        val librarySettingsManager = createLibrarySettingsManager()

        assertNotEquals(defaultLibrarySettings, librarySettingsManager.librarySettings)
        assertEquals(false, librarySettingsManager.librarySettings.collectDispatcherEnabled)
        assertEquals(true, librarySettingsManager.librarySettings.tagManagementDispatcherEnabled)
        assertEquals(999, librarySettingsManager.librarySettings.batching.maxQueueSize)
        assertEquals(true, librarySettingsManager.librarySettings.batterySaver)
    }

    @Test
    fun remote_Json_Settings_Override_Defaults_When_Present() = runBlocking {
        val jsonLibrarySettings = LibrarySettings(
            collectDispatcherEnabled = false,
            tagManagementDispatcherEnabled = true,
            batching = Batching(maxQueueSize = 999),
            batterySaver = true
        ).toJsonString()
        config.useRemoteLibrarySettings = true
        mockStorage(cache = jsonLibrarySettings, asset = null)
        mockNullNetworkResponse()

        val librarySettingsManager = createLibrarySettingsManager()

        assertNotEquals(defaultLibrarySettings, librarySettingsManager.librarySettings)
        assertEquals(false, librarySettingsManager.librarySettings.collectDispatcherEnabled)
        assertEquals(true, librarySettingsManager.librarySettings.tagManagementDispatcherEnabled)
        assertEquals(999, librarySettingsManager.librarySettings.batching.maxQueueSize)
        assertEquals(true, librarySettingsManager.librarySettings.batterySaver)
    }

    @Test
    fun remote_Json_Settings_Override_Cache_When_Present() = runBlocking {
        val jsonLibrarySettings = LibrarySettings(
            collectDispatcherEnabled = false,
            tagManagementDispatcherEnabled = true,
            batching = Batching(maxQueueSize = 999),
            batterySaver = true
        ).toJsonString()
        config.useRemoteLibrarySettings = true
        config.overrideLibrarySettingsUrl = "tealium-settings.json"
        mockStorage(cache = defaultJsonLibrarySettings, asset = null)
        mockNetworkResponse(response = jsonLibrarySettings, status = ResponseStatus.Success)

        val librarySettingsManager = createLibrarySettingsManager()
        delay(1000)

        assertEquals(false, librarySettingsManager.librarySettings.collectDispatcherEnabled)
        assertEquals(true, librarySettingsManager.librarySettings.tagManagementDispatcherEnabled)
        assertEquals(999, librarySettingsManager.librarySettings.batching.maxQueueSize)
        assertEquals(true, librarySettingsManager.librarySettings.batterySaver)
    }

    @Test
    fun remote_Json_Settings_Publish_Updated_Setting_When_Changed() = runBlocking {
        val updatedLibrarySettings = LibrarySettings(
            collectDispatcherEnabled = false,
            tagManagementDispatcherEnabled = true,
            batching = Batching(maxQueueSize = 999),
            batterySaver = true
        )
        val jsonLibrarySettings = updatedLibrarySettings.toJsonString()
        config.useRemoteLibrarySettings = true
        config.overrideLibrarySettingsUrl = "tealium-settings.json"
        mockStorage(cache = defaultJsonLibrarySettings, asset = null)
        mockNetworkResponse(response = jsonLibrarySettings, status = ResponseStatus.Success)

        val initialSettings = createLibrarySettingsManager().librarySettings

        verify(timeout = 1000, ordering = Ordering.ORDERED) {
            mockEventRouter.onLibrarySettingsUpdated(initialSettings)
            mockEventRouter.onLibrarySettingsUpdated(updatedLibrarySettings)
        }
    }

    @Test
    fun remote_Json_Settings_Does_Not_Crash_When_Malformed_And_Returns_Default() = runBlocking {
        config.useRemoteLibrarySettings = true
        config.overrideLibrarySettingsUrl = "tealium-settings.json"
        mockStorage(cache = null, asset = null)
        mockNetworkResponse(response = malformedJson, status = ResponseStatus.Success)

        val librarySettingsManager = createLibrarySettingsManager()

        assertEquals(defaultLibrarySettings, librarySettingsManager.librarySettings)
    }

    @Test
    fun cached_Settings_Does_Not_Crash_When_Malformed_An_dReturns_Default() = runBlocking {
        config.useRemoteLibrarySettings = true
        config.overrideLibrarySettingsUrl = "tealium-settings.json"
        mockStorage(cache = malformedJson, asset = null)
        mockNetworkResponse(response = malformedJson, status = ResponseStatus.Success)

        val librarySettingsManager = createLibrarySettingsManager()

        assertEquals(defaultLibrarySettings, librarySettingsManager.librarySettings)
    }

    @Test
    fun remote_Html_Settings_Override_Cache_When_Present() = runBlocking {
        val htmlLibrarySettings = """<!DOCTYPE html>
                <!--tealium tag management - mobile.webview ut4.0.202006041357, Copyright 2020 Tealium.com Inc. All Rights Reserved.-->
                <html>
                <head><title>Tealium Mobile Webview</title></head>
                <body>
                <script type="text/javascript">var utag_cfg_ovrd={noview:true};var mps = {"4":{"_is_enabled":"true","battery_saver":"true","dispatch_expiration":"-1","event_batch_size":"1","ivar_tracking":"true","mobile_companion":"true","offline_dispatch_limit":"-1","ui_auto_tracking":"true","wifi_only_sending":"false"},"5":{"_is_enabled":"true","battery_saver":"true","dispatch_expiration":"-1","enable_collect":"false","enable_s2s_legacy":"false","enable_tag_management":"false","event_batch_size":"1","minutes_between_refresh":"15.0","offline_dispatch_limit":"100","override_log":"dev","wifi_only_sending":"false"},"_firstpublish":"true"}</script>
                <script type="text/javascript">
                    if (("" + location.search).indexOf("sdk_session_count=true") > -1) {
                      window.utag_cfg_ovrd = window.utag_cfg_ovrd || {};
                      window.utag_cfg_ovrd.no_session_count = true;
                    } 
                </script>
                <script type="text/javascript" src="//tags.tiqcdn.com/utag/tealium/test/dev/utag.js"></script>
                </body>
                </html>"""
        config.useRemoteLibrarySettings = true
        config.overrideLibrarySettingsUrl = "mobile.html"
        mockStorage(cache = defaultJsonLibrarySettings, asset = null)
        mockNetworkResponse(response = htmlLibrarySettings, status = ResponseStatus.Success)

        val librarySettingsManager = createLibrarySettingsManager()
        delay(1000)

        assertEquals(false, librarySettingsManager.librarySettings.collectDispatcherEnabled)
        assertEquals(false, librarySettingsManager.librarySettings.tagManagementDispatcherEnabled)
        assertEquals(100, librarySettingsManager.librarySettings.batching.maxQueueSize)
        assertEquals(true, librarySettingsManager.librarySettings.batterySaver)
    }

    @Test
    fun localSettingsDoesNotOverrideDefaultsOnMalformedJson() = runBlocking {
        config.useRemoteLibrarySettings = false
        mockStorage(cache = null, asset = malformedJson)
        val librarySettingsManager = createLibrarySettingsManager()

        assertEquals(defaultLibrarySettings, librarySettingsManager.librarySettings)

        coVerify(timeout = 500, exactly = 0) {
            mockEventRouter.onLibrarySettingsUpdated(match { it != defaultLibrarySettings })
        }
    }

    @Test
    fun fetchRemoteLibrarySettings_AdheresTo_RefreshInterval() = runBlocking {
        config.useRemoteLibrarySettings = true
        mockStorage(cache = null, asset = null)
        mockNetworkResponse(response = null, status = ResponseStatus.Non200Response(304))

        val librarySettingsManager = createLibrarySettingsManager()

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
        mockStorage(cache = null, asset = null)
        mockNetworkResponse(
            response = """{
              "refresh_interval": "25m"
            }""".trimIndent(),
            status = ResponseStatus.Success
        )
        val librarySettingsManager = createLibrarySettingsManager()

        delay(500)
        assertEquals(25, librarySettingsManager.resourceRetriever.refreshInterval)
    }

    @Test
    fun initialSettings_Returns_Null_When_NoCache_And_RemoteSetting_Enabled() {
        config.useRemoteLibrarySettings = true
        mockStorage(cache = null, asset = null)

        val librarySettingsManager = createLibrarySettingsManager()

        assertNull(librarySettingsManager.initialSettings)
    }

    @Test
    fun initialSettings_Returns_Null_When_NoAsset_And_RemoteSetting_Disabled() {
        config.useRemoteLibrarySettings = false
        mockStorage(cache = null, asset = null)

        val librarySettingsManager = createLibrarySettingsManager()

        assertNull(librarySettingsManager.initialSettings)
    }

    @Test
    fun initialSettings_Returns_CachedSettings_When_RemoteSetting_Enabled() {
        config.useRemoteLibrarySettings = true
        mockStorage(cache = LibrarySettings().toJsonString(), asset = null)

        val librarySettingsManager = createLibrarySettingsManager()

        assertEquals(defaultLibrarySettings, librarySettingsManager.initialSettings)
    }

    @Test
    fun initialSettings_Returns_AssetSettings_When_RemoteSetting_Disabled() {
        config.useRemoteLibrarySettings = false
        mockStorage(cache = null, asset = LibrarySettings().toJsonString())

        val librarySettingsManager = createLibrarySettingsManager()

        assertEquals(defaultLibrarySettings, librarySettingsManager.initialSettings)
    }

    @Test
    fun settings_Are_Published_To_Event_Router_On_Init() {
        mockStorage(cache = null, asset = null)
        createLibrarySettingsManager()

        verify(exactly = 1) {
            mockEventRouter.onLibrarySettingsUpdated(defaultLibrarySettings)
        }
    }

    /**
     * Use to set the initially stored settings data.
     */
    private fun mockStorage(cache: String?, asset: String?) {
        every { mockLoader.loadFromFile(any()) } returns cache
        every { mockLoader.loadFromAsset(any()) } returns asset
    }

    /**
     * Sets the response for any remote settings
     */
    private fun mockNetworkResponse(
        response: String?,
        etag: String? = null,
        status: ResponseStatus = ResponseStatus.Success
    ) {
        val entity = ResourceEntity(response, etag, status)
        coEvery { mockNetworkClient.getResourceEntity(any(), any()) } returns entity
    }

    /**
     * Returns a `null` from the network client
     */
    private fun mockNullNetworkResponse() {
        coEvery { mockNetworkClient.getResourceEntity(any(), any()) } returns null
    }

    private fun LibrarySettings.toJson(): JSONObject =
        LibrarySettings.toJson(this)

    private fun LibrarySettings.toJsonString(): String =
        this.toJson().toString()

    private fun createLibrarySettingsManager(
        config: TealiumConfig = this.config,
        networkClient: NetworkClient = this.mockNetworkClient,
        loader: Loader = this.mockLoader,
        eventRouter: EventRouter = this.mockEventRouter,
        backgroundScope: CoroutineScope = this.backgroundScope
    ): LibrarySettingsManager {
        return LibrarySettingsManager(
            config,
            networkClient,
            loader,
            eventRouter,
            backgroundScope
        )
    }
}