package com.tealium.core.dispatcher

import com.tealium.dispatcher.ViewDispatch
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DispatchTest {

    @Test
    fun toJsonStringEncodesString() {
        val data: MutableMap<String, Any> = mutableMapOf("key" to "value")
        val viewDispatch = ViewDispatch("test", data)

        val result = viewDispatch.toJsonString()
        val match = Regex("\"key\":\"value\"").find(result)
        Assert.assertNotNull(match)
    }

    @Test
    fun toJsonStringEncodesInt() {
        val data: MutableMap<String, Any> = mutableMapOf("key" to 123)
        val viewDispatch = ViewDispatch("test", data)

        val result = viewDispatch.toJsonString()
        val match = Regex("\"key\":123").find(result)
        Assert.assertNotNull(match)
    }

    @Test
    fun toJsonStringEncodesFloat() {
        val data: MutableMap<String, Any> = mutableMapOf("key" to 12.34f)
        val viewDispatch = ViewDispatch("test", data)

        val result = viewDispatch.toJsonString()
        val match = Regex("\"key\":12[.]34").find(result)
        Assert.assertNotNull(match)
    }

    @Test
    fun toJsonStringEncodesDouble() {
        val data: MutableMap<String, Any> = mutableMapOf("key" to 12.34)
        val viewDispatch = ViewDispatch("test", data)

        val result = viewDispatch.toJsonString()
        val match = Regex("\"key\":12[.]34").find(result)
        Assert.assertNotNull(match)
    }

    @Test
    fun toJsonStringEncodesStringArray() {
        val data: MutableMap<String, Any> = mutableMapOf("key" to listOf("a", "b", "c"))
        val viewDispatch = ViewDispatch("test", data)

        val result = viewDispatch.toJsonString()
       println(result)
        // TODO: more tests for lists, arrays,. this test isn't complete itself!
    }
}