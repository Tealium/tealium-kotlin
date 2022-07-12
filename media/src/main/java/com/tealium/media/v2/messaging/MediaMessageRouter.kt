package com.tealium.media.v2.messaging

import com.tealium.core.messaging.Listener
import com.tealium.media.v2.MediaSessionEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet

interface MediaMessageRouter: MediaSessionEvents {
    fun <T : MediaListener> send(messenger: MediaMessenger<T>)
}

class MediaMessageDispatcher(
    private val backgroundScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    initialListeners: Set<MediaListener> = emptySet(),
): MediaMessageRouter, MediaSessionEvents {

    private val listeners = CopyOnWriteArraySet<Listener>().apply {
        addAll(initialListeners)
    }

    override fun <T : MediaListener> send(messenger: MediaMessenger<T>) {
        backgroundScope.launch {
            listeners.filterIsInstance(messenger.listenerClass.java).forEach {
                messenger.deliver(it)
            }
        }
    }

    override fun subscribe(mediaListener: MediaListener) {
        listeners.add(mediaListener)
    }

    override fun unsubscribe(mediaListener: MediaListener) {
        listeners.remove(mediaListener)
    }
}