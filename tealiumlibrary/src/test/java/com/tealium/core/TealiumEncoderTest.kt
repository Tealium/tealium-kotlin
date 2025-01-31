package com.tealium.core

import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.TealiumView
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

class TealiumEncoderTest {

    @Before
    fun setUp() {
        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns "test_id"
    }

    @After
    fun tearDown() {
        unmockkStatic(UUID::class)
    }

    @Test
    fun encodeViewDispatchStringPayload() {
        val dispatch = TealiumView("my_view", mapOf("test_key" to "test_value"))
        val result = TealiumEncoder.encode(dispatch)
        var expected = "${Dispatch.Keys.TEALIUM_EVENT_TYPE}=view"
        expected += "&${Dispatch.Keys.TEALIUM_EVENT}=my_view"
        expected += "&${Dispatch.Keys.SCREEN_TITLE}=my_view"
        expected += "&${Dispatch.Keys.REQUEST_UUID}=test_id"
        expected += "&test_key=test_value"
        assertTrue("Expected \n$expected does not match \n$result", expected == result)
    }

    @Test
    fun encodeViewDispatchStringWithSpacesPayload() {
        val dispatch = TealiumView("my_view", mapOf("test_key" to "value with spaces"))
        val result = TealiumEncoder.encode(dispatch)
        var expected = "${Dispatch.Keys.TEALIUM_EVENT_TYPE}=view"
        expected += "&${Dispatch.Keys.TEALIUM_EVENT}=my_view"
        expected += "&${Dispatch.Keys.SCREEN_TITLE}=my_view"
        expected += "&${Dispatch.Keys.REQUEST_UUID}=test_id"
        expected += "&test_key=value+with+spaces"
        assertTrue("Expected \n$expected does not match \n$result", expected == result)
    }

    @Test
    fun encodeViewDispatchIntPayload() {
        val dispatch = TealiumView("my_view", mapOf("test_key" to 1234))
        val result = TealiumEncoder.encode(dispatch)
        var expected = "${Dispatch.Keys.TEALIUM_EVENT_TYPE}=view"
        expected += "&${Dispatch.Keys.TEALIUM_EVENT}=my_view"
        expected += "&${Dispatch.Keys.SCREEN_TITLE}=my_view"
        expected += "&${Dispatch.Keys.REQUEST_UUID}=test_id"
        expected += "&test_key=1234"
        assertTrue("Expected $expected does not match $result", expected == result)
    }

    @Test
    fun encodeViewDispatchFloatPayload() {
        val dispatch = TealiumView("my_view", mapOf("test_key" to 12.34f))
        val result = TealiumEncoder.encode(dispatch)
        var expected = "${Dispatch.Keys.TEALIUM_EVENT_TYPE}=view"
        expected += "&${Dispatch.Keys.TEALIUM_EVENT}=my_view"
        expected += "&${Dispatch.Keys.SCREEN_TITLE}=my_view"
        expected += "&${Dispatch.Keys.REQUEST_UUID}=test_id"
        expected += "&test_key=12.34"
        assertTrue("Expected $expected does not match $result", expected == result)
    }

    @Test
    fun encodeViewDispatchArrayPayload() {
        val dispatch = TealiumView("my_view", mapOf("test_key" to arrayOf("1", "2", "3")))
        val result = TealiumEncoder.encode(dispatch)
        val commaEncoded = "%2C"
        var expected = "${Dispatch.Keys.TEALIUM_EVENT_TYPE}=view"
        expected += "&${Dispatch.Keys.TEALIUM_EVENT}=my_view"
        expected += "&${Dispatch.Keys.SCREEN_TITLE}=my_view"
        expected += "&${Dispatch.Keys.REQUEST_UUID}=test_id"
        expected += "&test_key=1${commaEncoded}2${commaEncoded}3"
        assertTrue("Expected $expected does not match $result", expected == result)
    }

    @Test
    fun encodeViewDispatchListPayload() {
        val dispatch = TealiumView("my_view", mapOf("test_key" to listOf("1", "2", "3")))
        val result = TealiumEncoder.encode(dispatch)
        val commaEncoded = "%2C"
        var expected = "${Dispatch.Keys.TEALIUM_EVENT_TYPE}=view"
        expected += "&${Dispatch.Keys.TEALIUM_EVENT}=my_view"

        assertTrue(result.contains("1$commaEncoded"))
        assertTrue(result.contains("2$commaEncoded"))
        assertTrue(result.contains("3"))
        assertTrue(result.contains("${Dispatch.Keys.TEALIUM_EVENT_TYPE}=view"))
        assertTrue(result.contains("${Dispatch.Keys.TEALIUM_EVENT}=my_view"))
    }
}