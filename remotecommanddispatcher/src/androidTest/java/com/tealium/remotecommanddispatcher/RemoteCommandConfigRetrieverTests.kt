package com.tealium.remotecommanddispatcher

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.Environment
import com.tealium.core.Loader
import com.tealium.core.TealiumConfig
import com.tealium.core.network.NetworkClient
import com.tealium.core.network.ResourceRetriever
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RemoteCommandConfigRetrieverTests {
    @MockK
    lateinit var mockNetworkClient: NetworkClient

    @MockK
    lateinit var mockLoader: Loader

    lateinit var context: Application
    lateinit var config: TealiumConfig

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        context = ApplicationProvider.getApplicationContext()

        mockNetworkClient = mockk()

        config = TealiumConfig(
            context, "test", "profile", Environment.DEV,
            collectors = mutableSetOf()
        )
    }

    @Test
    fun remoteCommandConfig_LoadsValidConfig_FromAsset() {
        every { mockLoader.loadFromAsset(any()) } returns validCommandsJson

        val configRetriever = AssetRemoteCommandConfigRetriever(
            config,
            "testCommand",
            loader = mockLoader,
        )

        assertValidRemoteCommandsConfig(configRetriever.remoteCommandConfig)
        verify {
            mockLoader.loadFromAsset(any())
        }
    }

    @Test
    fun remoteCommandConfig_DoesNotThrow_WhenNullJson_FromAsset() {
        every { mockLoader.loadFromAsset(any()) } returns null

        val configRetriever = AssetRemoteCommandConfigRetriever(
            config,
            "testCommand",
            loader = mockLoader,
        )

        assertDefaultRemoteCommandsConfig(configRetriever.remoteCommandConfig)
        verify {
            mockLoader.loadFromAsset(any())
        }
    }

    @Test
    fun remoteCommandConfig_DoesNotThrow_WhenInvalidJson_FromAsset() {
        every { mockLoader.loadFromAsset(any()) } returns ""

        val configRetriever = AssetRemoteCommandConfigRetriever(
            config,
            "testCommandId",
            loader = mockLoader,
        )

        assertDefaultRemoteCommandsConfig(configRetriever.remoteCommandConfig)
        verify {
            mockLoader.loadFromAsset(any())
        }
    }

    @Test
    fun remoteCommandConfig_LoadsAssetWithJsonTypeExtension_WhenExtensionIsMissing_FromAsset() {
        every { mockLoader.loadFromAsset("testCommandId.json") } returns validCommandsJson
        every { mockLoader.loadFromAsset("testCommandId") } returns null

        val configRetriever = AssetRemoteCommandConfigRetriever(
            config,
            "testCommandId",
            loader = mockLoader,
        )

        assertValidRemoteCommandsConfig(configRetriever.remoteCommandConfig)
        verify {
            mockLoader.loadFromAsset("testCommandId.json")
        }
        verify(inverse = true) {
            mockLoader.loadFromAsset("testCommandId")
        }
    }

    @Test
    fun remoteCommandConfig_LoadsAssetWithoutTypeExtension_WhenExtensionFailed_FromAsset() {
        every { mockLoader.loadFromAsset("testCommandId.json") } returns null
        every { mockLoader.loadFromAsset("testCommandId") } returns validCommandsJson

        val configRetriever = AssetRemoteCommandConfigRetriever(
            config,
            "testCommandId",
            loader = mockLoader,
        )

        assertValidRemoteCommandsConfig(configRetriever.remoteCommandConfig)
        verify(ordering = Ordering.ORDERED) {
            mockLoader.loadFromAsset("testCommandId.json")
            mockLoader.loadFromAsset("testCommandId")
        }
    }

    @Test
    fun remoteCommandConfig_LoadsAssetWithTypeExtension_WhenExtensionIsSpecified_FromAsset() {
        every { mockLoader.loadFromAsset("testCommandId.json") } returns validCommandsJson
        every { mockLoader.loadFromAsset("testCommandId") } returns null

        val configRetriever = AssetRemoteCommandConfigRetriever(
            config,
            "testCommandId.json",
            loader = mockLoader,
        )

        assertValidRemoteCommandsConfig(configRetriever.remoteCommandConfig)
        verify {
            mockLoader.loadFromAsset("testCommandId.json")
        }
        verify(inverse = true) {
            mockLoader.loadFromAsset("testCommandId")
        }
    }

    @Test
    fun remoteCommandConfig_LoadsValidConfig_FromCache() = runBlocking {
        every { mockLoader.loadFromFile(any()) } returns validCommandsJson
        coEvery { mockNetworkClient.get(any()) } returns null

        val configRetriever = UrlRemoteCommandConfigRetriever(
            config,
            "testCommandId",
            remoteUrl = "testRemoteUrl",
            client = mockNetworkClient,
            loader = mockLoader,
            backgroundScope = this
        )

        assertValidRemoteCommandsConfig(configRetriever.remoteCommandConfig)
        verify {
            mockLoader.loadFromFile(any())
        }
    }

    @Test
    fun remoteCommandConfig_DoesNotThrow_WhenInvalidJson_LoadFromCache() = runBlocking {
        every { mockLoader.loadFromFile(any()) } returns ""
        coEvery { mockNetworkClient.get(any()) } returns null

        val configRetriever = UrlRemoteCommandConfigRetriever(
            config,
            "testCommandId",
            remoteUrl = "testRemoteUrl",
            client = mockNetworkClient,
            loader = mockLoader,
            backgroundScope = this
        )

        assertDefaultRemoteCommandsConfig(configRetriever.remoteCommandConfig)
        verify {
            mockLoader.loadFromFile(any())
        }
    }

    @Test
    fun remoteCommandConfig_DoesNotThrow_WhenNullJson_LoadFromCache() = runBlocking {
        every { mockLoader.loadFromFile(any()) } returns null
        coEvery { mockNetworkClient.get(any()) } returns null

        val configRetriever = UrlRemoteCommandConfigRetriever(
            config,
            "testCommandId",
            remoteUrl = "testRemoteUrl",
            client = mockNetworkClient,
            loader = mockLoader,
            backgroundScope = this
        )

        assertDefaultRemoteCommandsConfig(configRetriever.remoteCommandConfig)
        verify {
            mockLoader.loadFromFile(any())
        }
    }

    @Test
    fun remoteCommandConfig_LoadsValidConfig_FromUrl() = runBlocking {
        val testUrl = "testRemoteUrl"
        every { mockLoader.loadFromFile(any()) } returns null
        coEvery { mockNetworkClient.get(testUrl) } returns validCommandsJson

        val mockResourceRetriever = ResourceRetriever(config, testUrl, mockNetworkClient)

        val configRetriever = UrlRemoteCommandConfigRetriever(
            config,
            "testCommandId",
            remoteUrl = testUrl,
            client = mockNetworkClient,
            loader = mockLoader,
            resourceRetriever = mockResourceRetriever,
            backgroundScope = CoroutineScope(Dispatchers.IO)
        )

        // cache is null
        assertDefaultRemoteCommandsConfig(configRetriever.remoteCommandConfig)
        verify {
            mockLoader.loadFromFile(any())
        }
        coVerify(timeout = 500) {
            mockNetworkClient.get(testUrl)
        }

        delay(500)
        assertValidRemoteCommandsConfig(configRetriever.remoteCommandConfig)
    }

    @Test
    fun remoteCommandConfig_DoesNotThrow_WhenInvalidJson_LoadFromUrl() = runBlocking {
        val testUrl = "testRemoteUrl"
        every { mockLoader.loadFromFile(any()) } returns null
        coEvery { mockNetworkClient.get(testUrl) } returns ""
        val mockResourceRetriever = ResourceRetriever(config, testUrl, mockNetworkClient)

        val configRetriever = UrlRemoteCommandConfigRetriever(
            config,
            "testCommandId",
            remoteUrl = testUrl,
            client = mockNetworkClient,
            loader = mockLoader,
            resourceRetriever = mockResourceRetriever,
            backgroundScope = CoroutineScope(Dispatchers.IO)
        )

        assertDefaultRemoteCommandsConfig(configRetriever.remoteCommandConfig)
        verify {
            mockLoader.loadFromFile(any())
        }
        coVerify(timeout = 500) {
            mockNetworkClient.get(testUrl)
        }

        delay(500)
        assertDefaultRemoteCommandsConfig(configRetriever.remoteCommandConfig)
    }

    @Test
    fun remoteCommandConfig_DoesNotThrow_WhenNullJson_LoadFromUrl() = runBlocking {
        val testUrl = "testRemoteUrl"
        every { mockLoader.loadFromFile(any()) } returns null
        coEvery { mockNetworkClient.get(testUrl) } returns null
        val mockResourceRetriever = ResourceRetriever(config, testUrl, mockNetworkClient)

        val configRetriever = UrlRemoteCommandConfigRetriever(
            config,
            "testCommandId",
            remoteUrl = testUrl,
            client = mockNetworkClient,
            loader = mockLoader,
            resourceRetriever = mockResourceRetriever,
            backgroundScope = CoroutineScope(Dispatchers.IO)
        )

        assertDefaultRemoteCommandsConfig(configRetriever.remoteCommandConfig)
        verify {
            mockLoader.loadFromFile(any())
        }
        coVerify(timeout = 500) {
            mockNetworkClient.get(testUrl)
        }

        delay(500)
        assertDefaultRemoteCommandsConfig(configRetriever.remoteCommandConfig)
    }

    private fun assertValidRemoteCommandsConfig(config: RemoteCommandConfig) {
        assertNotNull(config.apiConfig)
        assertNotNull(config.mappings)
        assertNotNull(config.apiCommands)

        val apiConfig = config.apiConfig!!
        assertEquals("true", apiConfig["is_something_enabled"])
        assertEquals("30", apiConfig["session_timeout"])
        assertEquals("max", apiConfig["log_level"])

        val mappings = config.mappings!!
        assertEquals("param_item_brand", mappings["product_brand"])
        assertEquals("param_item_category", mappings["product_category"])
        assertEquals("param_item_id", mappings["product_id"])
        assertEquals("param_item_name", mappings["product_name"])
        assertEquals("command_name", mappings["tealium_event"])

        val commands = config.apiCommands!!
        assertEquals("initialize", commands["launch"])
    }

    private fun assertDefaultRemoteCommandsConfig(config: RemoteCommandConfig) {
        assertNull(config.apiConfig)
        assertNull(config.mappings)
        assertNull(config.apiCommands)
    }

    private val validCommandsJson = """
        {
          "config": {
            "is_something_enabled": "true",
            "session_timeout": "30",
            "log_level": "max"
          },
          "mappings": {
            "product_brand": "param_item_brand",
            "product_category": "param_item_category",
            "product_id": "param_item_id",
            "product_name": "param_item_name",
            "tealium_event": "command_name"
          },
          "commands": {
            "launch": "initialize"
          }
        }
    """.trimIndent()
}