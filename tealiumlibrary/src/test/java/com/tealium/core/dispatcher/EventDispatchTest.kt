package com.tealium.core.dispatcher

import com.tealium.core.DispatchType
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.TealiumEvent
import org.junit.Test
import org.junit.Assert.*

class EventDispatchTest {

    @Test
    fun payloadHasDefaultValues() {
        val data: MutableMap<String, Any> = mutableMapOf("key" to "value")
        val eventDispatch = TealiumEvent("test", data)

        assertSame("value", eventDispatch.payload()["key"])
        assertSame(DispatchType.EVENT, eventDispatch.payload()[Dispatch.Keys.TEALIUM_EVENT_TYPE])
        assertSame("test", eventDispatch.payload()[Dispatch.Keys.TEALIUM_EVENT])
        assertSame(eventDispatch.id, eventDispatch.payload()[Dispatch.Keys.REQUEST_UUID])
    }
}