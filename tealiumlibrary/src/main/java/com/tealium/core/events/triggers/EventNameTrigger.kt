package com.tealium.core.events.triggers

import com.tealium.core.events.EventTrigger
import com.tealium.dispatcher.Dispatch

internal class EventNameTrigger(val startName: String,
                                val stopName: String,
                                eventName: String? = null) : EventTrigger {

    override val eventName: String = eventName ?: "$startName::$stopName"

    override fun shouldStart(dispatch: Dispatch): Boolean {
        return dispatch[Dispatch.Keys.TEALIUM_EVENT]?.let {
            it == startName
        } ?: false
    }

    override fun shouldStop(dispatch: Dispatch): Boolean {
        return dispatch[Dispatch.Keys.TEALIUM_EVENT]?.let {
            it == stopName
        } ?: false
    }
}