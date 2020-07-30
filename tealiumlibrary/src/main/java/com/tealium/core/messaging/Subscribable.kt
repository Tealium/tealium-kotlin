package com.tealium.core.messaging

interface Subscribable {
    fun subscribe(listener: Listener)
    fun unsubscribe(listener: Listener)
}
