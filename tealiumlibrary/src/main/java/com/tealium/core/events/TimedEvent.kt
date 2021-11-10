package com.tealium.core.events

import com.tealium.core.Logger
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.TealiumEvent
import com.tealium.tealiumlibrary.BuildConfig

data class TimedEvent(
        val eventName: String,
        val startTime: Long,
        val data: Map<String, Any>? = null) {

    /**
     * Returns the end time of this event.
     */
    var stopTime: Long? = null

    /**
     * Returns the duration of this timed event, or null if it has not ended.
     */
    val duration: Long?
        get() = stopTime?.let { it - startTime }

    companion object {
        const val TIMED_EVENT_NAME = "timed_event"
        @Deprecated(
            "Constant has been moved.",
            ReplaceWith("Dispatch.Keys.TIMED_EVENT_NAME", "com.tealium.dispatcher.Dispatch")
        )
        const val KEY_TIMED_EVENT_NAME = "timed_event_name"

        @Deprecated(
            "Constant has been moved.",
            ReplaceWith("Dispatch.Keys.TIMED_EVENT_START", "com.tealium.dispatcher.Dispatch")
        )
        const val KEY_TIMED_EVENT_START = "timed_event_start"

        @Deprecated(
            "Constant has been moved.",
            ReplaceWith("Dispatch.Keys.TIMED_EVENT_END", "com.tealium.dispatcher.Dispatch")
        )
        const val KEY_TIMED_EVENT_END = "timed_event_end"

        @Deprecated(
            "Constant has been moved.",
            ReplaceWith("Dispatch.Keys.TIMED_EVENT_DURATION", "com.tealium.dispatcher.Dispatch")
        )
        const val KEY_TIMED_EVENT_DURATION = "timed_event_duration"

        private fun isValid(timedEvent: TimedEvent): Boolean {
            return when {
                timedEvent.stopTime == null || timedEvent.duration == null -> {
                    Logger.dev(BuildConfig.TAG, "Missing required data on TimedEvent($timedEvent)")
                    false
                }
                else -> true
            }
        }

        fun toDispatch(timedEvent: TimedEvent): Dispatch? {
            if (!isValid(timedEvent)) return null

            return TealiumEvent(TIMED_EVENT_NAME, toMap(timedEvent))
        }

        fun toMap(timedEvent: TimedEvent): Map<String, Any>? {
            return when (isValid(timedEvent)) {
                false -> null
                true -> {
                    val stopTime = timedEvent.stopTime ?: return null
                    val duration = timedEvent.duration ?: return null
                    
                    mutableMapOf<String, Any>(
                        Dispatch.Keys.TIMED_EVENT_NAME to timedEvent.eventName,
                        Dispatch.Keys.TIMED_EVENT_START to timedEvent.startTime,
                        Dispatch.Keys.TIMED_EVENT_END to stopTime,
                        Dispatch.Keys.TIMED_EVENT_DURATION to duration
                    ).also {
                        if (timedEvent.data != null) it.putAll(timedEvent.data)
                    }
                }
            }
        }
    }
}

