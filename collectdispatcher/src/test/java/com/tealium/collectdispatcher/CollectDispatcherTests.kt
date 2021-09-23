package com.tealium.collectdispatcher

import android.app.Application
import com.tealium.core.*
import com.tealium.core.consent.ConsentManagerConstants
import com.tealium.core.consent.consentManagerLoggingProfile
import com.tealium.core.consent.consentManagerLoggingUrl
import com.tealium.core.messaging.AfterDispatchSendCallbacks
import com.tealium.core.network.HttpClient
import com.tealium.core.network.NetworkClient
import com.tealium.dispatcher.BatchDispatch
import com.tealium.dispatcher.Dispatch
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
import org.robolectric.annotation.Config
import java.io.File


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21, 29])
class CollectDispatcherTests {

    @MockK
    lateinit var mockApplication: Application

    @MockK
    lateinit var mockContext: TealiumContext

    @MockK
    lateinit var mockNetworkClient: NetworkClient

    @MockK
    lateinit var mockAfterDispatchSendCallbacks: AfterDispatchSendCallbacks

    @MockK
    lateinit var mockFile: File

    lateinit var mockDispatch: Dispatch
    lateinit var config: TealiumConfig

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { mockNetworkClient.networkClientListener = any() } just Runs
//        every { config.accountName } returns "test-account"
//        every { config.profileName } returns "test-profile"
//        every { config.environment } returns Environment.DEV
//        every { config.dataSourceId } returns "test-datasource"
//        every { config.application } returns mockApplication
//        every { config.options } returns mutableMapOf()
        every { mockApplication.filesDir } returns mockFile

        config = TealiumConfig(mockApplication, "test", "profile12345", Environment.QA)
        every { mockContext.config } returns config

        // no overrides byt default
        config.overrideCollectDomain = null
        config.overrideCollectUrl = null
        config.overrideCollectBatchUrl = null
        config.overrideCollectProfile = null
        coEvery { mockNetworkClient.post(any(), any(), any()) } just Runs

