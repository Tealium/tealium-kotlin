package com.tealium.core.events

import com.tealium.core.LogLevel
import com.tealium.core.Logger
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.TealiumEvent
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TimedEventsManagerTests {

    @MockK
    lateinit var mockContext: TealiumContext

    @MockK
    lateinit var mockConfig: TealiumConfig

    internal lateinit var timedEventsManager: TimedEventsManager

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { mockContext.config } returns mockConfig
        every { mockContext.track(any()) } just Runs
        every { mockConfig.timedEventTriggers } returns mutableListOf()

        Logger.logLevel = LogLevel.SILENT

        timedEventsManager = TimedEventsManager(mockContext)
    }

    @Test
    fun config_TriggersGetAddedOnInit() {
        val trigger1 = createMockTrigger("trigger_1", true, false)
        val trigger2 = createMockTrigger("trigger_2", false, true)
        every { mockConfig.timedEventTriggers } returns mutableListOf(trigger1, trigger2)

        timedEventsManager = TimedEventsManager(mockContext)

        val foundTrigger1 = timedEventsManager.triggers.find { it.eventName == trigger1.eventName }!!
        val foundTrigger2 = timedEventsManager.triggers.find { it.eventName == trigger2.eventName }!!

        assertSame(trigger1, foundTrigger1)
        assertSame(trigger2, foundTrigger2)
    }

    @Test
    fun startTimedEvent_Starts_WhenNotAlreadyStarted() {
        val timestamp = timedEventsManager.startTimedEvent("test", null)

        val event = timedEventsManager.timedEvents.find { it.eventName == "test" }!!
        assertNotNull(event)
        assertEquals("test", event?.eventName)
        assertTrue(event.startTime == timestamp)
    }

    @Test
    fun startTimedEvent_DoesNothing_WhenAlreadyStarted() {
        val timestamp = timedEventsManager.startTimedEvent("test", null)
        val original = timedEventsManager.timedEvents.find { it.eventName == "test" }?.copy()!!

        timedEventsManager.startTimedEvent("test", null)
        val event = timedEventsManager.timedEvents.find { it.eventName == "test" }?.copy()!!
        assertEquals(original.eventName, event.eventName)
        assertEquals(timestamp, original.startTime)
        assertEquals(timestamp, event.startTime)
    }

    @Test
    fun stopTimedEvent_StopsSendsAndCancels_WhenAlreadyStarted() {
        val startTime = timedEventsManager.startTimedEvent("test", null)
        val stopTime = timedEventsManager.stopTimedEvent("test")

        assertNull(timedEventsManager.timedEvents.find { it.eventName == "test" })
        verify {
            mockContext.track(match {
                it[Dispatch.Keys.TEALIUM_EVENT] == TimedEvent.TIMED_EVENT_NAME
                        && it[Dispatch.Keys.TIMED_EVENT_NAME] == "test"
                        && it[Dispatch.Keys.TIMED_EVENT_START] == startTime
                        && it[Dispatch.Keys.TIMED_EVENT_END] == stopTime
            })
        }
    }

    @Test
    fun stopTimedEvent_AddDataToDispatch() {
        timedEventsManager.startTimedEvent("test", mapOf("extra" to "data"))
        timedEventsManager.stopTimedEvent("test")

        assertNull(timedEventsManager.timedEvents.find { it.eventName == "test" })
        verify {
            mockContext.track(match {
                it["extra"] == "data"
            })
        }
    }

    @Test
    fun stopTimedEvent_DoesNothing_WhenNotAlreadyStarted() {
        timedEventsManager.stopTimedEvent("test")

        verify(exactly = 0) {
            mockContext.track(any())
        }
    }

    @Test
    fun cancelTimedEvent_RemovesEvent() {
        timedEventsManager.startTimedEvent("test", null)
        assertNotNull(timedEventsManager.timedEvents.find { it.eventName == "test" })
        timedEventsManager.cancelTimedEvent("test")
        assertNull(timedEventsManager.timedEvents.find { it.eventName == "test" })

        // No event should be cancelled
        verify(exactly = 0) {
            mockContext.track(any())
        }
    }

    @Test
    fun addEventTrigger_AddsTrigger_WhenNotExists() {
        val trigger: EventTrigger = mockk(relaxed = true)
        every { trigger.eventName } returns "trigger_name"
        timedEventsManager.addEventTrigger(trigger)

        val foundTrigger = timedEventsManager.triggers.find { it.eventName == "trigger_name" }
        assertNotNull(foundTrigger)
        assertSame(trigger, foundTrigger)
    }

    @Test
    fun addEventTrigger_AddsMultiple() {
        val trigger1: EventTrigger = mockk(relaxed = true)
        every { trigger1.eventName } returns "trigger_name_1"
        val trigger2: EventTrigger = mockk(relaxed = true)
        every { trigger2.eventName } returns "trigger_name_2"
        timedEventsManager.addEventTrigger(trigger1, trigger2)

        val foundTrigger1 = timedEventsManager.triggers.find { it.eventName == "trigger_name_1" }
        val foundTrigger2 = timedEventsManager.triggers.find { it.eventName == "trigger_name_2" }
        assertNotNull(foundTrigger1)
        assertNotNull(foundTrigger2)
        assertSame(trigger1, foundTrigger1)
        assertSame(trigger2, foundTrigger2)
    }

    @Test
    fun addEventTrigger_DoesNothing_WhenAlreadyExists() {
        val trigger: EventTrigger = mockk(relaxed = true)
        every { trigger.eventName } returns "trigger_name"
        timedEventsManager.addEventTrigger(trigger)

        val newTrigger: EventTrigger = mockk(relaxed = true)
        every { newTrigger.eventName } returns "trigger_name"
        timedEventsManager.addEventTrigger(newTrigger)

        val foundTrigger = timedEventsManager.triggers.find { it.eventName == "trigger_name" }
        assertNotNull(foundTrigger)
        assertSame(trigger, foundTrigger)
    }

    @Test
    fun removeEventTrigger_RemovesTrigger() {
        val trigger: EventTrigger = mockk(relaxed = true)
        every { trigger.eventName } returns "trigger_name"
        timedEventsManager.addEventTrigger(trigger)

        var foundTrigger = timedEventsManager.triggers.find { it.eventName == "trigger_name" }
        assertNotNull(foundTrigger)

        timedEventsManager.removeEventTrigger(trigger.eventName)
        foundTrigger = timedEventsManager.triggers.find { it.eventName == "trigger_name" }
        assertNull(foundTrigger)
    }

    @Test
    fun onDispatchReady_DoesNothing_WhenNoTriggers() = runBlocking {
        val dispatch: Dispatch = TealiumEvent("test")
        timedEventsManager.transform(dispatch)

        val payload = dispatch.payload()
        assertFalse(payload.containsKey(Dispatch.Keys.TIMED_EVENT_NAME))
        assertFalse(payload.containsKey(Dispatch.Keys.TIMED_EVENT_START))
        assertFalse(payload.containsKey(Dispatch.Keys.TIMED_EVENT_END))
        assertFalse(payload.containsKey(Dispatch.Keys.TIMED_EVENT_DURATION))
    }

    @Test
    fun transform_ChecksTriggers_AndStartsTimedEvent() = runBlocking {
        val startTrigger: EventTrigger = createMockTrigger("test_trigger", true, false)

        assertNull(timedEventsManager.triggers.find { it.eventName == startTrigger.eventName })
        assertNull(timedEventsManager.timedEvents.find { it.eventName == startTrigger.eventName })

        timedEventsManager.addEventTrigger(startTrigger)
        assertNotNull(timedEventsManager.triggers.find { it.eventName == startTrigger.eventName })

        val dispatch: Dispatch = TealiumEvent("test")
        timedEventsManager.transform(dispatch)

        val event = timedEventsManager.timedEvents.find { it.eventName == startTrigger.eventName }
        assertNotNull(event)
        assertEquals(startTrigger.eventName, event?.eventName)
        assertNotNull(event?.startTime)
    }

    @Test
    fun transform_StartTime_UsesDispatchTimestamp() = runBlocking {
        val startTrigger: EventTrigger = createMockTrigger("test_trigger", true, false)
        timedEventsManager.addEventTrigger(startTrigger)

        val dispatch: Dispatch = TealiumEvent("test").apply { timestamp = 1000L }
        timedEventsManager.transform(dispatch)

        val event = timedEventsManager.timedEvents.find { it.eventName == startTrigger.eventName }!!
        assertEquals(1000L, event.startTime)
    }

    @Test
    fun transform_StopTime_UsesDispatchTimestamp() = runBlocking {
        val stopTrigger: EventTrigger = createMockTrigger("test_trigger", false, true)
        timedEventsManager.addEventTrigger(stopTrigger)
        val startTime = timedEventsManager.startTimedEvent(stopTrigger.eventName, null)!!

        val dispatch: Dispatch = TealiumEvent("test").apply { timestamp = startTime + 1000L }
        // fetch event before stopping
        val event = timedEventsManager.timedEvents.find { it.eventName == stopTrigger.eventName }!!
        timedEventsManager.transform(dispatch)

        assertEquals(startTime + 1000L, event.stopTime)
    }

    @Test
    fun transform_ChecksTriggers_AndStopsTimedEvent() = runBlocking {
        val stopTrigger: EventTrigger = createMockTrigger("test_trigger", false, true)
        timedEventsManager.addEventTrigger(stopTrigger)
        val startTime = timedEventsManager.startTimedEvent("test_trigger", null)!!

        val dispatch: Dispatch = TealiumEvent("test").apply { timestamp = startTime + 1000L }
        // fetch event before stopping
        val event = timedEventsManager.timedEvents.find { it.eventName == stopTrigger.eventName }!!
        timedEventsManager.transform(dispatch)

        verify {
            mockContext.track(match {
                it[Dispatch.Keys.TEALIUM_EVENT] == TimedEvent.TIMED_EVENT_NAME
                        && it[Dispatch.Keys.TIMED_EVENT_NAME] == "test_trigger"
                        && it[Dispatch.Keys.TIMED_EVENT_START] == startTime
                        && it[Dispatch.Keys.TIMED_EVENT_END] == startTime + 1000L
                        && it[Dispatch.Keys.TIMED_EVENT_DURATION] == 1000L
            })
        }
    }

    @Test
    fun transform_ChecksTriggers_AndStopsMultipleTimedEvent() = runBlocking {
        val stopTrigger1: EventTrigger = createMockTrigger("test_trigger_1", false, true)
        val stopTrigger2: EventTrigger = createMockTrigger("test_trigger_2", false, true)
        timedEventsManager.addEventTrigger(stopTrigger1)
        timedEventsManager.addEventTrigger(stopTrigger2)
        val startTime1 = timedEventsManager.startTimedEvent("test_trigger_1", null)!!
        val startTime2 = timedEventsManager.startTimedEvent("test_trigger_2", null)!!

        val dispatch: Dispatch = TealiumEvent("test").apply { timestamp = startTime1 + 1000L }
        // fetch event before stopping
        val event1 = timedEventsManager.timedEvents.find { it.eventName == stopTrigger1.eventName }!!
        val event2 = timedEventsManager.timedEvents.find { it.eventName == stopTrigger2.eventName }!!
        timedEventsManager.transform(dispatch)

        verify {
            mockContext.track(match {
                it[Dispatch.Keys.TEALIUM_EVENT] == TimedEvent.TIMED_EVENT_NAME
                        && it[Dispatch.Keys.TIMED_EVENT_NAME] == "test_trigger_1"
                        && it[Dispatch.Keys.TIMED_EVENT_START] == event1.startTime
                        && it[Dispatch.Keys.TIMED_EVENT_END] == event1.stopTime
                        && it[Dispatch.Keys.TIMED_EVENT_DURATION] == event1.duration
            })
            mockContext.track(match {
                it[Dispatch.Keys.TEALIUM_EVENT] == TimedEvent.TIMED_EVENT_NAME
                        && it[Dispatch.Keys.TIMED_EVENT_NAME] == "test_trigger_2"
                        && it[Dispatch.Keys.TIMED_EVENT_START] == event2.startTime
                        && it[Dispatch.Keys.TIMED_EVENT_END] == event2.stopTime
                        && it[Dispatch.Keys.TIMED_EVENT_DURATION] == event2.duration
            })
        }
    }

    /**
     * Utility for mocking Triggers.
     */
    fun createMockTrigger(eventName: String, shouldStart: Boolean, shouldStop: Boolean): EventTrigger {
        val startTrigger: EventTrigger = mockk()
        every { startTrigger.eventName } returns eventName
        every { startTrigger.shouldStart(any()) } returns shouldStart
        every { startTrigger.shouldStop(any()) } returns shouldStop
        return startTrigger
    }
}