package com.tealium.core.events.triggers

import com.tealium.core.events.EventTrigger
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.TealiumEvent
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EventTriggerTests {

    private lateinit var eventNameTrigger: EventNameTrigger

    @Before
    fun setUp() {
        eventNameTrigger = EventNameTrigger("start", "stop")
    }

    @Test
    fun eventNameTrigger_CheckInit() {
        assertEquals("start::stop", eventNameTrigger.eventName)
        assertEquals("start", eventNameTrigger.startName)
        assertEquals("stop", eventNameTrigger.stopName)
    }

    @Test
    fun eventNameTrigger_ShouldStart_WhenEventNameMatches() {
        val dispatch = TealiumEvent("start")
        assertTrue(eventNameTrigger.shouldStart(dispatch))
    }

    @Test
    fun eventNameTrigger_ShouldNotStart_WhenEventNameDoesNotMatch() {
        val dispatch = TealiumEvent("not-start")
        assertFalse(eventNameTrigger.shouldStart(dispatch))
    }

    @Test
    fun eventNameTrigger_ShouldNotStart_WhenNoTealiumEvent() {
        val dispatch = object : Dispatch {
            override val id: String
                get() = ""
            override var timestamp: Long? = 1000L

            override fun payload(): Map<String, Any> {
                return emptyMap()
            }

            override fun addAll(data: Map<String, Any>) {
                // do nothing
            }

            override fun get(key: String): Any? {
                return null
            }
        }
        assertFalse(eventNameTrigger.shouldStart(dispatch))
    }

    @Test
    fun eventNameTrigger_ShouldStop_WhenEventNameMatches() {
        val dispatch = TealiumEvent("stop")
        assertTrue(eventNameTrigger.shouldStop(dispatch))
    }

    @Test
    fun eventNameTrigger_ShouldNotStop_WhenEventNameDoesNotMatch() {
        val dispatch = TealiumEvent("not-stop")
        assertFalse(eventNameTrigger.shouldStop(dispatch))
    }

    @Test
    fun eventNameTrigger_ShouldNotStop_WhenNoTealiumEvent() {
        val dispatch = object : Dispatch {
            override val id: String
                get() = ""
            override var timestamp: Long? = 1000L

            override fun payload(): Map<String, Any> {
                return emptyMap()
            }

            override fun addAll(data: Map<String, Any>) {
                // do nothing
            }

            override fun get(key: String): Any? {
                return null
            }
        }
        assertFalse(eventNameTrigger.shouldStop(dispatch))
    }

    @Test
    fun eventTrigger_CompanionShouldReturnTrigger() {
        val trigger1 = EventTrigger.forEventName("start", "stop", eventName = "my_event")
        assertNotNull(trigger1)
        assertEquals("my_event", trigger1.eventName)

        val trigger2 = EventTrigger.forEventName("start", "stop", "my_event")
        assertNotNull(trigger2)
        assertEquals("my_event", trigger2.eventName)
    }
}