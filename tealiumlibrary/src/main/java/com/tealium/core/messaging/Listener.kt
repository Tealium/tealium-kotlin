package com.tealium.core.messaging

import android.app.Activity
import com.tealium.core.consent.ConsentManagementPolicy
import com.tealium.core.consent.UserConsentPreferences
import com.tealium.core.settings.LibrarySettings
import com.tealium.core.validation.DispatchValidator
import com.tealium.dispatcher.Dispatch
import java.util.*

/**
 * Top level interface for all events that pass through the SDK.
 */
interface Listener : EventListener

/**
 * Restricted listener interface for all events that can be sent into the SDK from outside the core.
 */
interface ExternalListener : Listener

/**
 * Executes after the Dispatch has been populated, and after all [DispatchValidator.shouldDrop] have
 * been executed, but before all [DispatchValidator.shouldQueue]
 */
interface DispatchReadyListener : Listener {
    fun onDispatchReady(dispatch: Dispatch)
}

/**
 * Executes once it is safe to send from the device according to any [DispatchValidator]s
 */
interface DispatchSendListener : Listener {
    suspend fun onDispatchSend(dispatch: Dispatch)
}

/**
 * Executes once it is safe to send queued events, as a batch, from the device according to any
 * [DispatchValidator]s
 */
interface BatchDispatchSendListener : Listener {
    suspend fun onBatchDispatchSend(dispatches: List<Dispatch>)
}

/**
 * Executes when any [DispatchValidator] signifies that this dispatch should be queued.
 */
interface DispatchQueuedListener : Listener {
    fun onDispatchQueued(dispatch: Dispatch)
}

/**
 * Executes when any [DispatchValidator] signifies that this dispatch should be dropped.
 */
interface DispatchDroppedListener : Listener {
    fun onDispatchDropped(dispatch: Dispatch)
}

/**
 * Executes when a Remote Command execution is required from an external module.
 */
interface RemoteCommandListener : Listener {
    fun onRemoteCommandSend(url: String)
}

/**
 * Executes when new Library Settings have been received.
 */
interface LibrarySettingsUpdatedListener : Listener {
    fun onLibrarySettingsUpdated(settings: LibrarySettings)
}

/**
 * Executes when Activities have been paused and resume; largely analogous to
 * [android.app.Application.ActivityLifecycleCallbacks.onActivityPaused] and
 * [android.app.Application.ActivityLifecycleCallbacks.onActivityResumed] respectively
 */
interface ActivityObserverListener : ExternalListener {
    fun onActivityPaused(activity: Activity?)
    fun onActivityResumed(activity: Activity?)
    fun onActivityStopped(isChangingConfiguration: Boolean, activity: Activity?)
}

/**
 * Executes the given Javascript on the TagManagement WebView.
 */
interface EvaluateJavascriptListener : Listener {
    fun onEvaluateJavascript(js: String)
}

interface ValidationChangedListener : ExternalListener {

    /**
     * Indicates that a validation criterion has changed and it's now worth revalidating.
     * For example, a connectivity validator may have requested queueing due to lack of network
     * connectivity, and now that connectivity is restored it is safe to dispatch any queued events.
     *
     * The override parameter is available to ignore the result of any dispatch validator of that
     * class. Typically it would be expected that the calling class is used. For cases such as
     * batching events, where the underlying criterion won't change, but it is safe to ignore the
     * result and send the batch partially filled.
     *
     * @param override a DispatchValidator class to override the result of, or null
     */
    fun onRevalidate(override: Class<out DispatchValidator>?)
}

interface NewSessionListener : Listener {
    /**
     * Notifies of the generation of a new Session as a result of it being the first session or that an
     * existing session has expired.
     */
    fun onNewSession(sessionId: Long)
}

interface SessionStartedListener : Listener {
    /**
     * Notifies that the session has now started.
     */
    fun onSessionStarted(sessionId: Long)
}

/**
 * Is executed whenever there is a change in User Consent Preferences, with the current set of
 * preferences and the policy that is currently in force.
 */
interface UserConsentPreferencesUpdatedListener: Listener {

    fun onUserConsentPreferencesUpdated(userConsentPreferences: UserConsentPreferences, policy: ConsentManagementPolicy)
}