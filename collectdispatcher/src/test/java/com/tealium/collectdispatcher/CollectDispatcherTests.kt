package com.tealium.collectdispatcher

import android.app.Application
import com.tealium.core.*
import com.tealium.core.messaging.AfterDispatchSendCallbacks
import com.tealium.core.network.HttpClient
import com.tealium.core.network.NetworkClient
import com.tealium.dispatcher.BatchDispatch
import com.tealium.dispatcher.TealiumEvent
import io.mockk.MockKAnnotations
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class CollectDispatcherTests {

    @MockK
    lateinit var mockApplication: Application

    @MockK
    lateinit var mockContext: TealiumContext

    @MockK
    lateinit var mockConfig: TealiumConfig

    @MockK
    lateinit var mockEncoder: Encoder

    @MockK
    lateinit var mockNetworkClient: NetworkClient

    @MockK
    lateinit var mockAfterDispatchSendCallbacks: AfterDispatchSendCallbacks

    @MockK
    lateinit var mockFile: File

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { mockNetworkClient.networkClientListener = any() } just Runs
        every { mockConfig.accountName } returns "test-account"
        every { mockConfig.profileName } returns "test-profile"
        every { mockConfig.environment } returns Environment.DEV
        every { mockConfig.dataSourceId } returns "test-datasource"
        every { mockConfig.application } returns mockApplication
        every { mockApplication.filesDir } returns mockFile
        every { mockContext.config } returns mockConfig

        // no overrides byt default
        every { mockConfig.overrideCollectDomain } returns null
        every { mockConfig.overrideCollectUrl } returns null
        every { mockConfig.overrideCollectBatchUrl } returns null
    }

    @Test
    fun factory_DispatchersReferencesCompanion() {
        assertSame(CollectDispatcher.Companion, Dispatchers.Collect)
        assertTrue(Dispatchers.Collect is DispatcherFactory)
    }

    @Test
    fun factory_createsNewInstances() {
        val collect1 = CollectDispatcher.create(mockContext, mockAfterDispatchSendCallbacks)
        val collect2 = CollectDispatcher.create(mockContext, mockAfterDispatchSendCallbacks)

        assertNotNull(collect1)
        assertNotNull(collect2)
        assertNotSame(collect1, collect2)
    }

    @Test
    fun config_OverridesAreSetCorrectly() {
        val config = TealiumConfig(mockApplication,
                mockConfig.accountName,
                mockConfig.profileName,
                mockConfig.environment,
                dataSourceId = mockConfig.dataSourceId)

        config.overrideCollectDomain = null
        assertNull(config.overrideCollectDomain)
        config.overrideCollectDomain = "my.override"
        assertEquals("my.override", config.overrideCollectDomain)

        config.overrideCollectUrl = null
        assertNull(config.overrideCollectUrl)
        config.overrideCollectUrl = "my.override"
        assertEquals("my.override", config.overrideCollectUrl)

        config.overrideCollectBatchUrl = null
        assertNull(config.overrideCollectBatchUrl)
        config.overrideCollectBatchUrl = "my.override"
        assertEquals("my.override", config.overrideCollectBatchUrl)
    }

    @Test
    fun urls_DefaultsAreUsedWhenNotOverridden() {
        // no overrides set in [setUp]
        val collectDispatcher = CollectDispatcher(mockConfig, mockEncoder, mockNetworkClient)

        assertEquals(CollectDispatcher.COLLECT_URL, collectDispatcher.eventUrl)
        assertEquals(CollectDispatcher.BULK_URL, collectDispatcher.batchEventUrl)
    }

    @Test
    fun urls_DefaultDomainGetsOverridden() {
        every { mockConfig.overrideCollectDomain } returns "cname.domain.com"
        every { mockConfig.overrideCollectUrl } returns null
        every { mockConfig.overrideCollectBatchUrl } returns null

        val collectDispatcher = CollectDispatcher(mockConfig, mockEncoder, mockNetworkClient)

        assertEquals("https://cname.domain.com/event", collectDispatcher.eventUrl)
        assertEquals("https://cname.domain.com/bulk-event", collectDispatcher.batchEventUrl)
    }

    @Test
    fun urls_DefaultUrlsAreOverridden() {
        every { mockConfig.overrideCollectDomain } returns "cname.domain.com"
        // URL Overrides should take precedence
        every { mockConfig.overrideCollectUrl } returns "https://my.website.com/my-endpoint"
        every { mockConfig.overrideCollectBatchUrl } returns "https://my.website.com/my-bulk-endpoint"

        val collectDispatcher = CollectDispatcher(mockConfig, mockEncoder, mockNetworkClient)

        assertEquals("https://my.website.com/my-endpoint", collectDispatcher.eventUrl)
        assertEquals("https://my.website.com/my-bulk-endpoint", collectDispatcher.batchEventUrl)
    }

    @Test
    fun events_IndividualEventsAreEncodedCorrectly() = runBlocking {
        coEvery { mockNetworkClient.post(any(), any(), any()) } just Runs

        val collectDispatcher = CollectDispatcher(mockConfig, client = mockNetworkClient)
        val event = TealiumEvent("my-event", mapOf("key" to "value"))

        collectDispatcher.onDispatchSend(event)

        coVerify {
            mockNetworkClient.post(
                    "tealium_event_type=event&tealium_event=my-event&key=value&tealium_account=test-account&tealium_profile=test-profile",
                    CollectDispatcher.COLLECT_URL,
                    false
            )
        }
    }

    @Test
    fun events_BatchEventsAreEncodedCorrectly() = runBlocking {
        coEvery { mockNetworkClient.post(any(), any(), any()) } just Runs

        val collectDispatcher = CollectDispatcher(mockConfig, client = mockNetworkClient)
        val event = TealiumEvent("my-event", mapOf("key" to "value"))

        collectDispatcher.onBatchDispatchSend(listOf(event, event))

        coVerify {
            mockNetworkClient.post(
                    JSONObject(BatchDispatch.create(listOf(event, event))!!.payload()).toString(),
                    CollectDispatcher.BULK_URL,
                    true
            )
        }
    }

    @Test
    fun listener_ReceivesHttpResponses() {
        val listener = mockk<CollectDispatcherListener>()
        every { listener.successfulTrack() } just Runs
        every { listener.unsuccessfulTrack(any()) } just Runs

        val networkClient = spyk(HttpClient(mockConfig))
        val collectDispatcher = CollectDispatcher(mockConfig,
                client = networkClient,
                collectDispatchListener = listener)

        networkClient.networkClientListener?.onNetworkResponse(200,"")
        networkClient.networkClientListener?.onNetworkResponse(404,"Not Found")
        networkClient.networkClientListener?.onNetworkError("My Error")

        verify {
            listener.successfulTrack()
            listener.unsuccessfulTrack("Network error, response: Not Found")
        }
    }
}