package com.tealium.core.messaging

import android.app.Activity
import com.tealium.core.Module
import com.tealium.core.Tealium
import com.tealium.core.consent.ConsentManagementPolicy
import com.tealium.core.consent.UserConsentPreferences
import com.tealium.core.persistence.DataLayer
import com.tealium.core.settings.LibrarySettings
import com.tealium.core.validation.DispatchValidator
import com.tealium.dispatcher.Dispatch
import com.tealium.remotecommands.RemoteCommandRequest
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean

interface EventRouter :
    DispatchReadyListener,
    DispatchSendListener,
    BatchDispatchSendListener,
    DispatchQueuedListener,
    DispatchDroppedListener,
    RemoteCommandListener,
    LibrarySettingsUpdatedListener,
    ActivityObserverListener,
    EvaluateJavascriptListener,
    ValidationChangedListener,
    NewSessionListener,
    SessionStartedListener,
    UserConsentPreferencesUpdatedListener,
    InstanceShutdownListener,
    VisitorIdUpdatedListener,
    DataLayer.DataLayerUpdatedListener,
    Subscribable {

    fun <T : Listener> send(messenger: Messenger<T>)

}

/**
 * The [EventDispatcher] is the default implementation for routing listener events to other
 * components in the SDK, or user-specified listeners.
 *
 * Use the isReady parameter to set the initial buffering behavior; false to buffer incoming events
 * and true to not buffer.
 * When opting to buffer events, use the [setReady] method to dequeue any buffered events in order
 * and also to stop the [EventDispatcher] from buffering future events.
 * The default behavior is not to buffer events in keeping with previous behavior.
 *
 * @param isReady sets whether or not this event router is ready to send events on creation
 * @param eventQueue the queue implementation for buffering events
 */
