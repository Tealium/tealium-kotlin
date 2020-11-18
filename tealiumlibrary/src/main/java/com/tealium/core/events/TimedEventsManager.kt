package com.tealium.core.events

import com.tealium.core.Logger
import com.tealium.core.TealiumContext
import com.tealium.core.Transformer
import com.tealium.dispatcher.Dispatch
import com.tealium.tealiumlibrary.BuildConfig

internal class TimedEventsManager(private val context: TealiumContext) : TimedEvents,
        Transformer {

    override val name: String = "TIMED_EVENTS"
    override var enabled: Boolean = true

    val timestamp: Long
        get() = System.currentTimeMillis()

    private val _triggers: MutableList<EventTrigger> = mutableListOf()

    val triggers: List<EventTrigger>
        get() = _triggers

    private val _timedEvents: MutableMap<String, TimedEvent> = mutableMapOf()

    val timedEvents: List<TimedEvent>
        get() = _timedEvents.values.toList()

    init {
        if (context.config.timedEventTriggers.count() > 0) {
            addEventTrigger(*context.config.timedEventTriggers.toTypedArray())
        }
    }

    override fun startTimedEvent(name: String, data: Map<String, Any>?): Long? {
        return startTimedEvent(name, timestamp, data = data)
    }

    private fun startTimedEvent(name: String, timestamp: Long, data: Map<String, Any>?): Long? {
        if (_timedEvents.contains(name)) {
            Logger.dev(BuildConfig.TAG, "TimedEvent (name) is already started; ignoring.")
            return null
        }

        _timedEvents[name] = TimedEvent(name,
                timestamp,
                data).also {
            Logger.dev(BuildConfig.TAG, "TimedEvent started: $it")
        }

        return timestamp
    }

    override fun stopTimedEvent(name: String): Long? {
        return stopTimedEvent(name, timestamp)?.let {
            sendTimedEvent(it)
            it.stopTime
        }
    }

    private fun stopTimedEvent(name: String, timestamp: Long): TimedEvent? {
        return _timedEvents[name]?.also { event ->
            cancelTimedEvent(name)
            event.stopTime = timestamp
            Logger.dev(BuildConfig.TAG, "TimedEvent stopped: $event")
        }
    }

    override fun cancelTimedEvent(name: String) {
        _timedEvents.remove(name)
    }

    override fun addEventTrigger(vararg trigger: EventTrigger) {
        trigger.forEach {
            _triggers.add(it)
        }
    }

    override fun removeEventTrigger(name: String) {
        _triggers.indexOfFirst { it.eventName == name }.let {
            if (it > -1) _triggers.removeAt(it)
        }
    }

    private fun sendTimedEvent(timedEvent: TimedEvent) {
        TimedEvent.toDispatch(timedEvent)?.let {
            Logger.dev(BuildConfig.TAG, "Sending Timed Event($timedEvent)")
            context.track(it)
        }
    }

    override suspend fun transform(dispatch: Dispatch) {
        if (triggers.isNotEmpty()) {
            Logger.dev(BuildConfig.TAG, "Checking Timed Event Triggers.")
            triggers.forEach { trigger ->
                _timedEvents[trigger.eventName]?.let { event ->
                    // timed event has been started, so check if it should be stopped
                    if (trigger.shouldStop(dispatch)) {
                        stopTimedEvent(event.eventName, dispatch.timestamp ?: timestamp)
                        TimedEvent.toMap(event)?.let {
                            dispatch.addAll(it)
                        }
                    }
                } ?: run {
                    // timed event not started yet, so check if it should be started
                    if (trigger.shouldStart(dispatch)) {
                        startTimedEvent(trigger.eventName, dispatch.timestamp
                                ?: timestamp, null)
                    }
                }
            }
        }
    }
}