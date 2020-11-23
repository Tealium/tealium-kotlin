package com.tealium.core.events

interface TimedEvents {
    /**
     * Records the start time of a named Timed Event.
     *
     * Use [stopTimedEvent] to signal the completion of this Timed Event and trigger an event to
     * be sent via [Tealium.track][com.tealium.core.Tealium.track] or [cancelTimedEvent] to cancel
     * the event recording.
     *
     * If there is an existing Timed Event with the same [name] then it will not be restarted. You
     * can restart the Timed Event with the same [name] after you have cancelled it using
     * [cancelTimedEvent].
     *
     * @return The start time Timestamp for the event
     */
    fun startTimedEvent(name: String, data: Map<String, Any>?) : Long?

    /**
     * Manually records the end time of a named Timed Event.
     *
     * This will trigger an event to be sent via [Tealium.track][com.tealium.core.Tealium.track]
     * with context data for this event - event name, start time, stop time.
     *
     * If there is not an existing Timed Event with the given [name] then no event will be sent.
     *
     * @return The stop time Timestamp for the event
     */
    fun stopTimedEvent(name: String) : Long?

    /**
     * Cancels an existing Timed Event.
     */
    fun cancelTimedEvent(name: String)

    /**
     * Adds an Event Trigger to automatically start and stop timed events.
     */
    fun addEventTrigger(vararg trigger: EventTrigger)

    /**
     * Removes an existing Event Trigger, such that it no longer starts or stops timed events.
     * Any timed events started by a Trigger will not be cancelled upon removing the Event Trigger.
     * See [cancelTimedEvent]
     */
    fun removeEventTrigger(name: String)
}