class EventDispatcher(
    isReady: Boolean = true,
    private val eventQueue: Queue<() -> Unit> = ConcurrentLinkedQueue()
) : EventRouter {

    private val listeners = CopyOnWriteArraySet<Listener>()
    private val _isReady: AtomicBoolean = AtomicBoolean(isReady)

    /**
     * Marks this [EventDispatcher] as ready to route events, and executes any queued events.
     *
     * Event buffering will no longer take place after calling this method.
     */
    fun setReady() {
        dequeue()

        _isReady.set(true)
    }

    /**
     * Executes all queued events in order, removing them from the queue.
     */
    private fun dequeue() {
        if (eventQueue.isNotEmpty()) {
            var event: (() -> Unit)? = eventQueue.poll()
            while (event != null) {
                event.invoke()
                event = eventQueue.poll()
            }
        }
    }

    /**
     * Executes the given [block] of code, or queues it if the router not marked as ready yet.
     */
    private fun onReady(block: () -> Unit) {
        if (!_isReady.get()) {
            eventQueue.add(block)
            return
        }

        dequeue()
        block.invoke()
    }

    override fun <T : Listener> send(messenger: Messenger<T>) {
        onReady {
            listeners.filterIsInstance(messenger.listenerClass.java).forEach {
                messenger.deliver(it)
            }
        }
    }

    override fun subscribe(listener: Listener) {
        if (listener == this) return

        listeners.add(listener)
    }

    fun subscribeAll(listenerList: List<Listener>) {
        val filtered = listenerList.filterNot { it == this }

        listeners.addAll(filtered)
    }

    override fun unsubscribe(listener: Listener) {
        listeners.remove(listener)
    }

    override fun onDispatchReady(dispatch: Dispatch) {
        onReady {
            listeners.forEach {
                when (it) {
                    is DispatchReadyListener -> it.onDispatchReady(dispatch)
                }
            }
        }
    }

    override suspend fun onDispatchSend(dispatch: Dispatch) {
        onReady {
            @Suppress("BlockingMethodInNonBlockingContext")
            runBlocking {
                // onDispatchSend suspend fun can cause events to arrive out
                // of order at Dispatchers - use runBlocking to make this synchronous
                // should address in next major release
                listeners.forEach {
                    when (it) {
                        is DispatchSendListener -> {
                            if (it !is Module || it.enabled) {
                                it.onDispatchSend(dispatch)
                            }
                        }
                    }
                }
            }
        }
    }

    override suspend fun onBatchDispatchSend(dispatches: List<Dispatch>) {
        onReady {
            @Suppress("BlockingMethodInNonBlockingContext")
            runBlocking {
                // onBatchDispatchSend suspend fun can cause events to arrive out
                // of order at Dispatchers - use runBlocking to make this synchronous
                // should address in next major release
                listeners.forEach {
                    when (it) {
                        is BatchDispatchSendListener -> {
                            if (it !is Module || it.enabled) {
                                it.onBatchDispatchSend(dispatches)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDispatchQueued(dispatch: Dispatch) {
        onReady {
            listeners.forEach {
                when (it) {
                    is DispatchQueuedListener -> it.onDispatchQueued(dispatch)
                }
            }
        }
    }

    override fun onDispatchDropped(dispatch: Dispatch) {
        onReady {
            listeners.forEach {
                when (it) {
                    is DispatchDroppedListener -> it.onDispatchDropped(dispatch)
                }
            }
        }
    }

    override fun onProcessRemoteCommand(dispatch: Dispatch) {
        onReady {
            listeners.forEach {
                when (it) {
                    is RemoteCommandListener -> it.onProcessRemoteCommand(dispatch)
                }
            }
        }
    }

    override fun onRemoteCommandSend(request: RemoteCommandRequest) {
        onReady {
            listeners.forEach {
                when (it) {
                    is RemoteCommandListener -> it.onRemoteCommandSend(request)
                }
            }
        }
    }

    override fun onLibrarySettingsUpdated(settings: LibrarySettings) {
        onReady {
            listeners.forEach {
                when (it) {
                    is LibrarySettingsUpdatedListener -> it.onLibrarySettingsUpdated(settings)
                }
            }
        }
    }

    override fun onActivityPaused(activity: Activity?) {
        onReady {
            listeners.forEach {
                when (it) {
                    is ActivityObserverListener -> it.onActivityPaused(activity)
                }
            }
        }
    }

    override fun onActivityResumed(activity: Activity?) {
        onReady {
            listeners.forEach {
                when (it) {
                    is ActivityObserverListener -> it.onActivityResumed(activity)
                }
            }
        }
    }

    override fun onActivityStopped(activity: Activity?, isChangingConfiguration: Boolean) {
        onReady {
            listeners.forEach {
                when (it) {
                    is ActivityObserverListener -> it.onActivityStopped(
                        activity,
                        isChangingConfiguration
                    )
                }
            }
        }
    }

    override fun onEvaluateJavascript(js: String) {
        onReady {
            listeners.forEach {
                when (it) {
                    is EvaluateJavascriptListener -> it.onEvaluateJavascript(js)
                }
            }
        }
    }

    override fun onRevalidate(override: Class<out DispatchValidator>?) {
        onReady {
            listeners.forEach {
                when (it) {
                    is ValidationChangedListener -> it.onRevalidate(override)
                }
            }
        }
    }

    override fun onNewSession(sessionId: Long) {
        onReady {
            listeners.forEach {
                when (it) {
                    is NewSessionListener -> it.onNewSession(sessionId)
                }
            }
        }
    }

    override fun onSessionStarted(sessionId: Long) {
        onReady {
            listeners.forEach {
                when (it) {
                    is SessionStartedListener -> it.onSessionStarted(sessionId)
                }
            }
        }
    }

    override fun onUserConsentPreferencesUpdated(
        userConsentPreferences: UserConsentPreferences,
        policy: ConsentManagementPolicy
    ) {
        onReady {
            listeners.forEach {
                when (it) {
                    is UserConsentPreferencesUpdatedListener -> it.onUserConsentPreferencesUpdated(
                        userConsentPreferences,
                        policy
                    )
                }
            }
        }
    }

    override fun onInstanceShutdown(name: String, instance: WeakReference<Tealium>) {
        onReady {
            listeners.forEach {
                when (it) {
                    is InstanceShutdownListener -> it.onInstanceShutdown(name, instance)
                }
            }
        }
    }

    override fun onVisitorIdUpdated(visitorId: String) {
        onReady {
            listeners.forEach {
                when (it) {
                    is VisitorIdUpdatedListener -> it.onVisitorIdUpdated(visitorId)
                }
            }
        }
    }

    override fun onDataUpdated(key: String, value: Any) {
        onReady {
            listeners.forEach {
                when (it) {
                    is DataLayer.DataLayerUpdatedListener -> it.onDataUpdated(key, value)
                }
            }
        }
    }

    override fun onDataRemoved(keys: Set<String>) {
        onReady {
            listeners.forEach {
                when (it) {
                    is DataLayer.DataLayerUpdatedListener -> it.onDataRemoved(keys)
                }
            }
        }
    }
}