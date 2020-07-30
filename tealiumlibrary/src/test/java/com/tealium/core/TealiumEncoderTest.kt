package com.tealium.core

import com.tealium.dispatcher.ViewDispatch
import junit.framework.Assert.assertTrue
import org.junit.Test

class TealiumEncoderTest {

    @Test
    fun encodeViewDispatchStringPayload() {
        val dispatch = ViewDispatch("my_view", mapOf("test_key" to "test_value"))
        val result = TealiumEncoder.encode(dispatch)
        var expected = "${CoreConstant.SCREEN_TITLE}=my_view"
        expected += "&${CoreConstant.TEALIUM_EVENT_TYPE}=view"
        expected += "&${CoreConstant.TEALIUM_EVENT}=my_view"
        expected += "&test_key=test_value"
        assertTrue("Expected \n$expected does not match \n$result", expected == result)
    }

    @Test
    fun encodeViewDispatchStringWithSpacesPayload() {
        val dispatch = ViewDispatch("my_view", mapOf("test_key" to "value with spaces"))
        val result = TealiumEncoder.encode(dispatch)
        var expected = "${CoreConstant.SCREEN_TITLE}=my_view"
        expected += "&${CoreConstant.TEALIUM_EVENT_TYPE}=view"
        expected += "&${CoreConstant.TEALIUM_EVENT}=my_view"
        expected += "&test_key=value+with+spaces"
        assertTrue("Expected \n$expected does not match \n$result", expected == result)
    }

    @Test
    fun encodeViewDispatchIntPayload() {
        val dispatch = ViewDispatch("my_view", mapOf("test_key" to 1234))
        val result = TealiumEncoder.encode(dispatch)
        var expected = "${CoreConstant.SCREEN_TITLE}=my_view"
        expected += "&${CoreConstant.TEALIUM_EVENT_TYPE}=view"
        expected += "&${CoreConstant.TEALIUM_EVENT}=my_view"
        expected += "&test_key=1234"
        assertTrue("Expected $expected does not match $result", expected == result)
    }

    @Test
    fun encodeViewDispatchFloatPayload() {
        val dispatch = ViewDispatch("my_view", mapOf("test_key" to 12.34f))
        val result = TealiumEncoder.encode(dispatch)
        var expected = "${CoreConstant.SCREEN_TITLE}=my_view"
        expected += "&${CoreConstant.TEALIUM_EVENT_TYPE}=view"
        expected += "&${CoreConstant.TEALIUM_EVENT}=my_view"
        expected += "&test_key=12.34"
        assertTrue("Expected $expected does not match $result", expected == result)
    }

    @Test
    fun encodeViewDispatchArrayPayload() {
        val dispatch = ViewDispatch("my_view", mapOf("test_key" to arrayOf("1", "2", "3")))
        val result = TealiumEncoder.encode(dispatch)
        val commaEncoded = "%2C"
        var expected = "${CoreConstant.SCREEN_TITLE}=my_view"
        expected += "&${CoreConstant.TEALIUM_EVENT_TYPE}=view"
        expected += "&${CoreConstant.TEALIUM_EVENT}=my_view"
        expected += "&test_key=1${commaEncoded}2${commaEncoded}3"
        assertTrue("Expected $expected does not match $result", expected == result)
    }

    @Test
    fun encodeViewDispatchListPayload() {
        val dispatch = ViewDispatch("my_view", mapOf("test_key" to listOf("1", "2", "3")))
        val result = TealiumEncoder.encode(dispatch)
        val commaEncoded = "%2C"
        var expected = "${CoreConstant.SCREEN_TITLE}=my_view"
        expected += "&${CoreConstant.TEALIUM_EVENT_TYPE}=view"
        expected += "&${CoreConstant.TEALIUM_EVENT}=my_view"

        assertTrue(result.contains("1$commaEncoded"))
        assertTrue(result.contains("2$commaEncoded"))
        assertTrue(result.contains("3"))
        assertTrue(result.contains("${CoreConstant.SCREEN_TITLE}=my_view"))
        assertTrue(result.contains("${CoreConstant.TEALIUM_EVENT_TYPE}=view"))
        assertTrue(result.contains("${CoreConstant.TEALIUM_EVENT}=my_view"))
    }
}