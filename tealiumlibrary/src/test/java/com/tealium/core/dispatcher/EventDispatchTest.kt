package com.tealium.core.dispatcher

import com.tealium.dispatcher.TealiumEvent
import org.junit.Test
import org.junit.Assert.*

class EventDispatchTest {

    @Test
    fun payloadHasDefaultValues() {
        val data: MutableMap<String, Any> = mutableMapOf("key" to "value")
        val eventDispatch = TealiumEvent("test", data)

        assertSame("value", eventDispatch.payload()["key"])
        assertSame(DispatchType.EVENT, eventDispatch.payload()[CoreConstant.TEALIUM_EVENT_TYPE])
        assertSame("test", eventDispatch.payload()[CoreConstant.TEALIUM_EVENT])
        assertSame(eventDispatch.id, eventDispatch.payload()[CoreConstant.REQUEST_UUID])
    }
}