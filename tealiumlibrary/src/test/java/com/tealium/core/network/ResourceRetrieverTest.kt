package com.tealium.core.network

import android.app.Application
import android.util.Log
import com.tealium.core.TealiumConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.io.File

class ResourceRetrieverTest {

    lateinit var config: TealiumConfig
    lateinit var mockContext: Application

    @Before
    fun setUp() {
        mockContext = mockk()

        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
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

    private suspend fun retryTester(i: Int): Int? {
        println("attempt: $i")
        delay(if (i != 3) 1000L else 200L)
        return 100
    }

    private suspend fun retryTesterOptional(i: Int): Int? {
        println("attempt: $i")
        delay(if (i != 10) 1000L else 200L)
        return null
    }
}