        mockDispatch = spyk(TealiumEvent("my-event", mapOf(
                "tealium_event" to "my-event",
                "tealium_account" to "test-account" ,
                "tealium_profile" to "test-profile",
                "key" to "value"))
        )
        every { mockDispatch.id } returns "test_id"
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
                config.accountName,
                config.profileName,
                config.environment,
                dataSourceId = config.dataSourceId)

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

        config.overrideCollectProfile = null
        assertNull(config.overrideCollectProfile)
        config.overrideCollectProfile = "my.override"
        assertEquals("my.override", config.overrideCollectProfile)
    }

    @Test
    fun urls_DefaultsAreUsedWhenNotOverridden() {
        // no overrides set in [setUp]
        val collectDispatcher = CollectDispatcher(config, mockNetworkClient)

        assertEquals(CollectDispatcher.COLLECT_URL, collectDispatcher.eventUrl)
        assertEquals(CollectDispatcher.BULK_URL, collectDispatcher.batchEventUrl)
    }

    @Test
    fun urls_DefaultDomainGetsOverridden() {
        config.overrideCollectDomain = "cname.domain.com"
        config.overrideCollectUrl = null
        config.overrideCollectBatchUrl = null

        val collectDispatcher = CollectDispatcher(config, mockNetworkClient)

        assertEquals("https://cname.domain.com/event", collectDispatcher.eventUrl)
        assertEquals("https://cname.domain.com/bulk-event", collectDispatcher.batchEventUrl)
    }

    @Test
    fun urls_DefaultUrlsAreOverridden() {
        config.overrideCollectDomain = "cname.domain.com"
        // URL Overrides should take precedence
        config.overrideCollectUrl = "https://my.website.com/my-endpoint"
        config.overrideCollectBatchUrl = "https://my.website.com/my-bulk-endpoint"

        val collectDispatcher = CollectDispatcher(config, mockNetworkClient)

        assertEquals("https://my.website.com/my-endpoint", collectDispatcher.eventUrl)
        assertEquals("https://my.website.com/my-bulk-endpoint", collectDispatcher.batchEventUrl)
    }

    @Test
    fun consentLogging_OverrideProfile() = runBlocking {
        config.consentManagerLoggingProfile = "testingProfile"
        config.consentManagerLoggingUrl = null
        every { mockDispatch.addAll(any()) } just Runs

        val testDispatch = TealiumEvent(ConsentManagerConstants.GRANT_FULL_CONSENT)
        val collectDispatcher = CollectDispatcher(config, mockNetworkClient)
        collectDispatcher.onDispatchSend(testDispatch)

        coVerify {
            mockNetworkClient.post(
                match { str ->
                    JSONObject(str).let { payload ->
                        payload.getString("tealium_profile") == "testingProfile"
                    }
                },
                any(),
                any()
            )
        }
    }

    @Test
    fun consentLogging_OverrideUrl() = runBlocking {
        config.consentManagerLoggingProfile = null
        config.consentManagerLoggingUrl = "https://customUrl.com/my-endpoint"

        val testDispatch = TealiumEvent(ConsentManagerConstants.GRANT_FULL_CONSENT)
        val collectDispatcher = CollectDispatcher(config, mockNetworkClient)
        collectDispatcher.onDispatchSend(testDispatch)

        coVerify {
            mockNetworkClient.post(
                any(),
                match { str -> str == "https://customUrl.com/my-endpoint" },
                any()
            )
        }
    }

    @Test
    fun consentLogging_OverrideProfileAndUrl() = runBlocking {
        config.consentManagerLoggingProfile = "testingProfile"
        config.consentManagerLoggingUrl = "https://customUrl.com/my-endpoint"
        every { mockDispatch.addAll(any()) } just Runs

        val testDispatch = TealiumEvent(ConsentManagerConstants.GRANT_FULL_CONSENT)
        val collectDispatcher = CollectDispatcher(config, mockNetworkClient)
        collectDispatcher.onDispatchSend(testDispatch)

        coVerify {
            mockNetworkClient.post(
                match { str ->
                    JSONObject(str).let { payload ->
                        payload.getString("tealium_profile") == "testingProfile"
                    }
                },
                match { str -> str == "https://customUrl.com/my-endpoint" },
                any()
            )
        }
    }

    @Test
    fun events_IndividualEvents_AreEncodedCorrectly() = runBlocking {
        val collectDispatcher = CollectDispatcher(config, client = mockNetworkClient)
        collectDispatcher.onDispatchSend(mockDispatch)

        coVerify {
            mockNetworkClient.post(
                    match { str ->
                       JSONObject(str).let { payload ->
                           payload.getString("tealium_account") == "test-account"
                                   && payload.getString("tealium_profile") == "test-profile"
                       }
                    },
                    CollectDispatcher.COLLECT_URL,
                    false
            )
        }
    }

    @Test
    fun events_IndividualEvents_HaveProfileOverridden() = runBlocking {
        config.overrideCollectProfile = "test-override"

        val collectDispatcher = CollectDispatcher(config, client = mockNetworkClient)
        collectDispatcher.onDispatchSend(mockDispatch)

        coVerify {
            mockNetworkClient.post(
                    match { str ->
                        JSONObject(str).let { payload ->
                            payload.getString("tealium_account") == "test-account"
                                    && payload.getString("tealium_profile") == "test-override"
                        }
                    },
                    CollectDispatcher.COLLECT_URL,
                    false
            )
        }
    }

    @Test
    fun events_BatchEvents_AreEncodedCorrectly(): Unit = runBlocking {
        val collectDispatcher = CollectDispatcher(config, client = mockNetworkClient)
        collectDispatcher.onBatchDispatchSend(listOf(mockDispatch, mockDispatch))

        coVerify {
            mockNetworkClient.post(
                    match { str ->
                        JSONObject(str).let { payload ->
                            payload.getJSONObject("shared").getString("tealium_account") == "test-account"
                                    && payload.getJSONObject("shared").getString("tealium_profile") == "test-profile"
                                    && payload.getJSONArray("events").length() == 2
                        }
                    },
                    CollectDispatcher.BULK_URL,
                    true
            )
        }
    }

    @Test
    fun events_BatchEvents_HaveProfileOverridden() = runBlocking {
        config.overrideCollectProfile = "test-override"

        val collectDispatcher = CollectDispatcher(config, client = mockNetworkClient)
        collectDispatcher.onBatchDispatchSend(listOf(mockDispatch, mockDispatch))

        coVerify {
            mockNetworkClient.post(
                    match { str ->
                        JSONObject(str).let { payload ->
                            payload.getJSONObject("shared").getString("tealium_account") == "test-account"
                                    && payload.getJSONObject("shared").getString("tealium_profile") == "test-override"
                                    && payload.getJSONArray("events").length() == 2
                        }
                    },
                    CollectDispatcher.BULK_URL,
                    true
            )
        }
    }

    @Test
    fun consentLogging_BatchEvents_ProfileOverridden() = runBlocking {
//        config.options = mutableMapOf()
        config.consentManagerLoggingProfile = "testingProfile"
        config.consentManagerLoggingUrl = null


        val testDispatch = TealiumEvent(ConsentManagerConstants.GRANT_FULL_CONSENT)
        val collectDispatcher = CollectDispatcher(config, client = mockNetworkClient)
        collectDispatcher.onBatchDispatchSend(listOf(testDispatch, mockDispatch, mockDispatch, mockDispatch))

        testDispatch.addAll(mapOf(TEALIUM_PROFILE to "testingProfile"))
        val testConsentPayload = JSONObject(testDispatch.payload()).toString()

        val batchPayload = BatchDispatch.create(listOf(mockDispatch, mockDispatch, mockDispatch))
        val batch = JSONObject(batchPayload?.payload()).toString()

        coVerify {
            collectDispatcher.onDispatchSend(testDispatch)
            mockNetworkClient.post(
                testConsentPayload,
                CollectDispatcher.COLLECT_URL,
                any()
            )

            mockNetworkClient.post(
                batch,
                CollectDispatcher.BULK_URL,
                any()
            )
        }
    }

    @Test
    fun consentLogging_BatchEvents_UrlOverridden() = runBlocking {
        val config = TealiumConfig(mockApplication,
            config.accountName,
            config.profileName,
            config.environment,
            dataSourceId = config.dataSourceId)
        config.consentManagerLoggingUrl = "https://customUrl.com/my-endpoint"

        val testDispatch = TealiumEvent(ConsentManagerConstants.GRANT_FULL_CONSENT)
        val collectDispatcher = CollectDispatcher(config, client = mockNetworkClient)
        collectDispatcher.onBatchDispatchSend(listOf(testDispatch, mockDispatch, mockDispatch, mockDispatch))

        val str = JSONObject(testDispatch.payload()).toString()

        val batchPayload = BatchDispatch.create(listOf(mockDispatch, mockDispatch, mockDispatch))
        val batch = JSONObject(batchPayload?.payload()).toString()

        coVerify {
            collectDispatcher.onDispatchSend(testDispatch)
            mockNetworkClient.post(
                str,
                "https://customUrl.com/my-endpoint",
                any()
            )

            mockNetworkClient.post(
                batch,
                CollectDispatcher.BULK_URL,
                any()
            )
        }
    }

    @Test
    fun consentLogging_BatchEvents_ProfileAndUrlOverridden() = runBlocking {
        val config = TealiumConfig(mockApplication,
            config.accountName,
            config.profileName,
            config.environment,
            dataSourceId = config.dataSourceId)
        config.consentManagerLoggingProfile  = "testingProfile"
        config.consentManagerLoggingUrl = "https://customUrl.com/my-endpoint"

        val testDispatch = TealiumEvent(ConsentManagerConstants.GRANT_FULL_CONSENT)
        val collectDispatcher = CollectDispatcher(config, client = mockNetworkClient)
        collectDispatcher.onBatchDispatchSend(listOf(testDispatch, mockDispatch, mockDispatch, mockDispatch))

        testDispatch.addAll(mapOf(TEALIUM_PROFILE to "testingProfile"))
        val str = JSONObject(testDispatch.payload()).toString()

        coVerify {
            collectDispatcher.onDispatchSend(testDispatch)
            mockNetworkClient.post(
                str,
                "https://customUrl.com/my-endpoint",
                any()
            )
        }
    }

    @Test
    fun consentLogging_BatchEvents_NoOverrides() = runBlocking {
        val config = TealiumConfig(mockApplication,
            config.accountName,
            config.profileName,
            config.environment,
            dataSourceId = config.dataSourceId)

        val testDispatch = TealiumEvent(ConsentManagerConstants.GRANT_FULL_CONSENT)
        val collectDispatcher = CollectDispatcher(config, client = mockNetworkClient)
        collectDispatcher.onBatchDispatchSend(listOf(testDispatch, mockDispatch, mockDispatch, mockDispatch))

        coVerify {
            mockNetworkClient.post(
                match { str ->
                    JSONObject(str).let { payload ->
                        payload.getJSONObject("shared").getString("tealium_account") == "test-account"
                                && payload.getJSONObject("shared").getString("tealium_profile") == "test-profile"
                                && payload.getJSONArray("events").length() == 4
                    }
                },
                CollectDispatcher.BULK_URL,
                any()
            )
        }
    }

    @Test
    fun listener_ReceivesHttpResponses() {
        val listener = mockk<CollectDispatcherListener>()
        every { listener.successfulTrack() } just Runs
        every { listener.unsuccessfulTrack(any()) } just Runs

        val networkClient = spyk(HttpClient(config))
        val collectDispatcher = CollectDispatcher(config,
                client = networkClient,
                collectDispatchListener = listener)

        networkClient.networkClientListener?.onNetworkResponse(200, "")
        networkClient.networkClientListener?.onNetworkResponse(404, "Not Found")
        networkClient.networkClientListener?.onNetworkError("My Error")

        verify {
            listener.successfulTrack()
            listener.unsuccessfulTrack("Network error, response: Not Found")
        }
    }

    @Test
    fun serialization_Arrays_AreSerializedCorrectly() = runBlocking {
        val collectDispatcher = CollectDispatcher(config, client = mockNetworkClient)
        every { mockDispatch.payload() } returns mapOf(
                "string_array" to arrayOf("value_1", "value_2"),
                "int_array" to arrayOf(10, 20),
                "double_array" to arrayOf(10.5, 20.75),
                "boolean_array" to arrayOf(true, false),
                "object_array" to arrayOf(mapOf("string" to "value"), mapOf("int" to 1), mapOf("boolean" to false))
        )

        collectDispatcher.onDispatchSend(mockDispatch)

        coVerify {
            mockNetworkClient.post(
                    match { str ->
                        JSONObject(str).let { payload ->
                            payload.getJSONArray("string_array").get(0) == "value_1"
                                    && payload.getJSONArray("string_array").get(1) == "value_2"
                                    && payload.getJSONArray("int_array").get(0) == 10
                                    && payload.getJSONArray("int_array").get(1) == 20
                                    && payload.getJSONArray("double_array").get(0) == 10.5
                                    && payload.getJSONArray("double_array").get(1) == 20.75
                                    && payload.getJSONArray("boolean_array").get(0) == true
                                    && payload.getJSONArray("boolean_array").get(1) == false
                                    && payload.getJSONArray("object_array").getJSONObject(0).get("string") == "value"
                                    && payload.getJSONArray("object_array").getJSONObject(1).get("int") == 1
                                    && payload.getJSONArray("object_array").getJSONObject(2).get("boolean") == false
                        }
                    },
                    CollectDispatcher.COLLECT_URL,
                    false
            )
        }
    }

    @Test
    fun serialization_Lists_AreSerializedCorrectly() = runBlocking {
        val collectDispatcher = CollectDispatcher(config, client = mockNetworkClient)
        every { mockDispatch.payload() } returns mapOf(
                "string_list" to listOf("value_1", "value_2"),
                "int_list" to listOf(10, 20),
                "double_list" to listOf(10.5, 20.75),
                "boolean_list" to listOf(true, false),
                "object_list" to listOf(mapOf("string" to "value"), mapOf("int" to 1), mapOf("boolean" to false))
        )

        collectDispatcher.onDispatchSend(mockDispatch)

        coVerify {
            mockNetworkClient.post(
                    match { str ->
                        JSONObject(str).let { payload ->
                            payload.getJSONArray("string_list").get(0) == "value_1"
                                    && payload.getJSONArray("string_list").get(1) == "value_2"
                                    && payload.getJSONArray("int_list").get(0) == 10
                                    && payload.getJSONArray("int_list").get(1) == 20
                                    && payload.getJSONArray("double_list").get(0) == 10.5
                                    && payload.getJSONArray("double_list").get(1) == 20.75
                                    && payload.getJSONArray("boolean_list").get(0) == true
                                    && payload.getJSONArray("boolean_list").get(1) == false
                                    && payload.getJSONArray("object_list").getJSONObject(0).get("string") == "value"
                                    && payload.getJSONArray("object_list").getJSONObject(1).get("int") == 1
                                    && payload.getJSONArray("object_list").getJSONObject(2).get("boolean") == false
                        }
                    },
                    CollectDispatcher.COLLECT_URL,
                    false
            )
        }
    }

    @Test
    fun serialization_Maps_AreSerializedCorrectly() = runBlocking {
        val collectDispatcher = CollectDispatcher(config, client = mockNetworkClient)
        every { mockDispatch.payload() } returns mapOf(
                "string_map" to mapOf("string" to "value"),
                "int_map" to mapOf("int" to 20),
                "double_map" to mapOf("double" to 20.75),
                "boolean_map" to mapOf("true" to false),
                "object_map" to mapOf("map1" to mapOf("string" to "value", "boolean" to true)) // TODO
        )

        collectDispatcher.onDispatchSend(mockDispatch)

        coVerify {
            mockNetworkClient.post(
                    match { str ->
                        JSONObject(str).let { payload ->
                            payload.getJSONObject("string_map").get("string") == "value"
                                    && payload.getJSONObject("int_map").get("int") == 20
                                    && payload.getJSONObject("double_map").get("double") == 20.75
                                    && payload.getJSONObject("boolean_map").get("true") == false
                                    && payload.getJSONObject("object_map").getJSONObject("map1").get("string") == "value"
                                    && payload.getJSONObject("object_map").getJSONObject("map1").get("boolean") == true
                        }
                    },
                    CollectDispatcher.COLLECT_URL,
                    false
            )
        }
    }
}