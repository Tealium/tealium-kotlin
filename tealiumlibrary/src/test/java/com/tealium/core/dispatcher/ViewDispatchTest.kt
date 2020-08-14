package com.tealium.core.dispatcher

import com.tealium.dispatcher.ViewDispatch
import org.junit.Test

import org.junit.Assert.*

class ViewDispatchTest {

    @Test
    fun payloadHasDefaultValues() {
        val data: MutableMap<String, Any> = mutableMapOf("key" to "value")
        val viewDispatch = ViewDispatch("test", data)

        assertSame("test", viewDispatch.payload()[CoreConstant.SCREEN_TITLE])
        assertSame(DispatchType.VIEW, viewDispatch.payload()[CoreConstant.TEALIUM_EVENT_TYPE])
        assertSame("test", viewDispatch.payload()[CoreConstant.TEALIUM_EVENT])
    }
}