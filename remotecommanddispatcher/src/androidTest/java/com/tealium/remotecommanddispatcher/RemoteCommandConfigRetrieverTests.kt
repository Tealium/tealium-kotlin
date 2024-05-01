package com.tealium.remotecommanddispatcher

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.Environment
import com.tealium.core.Loader
import com.tealium.core.TealiumConfig
import com.tealium.core.network.CooldownHelper
import com.tealium.core.network.NetworkClient
import com.tealium.core.network.ResourceEntity
import com.tealium.core.network.ResourceRetriever
import com.tealium.core.network.ResponseStatus
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class RemoteCommandConfigRetrieverTests {
    @MockK
    lateinit var mockNetworkClient: NetworkClient

    @MockK
    lateinit var mockLoader: Loader

    lateinit var context: Application
    lateinit var config: TealiumConfig

    private val commandId = "test-command"
    private val testUrl = "http://test.url"

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        context = ApplicationProvider.getApplicationContext()

        // default to no backup asset
        every { mockLoader.loadFromAsset(any()) } returns null
        every { mockLoader.loadFromFile(any()) } returns null

        mockNetworkClient = mockk()
        coEvery { mockNetworkClient.getResourceEntity(any(), any()) } returns null

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

        assertNull(configRetriever.remoteCommandConfig)
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

        assertNull(configRetriever.remoteCommandConfig)
        verify {
            mockLoader.loadFromAsset(any())
        }
    }

    @Test
    fun remoteCommandConfig_LoadsValidConfig_FromCache() = runBlocking {
        every { mockLoader.loadFromFile(any()) } returns validCommandsJson

        val configRetriever = createUrlRemoteCommandConfigRetriever(coroutineScope = this)

        assertValidRemoteCommandsConfig(configRetriever.remoteCommandConfig)
        verify {
            mockLoader.loadFromFile(any())
        }
    }

    @Test
    fun remoteCommandConfig_DoesNotThrow_WhenInvalidJson_LoadFromCache() = runBlocking {
        every { mockLoader.loadFromFile(any()) } returns ""

        val configRetriever = createUrlRemoteCommandConfigRetriever(coroutineScope = this)

        assertNull(configRetriever.remoteCommandConfig)
        verify {
            mockLoader.loadFromFile(any())
        }
    }

    @Test
    fun remoteCommandConfig_DoesNotThrow_WhenNullJson_LoadFromCache() = runBlocking {
        every { mockLoader.loadFromFile(any()) } returns null

        val configRetriever = createUrlRemoteCommandConfigRetriever(coroutineScope = this)

        assertNull(configRetriever.remoteCommandConfig)
        verify {
            mockLoader.loadFromFile(any())
        }
    }

    @Test
    fun remoteCommandConfig_LoadsValidConfig_FromUrl() = runBlocking {
        coEvery { mockNetworkClient.getResourceEntity(testUrl, any()) } returns ResourceEntity(
            validCommandsJson,
            status = ResponseStatus.Success
        )

        val configRetriever = createUrlRemoteCommandConfigRetriever(coroutineScope = this)

        // cache is null
        assertNull(configRetriever.remoteCommandConfig)
        verify {
            mockLoader.loadFromFile(any())
        }
        coVerify(timeout = 500) {
            mockNetworkClient.getResourceEntity(testUrl, any())
        }

        delay(500)
        assertValidRemoteCommandsConfig(configRetriever.remoteCommandConfig)
    }

    @Test
    fun remoteCommandConfig_DoesNotThrow_WhenInvalidJson_LoadFromUrl() = runBlocking {
        coEvery { mockNetworkClient.getResourceEntity(testUrl, any()) } returns ResourceEntity(
            "",
            null
        )
        val configRetriever = createUrlRemoteCommandConfigRetriever(coroutineScope = this)

        assertNull(configRetriever.remoteCommandConfig)
        verify {
            mockLoader.loadFromFile(any())
        }
        coVerify(timeout = 500) {
            mockNetworkClient.getResourceEntity(testUrl, any())
        }

        delay(500)
        assertNull(configRetriever.remoteCommandConfig)
    }

    @Test
    fun remoteCommandConfig_DoesNotThrow_WhenNullJson_LoadFromUrl() = runBlocking {
        val configRetriever = createUrlRemoteCommandConfigRetriever(coroutineScope = this)

        assertNull(configRetriever.remoteCommandConfig)
        verify {
            mockLoader.loadFromFile(any())
        }
        coVerify(timeout = 500) {
            mockNetworkClient.getResourceEntity(testUrl, any())
        }

        delay(500)
        assertNull(configRetriever.remoteCommandConfig)
    }

    @Test
    fun remoteCommandConfig_LoadsAsset_IfNoCacheAvailable() = runBlocking {
        val fileName = "test-asset.json"
        every { mockLoader.loadFromAsset(fileName) } returns validCommandsJson

        val configRetriever =
            createUrlRemoteCommandConfigRetriever(coroutineScope = this, localFileName = fileName)

        assertValidRemoteCommandsConfig(configRetriever.remoteCommandConfig)
        verify {
            mockLoader.loadFromAsset(fileName)
        }
    }

    @Test
    fun remoteCommandConfig_LoadsDefaultAsset_IfNoCacheAvailable_OrFileNameSupplied() =
        runBlocking {
            every { mockLoader.loadFromAsset("$commandId.json") } returns validCommandsJson

            val configRetriever = createUrlRemoteCommandConfigRetriever(coroutineScope = this)

            assertValidRemoteCommandsConfig(configRetriever.remoteCommandConfig)
            verify {
                mockLoader.loadFromAsset("$commandId.json")
            }
        }

    @Test
    fun remoteCommandConfig_SavesEtag_WhenPresent() =
        runBlocking {
            coEvery { mockNetworkClient.getResourceEntity(testUrl, any()) } returns ResourceEntity(
                validCommandsJson,
                "some-etag",
                ResponseStatus.Success
            )
            mockkStatic("kotlin.io.FilesKt__FileReadWriteKt")
            val mockFile =
                spyk<File>(UrlRemoteCommandConfigRetriever.getCacheFile(config, commandId))
            every { mockFile.writeBytes(any()) } just Runs
            every { mockFile.writeText(any(), any()) } just Runs

            val configRetriever =
                createUrlRemoteCommandConfigRetriever(coroutineScope = this, cacheFile = mockFile)

            delay(100)
            verify(timeout = 1000) {
                mockFile.writeText(match { json ->
                    JSONObject(json).get(Settings.ETAG) == "some-etag"
                }, Charsets.UTF_8)
            }
            delay(500)
            val config = configRetriever.remoteCommandConfig
            assertValidRemoteCommandsConfig(config)
            assertEquals("some-etag", config!!.etag)
        }

    @Test
    fun remoteCommandConfig_UsesCurrentEtag_WhenPresentInCache() =
        runBlocking {
            every { mockLoader.loadFromFile(any()) } returns """
                { "${Settings.ETAG}" : "some-etag" }
            """.trimIndent()

            val configRetriever = createUrlRemoteCommandConfigRetriever(coroutineScope = this)

            coVerify(timeout = 1000) {
                mockNetworkClient.getResourceEntity(any(), "some-etag")
            }
        }

    @Test
    fun remoteCommandConfig_DoesNotRefresh_When_InCooldown_And_AlreadyFetched() =
        runBlocking {
            val mockCooldown = mockk<CooldownHelper>(relaxed = true)
            every { mockCooldown.isInCooldown(any()) } returns true
            val mockResourceRetriever = mockk<ResourceRetriever>()
            every { mockResourceRetriever.lastFetchTimestamp } returns 0L
            coEvery { mockResourceRetriever.fetchWithEtag(any()) } returns null

            val configRetriever = createUrlRemoteCommandConfigRetriever(
                coroutineScope = this,
                resourceRetriever = mockResourceRetriever,
                cooldownHelper = mockCooldown
            )
            launch {
                configRetriever.refreshConfig()
            }
            delay(100)
            coVerify(timeout = 1000, inverse = true) {
                mockResourceRetriever.fetchWithEtag(any())
            }
        }

    @Test
    fun remoteCommandConfig_Refreshes_When_Not_AlreadyFetched() =
        runBlocking {
            val mockCooldown = mockk<CooldownHelper>(relaxed = true)
            every { mockCooldown.isInCooldown(any()) } returns true
            val mockResourceRetriever = mockk<ResourceRetriever>()
            every { mockResourceRetriever.lastFetchTimestamp } returns null
            coEvery { mockResourceRetriever.fetchWithEtag(any()) } returns null

            val configRetriever = createUrlRemoteCommandConfigRetriever(
                coroutineScope = this,
                resourceRetriever = mockResourceRetriever,
                cooldownHelper = mockCooldown
            )
            launch {
                configRetriever.refreshConfig()
            }
            delay(100)
            coVerify(timeout = 1000, exactly = 1) {
                mockResourceRetriever.fetchWithEtag(any())
            }
        }

    @Test
    fun remoteCommandConfig_DoesNotRefresh_When_IsInCooldown_FromInitialError() =
        runBlocking {
            val mockResourceRetriever = mockk<ResourceRetriever>()
            every { mockResourceRetriever.lastFetchTimestamp } returns null
            coEvery { mockResourceRetriever.fetchWithEtag(any()) } returns ResourceEntity(
                status = ResponseStatus.Non200Response(
                    400
                )
            )

            val configRetriever = createUrlRemoteCommandConfigRetriever(
                coroutineScope = this,
                resourceRetriever = mockResourceRetriever,
                cooldownHelper = CooldownHelper(15_000L, 5_000L)
            )
            launch {
                configRetriever.refreshConfig()
                configRetriever.refreshConfig()
            }
            delay(100)
            coVerify(timeout = 1000, exactly = 1) {
                mockResourceRetriever.fetchWithEtag(any())
            }
        }

    @Test
    fun assetLoader_Appends_Json_Filetype_If_Missing() {
        AssetRemoteCommandConfigRetriever.loadFromAsset(mockLoader, "test.json")
        AssetRemoteCommandConfigRetriever.loadFromAsset(mockLoader, "test")

        verify(exactly = 2) {
            mockLoader.loadFromAsset("test.json")
        }
    }

    @Test
    fun assetLoader_Appends_Json_Filetype_First_If_Missing() {
        AssetRemoteCommandConfigRetriever.loadFromAsset(mockLoader, "test")

        verifyOrder {
            mockLoader.loadFromAsset("test.json")
            mockLoader.loadFromAsset("test")
        }
    }

    private fun createUrlRemoteCommandConfigRetriever(
        config: TealiumConfig = this.config,
        commandId: String = this.commandId,
        remoteUrl: String = this.testUrl,
        networkClient: NetworkClient = this.mockNetworkClient,
        loader: Loader = this.mockLoader,
        resourceRetriever: ResourceRetriever? = null,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        localFileName: String? = null,
        cacheFile: File = UrlRemoteCommandConfigRetriever.getCacheFile(config, commandId),
        cooldownHelper: CooldownHelper = CooldownHelper(0, 0) // no cooldown
    ): UrlRemoteCommandConfigRetriever {
        val retriever = resourceRetriever ?: ResourceRetriever(config, remoteUrl, networkClient)

        return UrlRemoteCommandConfigRetriever(
            config = config,
            commandId = commandId,
            remoteUrl = remoteUrl,
            client = networkClient,
            loader = loader,
            resourceRetriever = retriever,
            backgroundScope = coroutineScope,
            assetFileName = localFileName,
            cacheFile = cacheFile,
            cooldownHelper = cooldownHelper
        )
    }

    private fun assertValidRemoteCommandsConfig(config: RemoteCommandConfig?) {
        if (config == null) {
            fail()
            return
        }

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