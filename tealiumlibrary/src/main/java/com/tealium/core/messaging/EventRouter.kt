package com.tealium.core.messaging

import com.tealium.core.consent.ConsentManagementPolicy
import com.tealium.core.consent.UserConsentPreferences
import com.tealium.core.model.LibrarySettings
import com.tealium.core.validation.DispatchValidator
import com.tealium.dispatcher.Dispatch

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
        Subscribable {

    fun <T : Listener> send(messenger: Messenger<T>)

}

class EventDispatcher : EventRouter {

    private val listeners = mutableSetOf<Listener>()

    override fun <T : Listener> send(messenger: Messenger<T>) {
        listeners.filterIsInstance(messenger.listenerClass.java).forEach {
            messenger.deliver(it)
        }
    }

    override fun subscribe(listener: Listener) {
        if (listener == this) return

        listeners.add(listener)
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
                is DispatchSendListener -> it.onDispatchSend(dispatch)
            }
        }
    }

    override suspend fun onBatchDispatchSend(dispatches: List<Dispatch>) {
        listeners.forEach {
            when (it) {
                is BatchDispatchSendListener -> it.onBatchDispatchSend(dispatches)
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

    override fun onRemoteCommandSend(url: String) {
        listeners.forEach {
            when (it) {
                is RemoteCommandListener -> it.onRemoteCommandSend(url)
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

    override fun onActivityPaused() {
        listeners.forEach {
            when (it) {
                is ActivityObserverListener -> it.onActivityPaused()
            }
        }
    }

    override fun onActivityResumed() {
        listeners.forEach {
            when (it) {
                is ActivityObserverListener -> it.onActivityResumed()
            }
        }
    }

    override fun onActivityStopped(isChangingConfiguration: Boolean) {
        listeners.forEach {
            when (it) {
                is ActivityObserverListener -> it.onActivityStopped(isChangingConfiguration)
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
}