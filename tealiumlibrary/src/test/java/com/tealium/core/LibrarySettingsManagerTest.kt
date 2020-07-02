//package com.tealium.core
//
//import android.app.Application
//import android.util.Log
//import com.tealium.core.model.Environment
//import com.tealium.core.model.LibrarySettings
//import com.tealium.core.model.TealiumConfig
//import com.tealium.core.network.Connectivity
//import io.mockk.*
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.runBlocking
//import org.json.JSONObject
//import org.junit.Assert.*
//import org.junit.Before
//import org.junit.Test
//import org.junit.runner.RunWith
//import org.robolectric.RobolectricTestRunner
//import java.io.File
//
//
//@RunWith(RobolectricTestRunner::class)
//class LibrarySettingsManagerTest {
//
//    lateinit var mockLoader: Loader
//    lateinit var mockConnectivity: Connectivity
//    lateinit var mockFile : File
//    lateinit var context: Application
//    lateinit var config: TealiumConfig
//    lateinit var librarySettingsManager: LibrarySettingsManager
//
//    val defaultJsonLibrarySettings = "{\n" +
//            "  \"collect_dispatcher\": false,\n" +
//            "  \"tag_management_dispatcher\": false,\n" +
//            "  \"batching\": {\n" +
//            "    \"batch_size\": 10,\n" +
//            "    \"interval\": \"10m\",\n" +
//            "    \"max_queue_size\": 100,\n" +
//            "    \"expiration\": \"1d\"\n" +
//            "  },\n" +
//            "  \"battery_saver\": false,\n" +
//            "  \"wifi_only\": false,\n" +
//            "  \"refresh_interval\": \"15m\",\n" +
//            "  \"log_level\": \"dev\",\n" +
//            "  \"disable_library\": false\n" +
//            "}"
//
//    @Before
//    fun setUp() {
//        context = mockk<Application>()
//        mockLoader = mockk<Loader>()
//        mockFile = mockk<File>()
//        mockkStatic(Log::class)
//        every { Log.v(any(), any()) } returns 0
//        mockConnectivity = mockk<Connectivity>()
//        mockkConstructor(TealiumConfig::class)
//        every { context.filesDir } returns mockFile
//        every { anyConstructed<TealiumConfig>().tealiumDirectory.mkdir() } returns mockk()
//        config = TealiumConfig(context, "test", "profile", Environment.QA)
//        librarySettingsManager = LibrarySettingsManager(config, mockConnectivity, mockLoader)
//    }
//
//    @Test
//    fun librarySettingsNotNull() {
//        assertNotNull(librarySettingsManager.librarySettings)
//    }
//
//    @Test
//    fun librarySettingsDefaultsMatchDefaultLibrarySettings() {
//        val default = LibrarySettings()
//        assertEquals(default, librarySettingsManager.librarySettings)
//    }
//
//    @Test
//    fun localSettingsJsonOverridesDefaultsOnSuccess() {
//        val jsonLibrarySettings = "{\n" +
//                "  \"collect_dispatcher\": false,\n" +
//                "  \"tag_management_dispatcher\": true,\n" +
//                "  \"batching\": {\n" +
//                "    \"batch_size\": 10,\n" +
//                "    \"interval\": \"10m\",\n" +
//                "    \"max_queue_size\": 999,\n" +
//                "    \"expiration\": \"1d\"\n" +
//                "  },\n" +
//                "  \"battery_saver\": true,\n" +
//                "  \"wifi_only\": false,\n" +
//                "  \"refresh_interval\": \"15m\",\n" +
//                "  \"log_level\": \"dev\",\n" +
//                "  \"disable_library\": false\n" +
//                "}"
//        mockkConstructor(TealiumConfig::class)
//        val config = TealiumConfig(context, "test", "profile", Environment.QA)
//        config.useRemoteLibrarySettings = false
//        val librarySettingsManager = LibrarySettingsManager(config, mockConnectivity, mockLoader)
//        val default = librarySettingsManager.librarySettings
//        every { mockLoader.loadFromAsset(any()) } returns jsonLibrarySettings
//        every { mockConnectivity.isConnected() } returns true
//
//        runBlocking {
//            librarySettingsManager.fetchLibrarySettings()
//
//            assertNotEquals(default, librarySettingsManager.librarySettings)
//            assertEquals(false, librarySettingsManager.librarySettings.collectDispatcherEnabled)
//            assertEquals(true, librarySettingsManager.librarySettings.tagManagementDispatcherEnabled)
//            assertEquals(999, librarySettingsManager.librarySettings.batching?.maxQueueSize)
//            assertEquals(true, librarySettingsManager.librarySettings.batterySaver)
//        }
//    }
//
//    @Test
//    fun localSettingsDoesNotOverrideDefaultsOnMalformedJson() {
//        val malformedJson = "{\n" +
//                "  \"collect dispatcher\": true,\n" +
//                "  \"tag management_dispatcher\": true,\n" +
//                "  \"batching\": {\n" +
//                "    \"batch_size\": 10,\n" +
//                "    \"interval\": \"10m\",\n" +
//                "    \"max_queue_size\": 100,\n" +
//                "    \"expiration\": \"1d\"\n" +
//                "  },\n" +
//                "  \"battery_saver\": false,\n" +
//                "  \"wifi_only\": false,\n" +
//                "  \"refresh_interval\": \"15m\",\n" +
//                "  \"log_level\": \"dev\",\n" +
//                "  \"disable_library\": false\n" +
//                "}"
//        val config = TealiumConfig(context, "test", "profile", Environment.QA)
//        config.useRemoteLibrarySettings = false
//        val librarySettingsManager = LibrarySettingsManager(config, mockConnectivity, mockLoader)
//        val default = librarySettingsManager.librarySettings
//        every { mockLoader.loadFromAsset(any()) } returns malformedJson
//        every { mockConnectivity.isConnected() } returns true
//
//        runBlocking {
//            librarySettingsManager.fetchLibrarySettings()
//
//            assertEquals(default, librarySettingsManager.librarySettings)
//        }
//    }
//
//    @Test
//    fun remoteSettingsOverrideDefaultsOnSuccess() {
//        val jsonLibrarySettings = "{\n" +
//                "  \"collect_dispatcher\": false,\n" +
//                "  \"tag_management_dispatcher\": true,\n" +
//                "  \"batching\": {\n" +
//                "    \"batch_size\": 10,\n" +
//                "    \"interval\": \"10m\",\n" +
//                "    \"max_queue_size\": 998,\n" +
//                "    \"expiration\": \"1d\"\n" +
//                "  },\n" +
//                "  \"battery_saver\": true,\n" +
//                "  \"wifi_only\": false,\n" +
//                "  \"refresh_interval\": \"15m\",\n" +
//                "  \"log_level\": \"dev\",\n" +
//                "  \"disable_library\": false\n" +
//                "}"
//        val default = librarySettingsManager.librarySettings
//        every { mockLoader.loadFromAsset(any()) } returns defaultJsonLibrarySettings
//        every { mockLoader.loadFromUrl(any()) } returns JSONObject(jsonLibrarySettings)
//        every { mockConnectivity.isConnected() } returns true
//
//        runBlocking {
//            librarySettingsManager.fetchLibrarySettings()
//            delay(100)  // todo: might be a better way to test this
//
//            assertNotEquals(default, librarySettingsManager.librarySettings)
//            assertEquals(false, librarySettingsManager.librarySettings.collectDispatcherEnabled)
//            assertEquals(true, librarySettingsManager.librarySettings.tagManagementDispatcherEnabled)
//            assertEquals(998, librarySettingsManager.librarySettings.batching?.maxQueueSize)
//            assertEquals(true, librarySettingsManager.librarySettings.batterySaver)
//        }
//    }
//
//    @Test
//    fun remoteSettingsNotFetchedIfNoConnectivity() {
//        val jsonLibrarySettings = "{\n" +
//                "  \"collect_dispatcher\": false,\n" +
//                "  \"tag_management_dispatcher\": true,\n" +
//                "  \"batching\": {\n" +
//                "    \"batch_size\": 10,\n" +
//                "    \"interval\": \"10m\",\n" +
//                "    \"max_queue_size\": 998,\n" +
//                "    \"expiration\": \"1d\"\n" +
//                "  },\n" +
//                "  \"battery_saver\": true,\n" +
//                "  \"wifi_only\": false,\n" +
//                "  \"refresh_interval\": \"15m\",\n" +
//                "  \"log_level\": \"dev\",\n" +
//                "  \"disable_library\": false\n" +
//                "}"
//        val default = librarySettingsManager.librarySettings
//        every { mockLoader.loadFromAsset(any()) } returns defaultJsonLibrarySettings
//        every { mockLoader.loadFromUrl(any()) } returns JSONObject(jsonLibrarySettings)
//        every { mockConnectivity.isConnected() } returns false
//
//        runBlocking {
//            librarySettingsManager.fetchLibrarySettings()
//
//            assertEquals(default, librarySettingsManager.librarySettings)
//        }
//    }
//
//    // todo: put in when using tiq
//    //    @Test
////    fun defaultUrlIsCorrect() {
////        val expected = "https://tags.tiqcdn.com/utag/test/profile/qa/mobile.html?"
////        assertTrue("Url for library settings manager does not match", expected == librarySettingsManager.url)
////
////    }
//}