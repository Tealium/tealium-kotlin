package com.tealium.core.network

import android.util.Log
import android.webkit.URLUtil
import com.tealium.core.TealiumConfig
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HttpClientTest {

    @MockK
    lateinit var mockConfig: TealiumConfig

    @MockK
    lateinit var mockConnectivity: Connectivity

    private var mockNetworkClientListener: NetworkClientListener? = null

    lateinit var mockWebServer: MockWebServer

    lateinit var httpClient: HttpClient

    private val port = 8888

    var status: Int? = null
    var response: String? = null
    var errorMessage: String? = null

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockNetworkClientListener = object: NetworkClientListener {
            override fun onNetworkResponse(status: Int, response: String) {
                this@HttpClientTest.status = status
                this@HttpClientTest.response = response
            }

            override fun onNetworkError(message: String) {
                errorMessage = message
            }
        }

        httpClient = HttpClient(mockConfig, mockConnectivity, mockNetworkClientListener)
        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        mockNetworkClientListener = null
    }

    @Test
    fun postSuccess() = runBlockingTest {
        every { mockConnectivity.isConnected() } returns true
        mockkStatic(URLUtil::class)
        every { URLUtil.isValidUrl(any()) } returns true
        mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse()
                .setBody("hello")
                .setResponseCode(200))
        mockWebServer.start(port)

        val urlString = "http://localhost:$port"
        mockWebServer.url(urlString)

        httpClient.post("hello", urlString, false, null)

        val request = mockWebServer.takeRequest()
        assertEquals("POST / HTTP/1.1", request.requestLine)
        assertEquals("application/json", request.getHeader("Content-Type"))
        assertEquals("hello", request.body.readUtf8())
        assertEquals(200, status)
        assertEquals("OK", response)
    }

    @Test
    fun postFailsUnknownHost() = runBlockingTest {
        every { mockConnectivity.isConnected() } returns true
        mockkStatic(URLUtil::class)
        every { URLUtil.isValidUrl(any()) } returns true
        mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse()
                .setBody("hello")
                .setResponseCode(200))
        mockWebServer.start(port)

        val urlString = "http://localhost:1234"
        mockWebServer.url(urlString)

        assertNull(errorMessage)
        httpClient.post("hello", urlString, false, null)
        assertNotNull(errorMessage)
    }

    @Test
    fun ifModifiedReturnsModified() = runBlocking {
        every { mockConnectivity.isConnected() } returns true
        mockkStatic(URLUtil::class)
        every { URLUtil.isValidUrl(any()) } returns true
        mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse()
                .setResponseCode(200))
        mockWebServer.start(port)

        val urlString = "http://localhost:$port"
        mockWebServer.url(urlString)

        val isModified = httpClient.ifModified(urlString, System.currentTimeMillis())
        assertTrue(isModified!!)

        val request = mockWebServer.takeRequest()
        assertEquals("HEAD / HTTP/1.1", request.requestLine)
        assertEquals(200, status)
        assertEquals("OK", response)
    }

    @Test
    fun ifModifiedReturnsNotModified() = runBlocking {
        every { mockConnectivity.isConnected() } returns true
        mockkStatic(URLUtil::class)
        every { URLUtil.isValidUrl(any()) } returns true
        mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse()
                .setResponseCode(304))
        mockWebServer.start(port)

        val urlString = "http://localhost:$port"
        mockWebServer.url(urlString)

        val isModified = httpClient.ifModified(urlString, System.currentTimeMillis())
        assertFalse(isModified!!)

        val request = mockWebServer.takeRequest()
        assertEquals("HEAD / HTTP/1.1", request.requestLine)
        assertEquals(304, status)
    }

    @Test
    fun ifModifiedReturnsNullOnException() = runBlocking {
        every { mockConnectivity.isConnected() } returns true
        mockkStatic(URLUtil::class)
        every { URLUtil.isValidUrl(any()) } returns true
        mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse()
                .setResponseCode(304))
        mockWebServer.start(port)

        val urlString = "http://localhost:1234"
        mockWebServer.url(urlString)

        val isModified = httpClient.ifModified(urlString, System.currentTimeMillis())
        assertNull(isModified)
        assertNotNull(errorMessage)
    }

    @Test
    fun ifModifiedReturnsNullNoConnection() = runBlocking {
        every { mockConnectivity.isConnected() } returns false
        mockWebServer = MockWebServer()
        val isModified = httpClient.ifModified("http://test.com", System.currentTimeMillis())
        assertNull(isModified)
        assertNotNull(errorMessage)
    }

    @Test
    fun getJsonSuccess() = runBlocking {
        every { mockConnectivity.isConnected() } returns true
        mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse()
                .setResponseCode(200)
                .setBody("{\"hello\":\"world\"}"))
        mockWebServer.start(port)

        val urlString = "http://localhost:$port"
        mockWebServer.url(urlString)

        val json = httpClient.get(urlString)
        assertNotNull(json)
        val value = JSONObject(json!!).getString("hello")
        assertEquals("world", value)

        val request = mockWebServer.takeRequest()
        assertEquals("GET / HTTP/1.1", request.requestLine)
    }

    @Test
    fun getJsonReturnsNullNoConnection() = runBlocking {
        every { mockConnectivity.isConnected() } returns false
        mockWebServer = MockWebServer()
        val json = httpClient.get("http://test.com")
        assertNull(json)
        assertNotNull(errorMessage)
    }
}
