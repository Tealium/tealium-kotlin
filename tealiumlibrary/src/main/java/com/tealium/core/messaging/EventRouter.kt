package com.tealium.core.messaging

import android.app.Activity
import com.tealium.core.Module
import com.tealium.core.Tealium
import com.tealium.core.consent.ConsentManagementPolicy
import com.tealium.core.consent.UserConsentPreferences
import com.tealium.core.settings.LibrarySettings
import com.tealium.core.validation.DispatchValidator
import com.tealium.dispatcher.Dispatch
import com.tealium.remotecommands.RemoteCommandRequest
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArraySet

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
        Subscribable {

    fun <T : Listener> send(messenger: Messenger<T>)

}

class EventDispatcher : EventRouter {

    private val listeners = CopyOnWriteArraySet<Listener>()

    override fun <T : Listener> send(messenger: Messenger<T>) {
        listeners.filterIsInstance(messenger.listenerClass.java).forEach {
            messenger.deliver(it)
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
        listeners.forEach {
            when (it) {
                is DispatchReadyListener -> it.onDispatchReady(dispatch)
            }
        }
    }

    override suspend fun onDispatchSend(dispatch: Dispatch) {
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

    override suspend fun onBatchDispatchSend(dispatches: List<Dispatch>) {
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

    override fun onDispatchQueued(dispatch: Dispatch) {
        listeners.forEach {
            when (it) {
                is DispatchQueuedListener -> it.onDispatchQueued(dispatch)
            }
        }
    }

    override fun onDispatchDropped(dispatch: Dispatch) {
        listeners.forEach {
            when (it) {
                is DispatchDroppedListener -> it.onDispatchDropped(dispatch)
            }
        }
    }

    override fun onProcessRemoteCommand(dispatch: Dispatch) {
        listeners.forEach {
            when (it) {
                is RemoteCommandListener -> it.onProcessRemoteCommand(dispatch)
            }
        }
    }

    override fun onRemoteCommandSend(request: RemoteCommandRequest) {
        listeners.forEach {
            when (it) {
                is RemoteCommandListener -> it.onRemoteCommandSend(request)
            }
        }
    }

    override fun onLibrarySettingsUpdated(settings: LibrarySettings) {
        listeners.forEach {
            when (it) {
                is LibrarySettingsUpdatedListener -> it.onLibrarySettingsUpdated(settings)
            }
        }
    }

    override fun onActivityPaused(activity: Activity?) {
        listeners.forEach {
            when (it) {
                is ActivityObserverListener -> it.onActivityPaused(activity)
            }
        }
    }

    override fun onActivityResumed(activity: Activity?) {
        listeners.forEach {
            when (it) {
                is ActivityObserverListener -> it.onActivityResumed(activity)
            }
        }
    }

    override fun onActivityStopped(activity: Activity?, isChangingConfiguration: Boolean) {
        listeners.forEach {
            when (it) {
                is ActivityObserverListener -> it.onActivityStopped(activity, isChangingConfiguration)
            }
        }
    }

    override fun onEvaluateJavascript(js: String) {
        listeners.forEach {
            when (it) {
                is EvaluateJavascriptListener -> it.onEvaluateJavascript(js)
            }
        }
    }

    override fun onRevalidate(override: Class<out DispatchValidator>?) {
        listeners.forEach {
            when (it) {
                is ValidationChangedListener -> it.onRevalidate(override)
            }
        }
    }

    override fun onNewSession(sessionId: Long) {
        listeners.forEach {
            when (it) {
                is NewSessionListener -> it.onNewSession(sessionId)
            }
        }
    }

    override fun onSessionStarted(sessionId: Long) {
        listeners.forEach {
            when (it) {
                is SessionStartedListener -> it.onSessionStarted(sessionId)
            }
        }
    }

    override fun onUserConsentPreferencesUpdated(userConsentPreferences: UserConsentPreferences, policy: ConsentManagementPolicy) {
        listeners.forEach {
            when (it) {
                is UserConsentPreferencesUpdatedListener -> it.onUserConsentPreferencesUpdated(userConsentPreferences, policy)
            }
        }
    }

    override fun onInstanceShutdown(name: String, instance: WeakReference<Tealium>) {
        listeners.forEach {
            when (it) {
                is InstanceShutdownListener -> it.onInstanceShutdown(name, instance)
            }
        }
    }
}