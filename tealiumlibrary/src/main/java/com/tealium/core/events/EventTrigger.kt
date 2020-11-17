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

        /**
         * Creates an EventTrigger that uses the value of [TEALIUM_EVENT] from the [Dispatch] to
         * start or stop a Timed Event.
         *
         * @param startEvent The event name that should trigger the Timed Event to be started
         * @param stopEvent The event name that should trigger the Timed Event to be stopped
         * @param data Optional - context data that will be added to the Dispatch when the Timed Event is stopped
         * @param eventName Optional - override the timed_event_name value sent when the Timed Event is stopped.
         * Default is "$startName::$stopName"
         */
        fun forEventName(startEvent: String, stopEvent: String, data: Map<String, Any>? = null, eventName: String? = null): EventTrigger {
            return EventNameTrigger(startEvent, stopEvent, data, eventName)
        }
    }
}