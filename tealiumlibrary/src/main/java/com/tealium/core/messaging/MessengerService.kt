package com.tealium.core.messaging

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Facade around the EventRouter to allow users to subscribe to events and only send public ones.
class MessengerService(private val eventRouter: EventRouter,
                                private val background: CoroutineScope):
        Subscribable by eventRouter {

    fun <T: ExternalListener> send(messenger: Messenger<T>) {
        background.launch {
            eventRouter.send(messenger)
        }
    }
}