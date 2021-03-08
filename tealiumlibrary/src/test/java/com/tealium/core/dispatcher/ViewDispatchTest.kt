package com.tealium.core.dispatcher

import com.tealium.dispatcher.TealiumView
import org.junit.Test
import org.junit.Assert.*

class ViewDispatchTest {

    @Test
    fun payloadHasDefaultValues() {
        val data: MutableMap<String, Any> = mutableMapOf("key" to "value", "screen_title" to "home")
        val viewDispatch = TealiumView("test", data)

        assertSame("home", viewDispatch.payload()["screen_title"])
        assertSame(DispatchType.VIEW, viewDispatch.payload()[CoreConstant.TEALIUM_EVENT_TYPE])
        assertSame("test", viewDispatch.payload()[CoreConstant.TEALIUM_EVENT])
        assertSame(viewDispatch.id, viewDispatch.payload()[CoreConstant.REQUEST_UUID])
    }
}