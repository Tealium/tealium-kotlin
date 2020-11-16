package com.tealium.core.events

import com.tealium.core.events.triggers.EventNameTrigger
import com.tealium.dispatcher.Dispatch

interface EventTrigger {

    /**
     * The event name to send when the Trigger signals the event should stop timing.
     */
    val eventName: String

    /**
     * Optional additional event data to add to the payload when the Trigger signals the event
     * should stop timing.
     */
    val data: Map<String, Any>?

    /**
     * Signals that the timer should begin for this named event.
     *
     * @return true if the timer should begin, otherwise false
     */
    fun shouldStart(dispatch: Dispatch): Boolean

    /**
     * Signals that the timer should end for this named event.
     *
     * @return true if the timer should end, otherwise false
     */
    fun shouldStop(dispatch: Dispatch): Boolean

    companion object {
        fun forEventName(eventName: String, startEvent: String, stopEvent: String, data: Map<String, Any>? = null): EventTrigger {
            return EventNameTrigger(eventName, startEvent, stopEvent, data)
        }
    }
}