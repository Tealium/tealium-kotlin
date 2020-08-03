package com.tealium.hosteddatalayer

import com.tealium.core.TealiumConfig
import com.tealium.core.network.Connectivity
import com.tealium.core.network.HttpClient
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.EventDispatch
import io.mockk.*
import io.mockk.impl.annotations.MockK
import junit.framework.Assert.*
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class HostedDataLayerTests {

    @MockK
    lateinit var mockConfig: TealiumConfig

    @MockK
    lateinit var mockConnectivity: Connectivity

    @MockK(relaxed = true)
    lateinit var mockStore: DataLayerStore

    lateinit var mockDispatch: Dispatch
    lateinit var mockHttpClient: HttpClient

    private val defaultCacheSize = 5
    private val defaultCacheTime = 5L

    private lateinit var hostedDataLayer: HostedDataLayer
    private val validJsonResponse = """{
                "product_category": "shoes",
                "product_color" : "red",
                "product_price" : 10.50
            }"""

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        mockDispatch = spyk(EventDispatch("event_name"))

        // reasonable defaults
        every { mockConfig.accountName } returns "account"
        every { mockConfig.profileName } returns "profile"
        every { mockConfig.hostedDataLayerEventMappings } returns mapOf("event_name" to "lookup_key")
        every { mockConfig.hostedDataLayerMaxCacheSize } returns defaultCacheSize
        every { mockConfig.hostedDataLayerMaxCacheTimeMinutes } returns defaultCacheTime
        every { mockConnectivity.isConnected() } returns true
        every { mockConnectivity.isConnectedWifi() } returns true

        mockHttpClient = mockk()

        hostedDataLayer = spyk(HostedDataLayer(mockConfig, mockStore, mockHttpClient),
                recordPrivateCalls = true)
    }

    @Test
    fun eventMappings_CorrectDataLayerIdIsFound() = runBlocking {
        every { mockDispatch.get("lookup_key") } returns "12345"
        every { mockStore.contains("12345") } returns false
        coEvery { mockHttpClient.get(any()) } returns validJsonResponse

        hostedDataLayer.transform(mockDispatch)
        verify {
            mockStore.contains("12345")
        }
    }

    @Test
    fun eventMappings_StoreNotCheckedWhenNoMapping() = runBlocking {
        every { mockDispatch.get("lookup_key") } returns null

        hostedDataLayer.transform(mockDispatch)
        verify(exactly = 0) {
            mockStore.contains(any())
        }
    }

    @Test
    fun eventMappings_StoreNotCheckedWhenMappingIsEmpty() = runBlocking {
        every { mockDispatch.get("lookup_key") } returns ""

        hostedDataLayer.transform(mockDispatch)
        verify(exactly = 0) {
            mockStore.contains(any())
        }
    }

    @Test
    fun fetch_ResourceIsFetchedWhenNotCached() = runBlocking {
        every { mockDispatch.get("lookup_key") } returns "12345"
        every { mockStore.contains("12345") } returns false
        coEvery { mockHttpClient.get(any()) } returns validJsonResponse

        hostedDataLayer.transform(mockDispatch)
        verify {
            hostedDataLayer invoke "fetch" withArguments listOf("12345")
        }
    }

    @Test
    fun fetch_ResourceIsCachedWhenSuccessfullyFetched() = runBlocking {
        every { mockDispatch.get("lookup_key") } returns "12345"
        every { mockStore.contains("12345") } returns false
        coEvery { mockHttpClient.get(any()) } returns validJsonResponse

        hostedDataLayer.transform(mockDispatch)
        verify {
            mockStore.insert(any())
        }
    }

    @Test
    fun fetch_ResourceIsNotCachedWhenFailedFetch() = runBlocking {
        every { mockDispatch.get("lookup_key") } returns "12345"
        every { mockStore.contains("12345") } returns false
        coEvery { mockHttpClient.get(any()) } returns "//"

        hostedDataLayer.transform(mockDispatch)
        verify(exactly = 0) {
            mockStore.insert(any())
        }
    }

    @Test
    fun queueing_shouldQueueWhenAwaitingResponse() = runBlocking {
        every { mockDispatch.get("lookup_key") } returns "12345"
        every { mockStore.contains("12345") } returns false

        coEvery { mockHttpClient.get(any()) } coAnswers {
            delay(1500)
            validJsonResponse
        }

        val job = async {
            hostedDataLayer.transform(mockDispatch)
            // fetching completed; should no longer queue
            assertFalse(hostedDataLayer.shouldQueue(mockDispatch))
        }
        delay(500)
        assertTrue(hostedDataLayer.shouldQueue(mockDispatch))
        job.await()
    }

    @Test
    fun merge_MergesValues() = runBlocking {
        val dispatch = EventDispatch("event_name", mapOf("lookup_key" to "12345"))
        every { mockStore.contains("12345") } returns true
        every { mockStore.get("12345") } returns HostedDataLayerEntry("12345",
                100,
                JSONObject(validJsonResponse))
        hostedDataLayer.transform(dispatch)

        assertEquals("shoes", dispatch.get("product_category"))
        assertEquals("red", dispatch.get("product_color"))
        assertEquals(10.50, dispatch.get("product_price"))
    }

    @Test
    fun cache_ClearsCache() {
        hostedDataLayer.clearCache()

        verify {
            mockStore.clear()
        }
    }
}