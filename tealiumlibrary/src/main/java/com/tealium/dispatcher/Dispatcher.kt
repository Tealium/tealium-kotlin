package com.tealium.dispatcher

import com.tealium.core.Module
import com.tealium.core.messaging.BatchDispatchSendListener
import com.tealium.core.messaging.DispatchSendListener

interface Dispatcher: Module, DispatchSendListener, BatchDispatchSendListener

interface DispatcherListener {
    fun successfulTrack()
    fun unsuccessfulTrack(message: String)
}

