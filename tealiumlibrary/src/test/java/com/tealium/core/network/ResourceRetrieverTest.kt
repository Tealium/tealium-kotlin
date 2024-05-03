package com.tealium.core.network

import android.app.Application
import android.util.Log
import com.tealium.core.TealiumConfig
import com.tealium.core.persistence.getTimestampMilliseconds
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.async
import org.junit.Assert.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class ResourceRetrieverTest {

    lateinit var config: TealiumConfig
    lateinit var mockContext: Application
    lateinit var mockNetworkClient: NetworkClient

    @Before
    fun setUp() {
        config = mockk(relaxed = true)
        mockContext = mockk()
        mockNetworkClient = mockk()

        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
    }

    @Test
    fun retrySuccess() = runBlocking {
        var attempts = 0
        val result = ResourceRetriever.retry(5, 500) {
            attempts += 1
            retryTester(it)
        }
        assertEquals(100, result)
        assertEquals(3, attempts)
    }

    @Test
    fun retryReturnsOptional() = runBlocking {
        var attempts = 0
        val result = ResourceRetriever.retry(5, 500) {
            attempts += 1
            retryTesterOptional(it)
        }
        assertNull(result)
        assertEquals(6, attempts)
    }

    @Test
    fun retry_Only_Executes_A_Maximum_Number_Of_Times() = runBlocking {
        var attempts = 0
        val result = ResourceRetriever.retryWithBackoff(
            numRetries = 5,
            retryDelayMs = 0,
            timeout = 500,
            retryWhile = { true }) {
            attempts += 1
            retryTesterOptional(it)
        }
        assertNull(result)
        assertEquals(6, attempts)
    }

    @Test
    fun retry_Retries_Only_While_Predicate_Is_True() = runBlocking {
        var attempts = 0
        val result = ResourceRetriever.retryWithBackoff(
            numRetries = 10,
            retryDelayMs = 0,
            timeout = 500,
            retryWhile = { attempts < 3 }) {
            attempts += 1
            retryTesterOptional(it)
        }
        assertNull(result)
        assertEquals(3, attempts)
    }

    @Test
    fun retry_Delays_Next_Retry() = runBlocking {
        val block = mockk<(Int) -> Int?>(relaxed = true)
        every { block.invoke(any()) } returns 1

        val startTime = getTimestampMilliseconds()
        ResourceRetriever.retryWithBackoff(
            numRetries = 1,
            retryDelayMs = 1000,
            timeout = 500,
            retryWhile = { true },
            block
        )
        val endTime = getTimestampMilliseconds()

        assertTrue(endTime - startTime > 1000)
        assertTrue(endTime - startTime < 1500)
        verify {
            block(0)
            block(1)
        }
        confirmVerified(block)
    }

    @Test
    fun fetchWithEtag_Doesnt_Fetch_If_Already_Fetching() = runBlocking {
        coEvery { mockNetworkClient.getResourceEntity(any(), any()) } coAnswers {
            delay(100)
            ResourceEntity()
        }
        val resourceRetriever =
            ResourceRetriever(config, "https://localhost:8888", mockNetworkClient)
        val result1 = async {
            resourceRetriever.fetchWithEtag(null)
        }
        val result2 = async {
            resourceRetriever.fetchWithEtag(null)
        }

        assertNotNull(result1.await())
        assertNull(result2.await())
    }

    @Test
    fun fetchWithEtag_Doesnt_Fetch_If_Last_Fetch_Date_Too_Soon() = runBlocking {
        coEvery { mockNetworkClient.getResourceEntity(any(), any()) } coAnswers {
            delay(100)
            ResourceEntity()
        }
        val resourceRetriever =
            ResourceRetriever(config, "https://localhost:8888", mockNetworkClient)
        val result1 = resourceRetriever.fetchWithEtag(null)
        val result2 = resourceRetriever.fetchWithEtag(null)

        assertNotNull(result1)
        assertNull(result2)
    }

    @Test
    fun shouldRetry_True_When_Response_Is_Null() {
        assertTrue(ResourceRetriever.shouldRetry(null))
    }

    @Test
    fun shouldRetry_False_When_Response_Is_Success() {
        assertFalse(ResourceRetriever.shouldRetry(ResponseStatus.Success))
    }

    @Test
    fun shouldRetry_False_When_Response_Is_Cancelled() {
        assertFalse(ResourceRetriever.shouldRetry(ResponseStatus.Cancelled))
    }

    @Test
    fun shouldRetry_True_When_Response_Is_Unknown() {
        assertTrue(ResourceRetriever.shouldRetry(ResponseStatus.UnknownError(null)))
    }

    @Test
    fun shouldRetry_True_When_Response_Is_Retryable_ResponseCode() {
        assertTrue(ResourceRetriever.shouldRetry(ResponseStatus.Non200Response(408)))
        assertTrue(ResourceRetriever.shouldRetry(ResponseStatus.Non200Response(429)))
        assertTrue(ResourceRetriever.shouldRetry(ResponseStatus.Non200Response(500)))
        assertTrue(ResourceRetriever.shouldRetry(ResponseStatus.Non200Response(503)))
        assertTrue(ResourceRetriever.shouldRetry(ResponseStatus.Non200Response(550)))
        assertTrue(ResourceRetriever.shouldRetry(ResponseStatus.Non200Response(599)))

        assertFalse(ResourceRetriever.shouldRetry(ResponseStatus.Non200Response(201)))
        assertFalse(ResourceRetriever.shouldRetry(ResponseStatus.Non200Response(301)))
        assertFalse(ResourceRetriever.shouldRetry(ResponseStatus.Non200Response(401)))
        assertFalse(ResourceRetriever.shouldRetry(ResponseStatus.Non200Response(404)))
    }

    private suspend fun retryTester(i: Int): Int? {
        delay(if (i != 3) 1000L else 200L)
        return 100
    }

    private suspend fun retryTesterOptional(i: Int): Int? {
        delay(if (i != 10) 1000L else 200L)
        return null
    }
}