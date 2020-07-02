package com.tealium.core.network

import com.tealium.core.TealiumConfig
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HttpClientListenerTest {

    @MockK
    lateinit var mockConfig: TealiumConfig

    @MockK
    lateinit var mockConnectivity: Connectivity

    @MockK
    lateinit var mockNetworkClientListener: NetworkClientListener

    lateinit var httpClient: HttpClient

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        httpClient = HttpClient(mockConfig, mockConnectivity, mockNetworkClientListener)
    }

    @Test
    fun networkErrorReceivedWhenNotConnected() = runBlocking {
        every { mockConnectivity.isConnected() } returns false
        every { mockNetworkClientListener.onNetworkError(any()) } just Runs

        httpClient.post("test", "test_url", false)

        verify {
            mockNetworkClientListener.onNetworkError("No network connection.")
        }
        confirmVerified(mockNetworkClientListener)
    }

    @Test
    fun networkErrorReceivedWhenInvalidURL() = runBlocking {
        every { mockConnectivity.isConnected() } returns true
        every { mockNetworkClientListener.onNetworkError(any()) } just Runs

        val urlString = "invalid_url"
        httpClient.post("test", urlString, false)

        verify {
            mockNetworkClientListener.onNetworkError("Invalid URL: $urlString.")
        }
        confirmVerified(mockNetworkClientListener)
    }
}