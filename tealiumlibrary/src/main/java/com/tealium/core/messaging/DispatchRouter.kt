package com.tealium.core.messaging

import com.tealium.core.Collector
import com.tealium.core.settings.LibrarySettingsManager
import com.tealium.core.Logger
import com.tealium.core.consent.ConsentManagementPolicy
import com.tealium.core.consent.ConsentManager
import com.tealium.core.consent.ConsentStatus
import com.tealium.core.consent.UserConsentPreferences
import com.tealium.core.network.Connectivity
import com.tealium.core.persistence.DispatchStorage
import com.tealium.core.validation.DispatchValidator
import com.tealium.dispatcher.Dispatch
import com.tealium.tealiumlibrary.BuildConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class DispatchRouter(coroutineDispatcher: CoroutineDispatcher,
                              private val collectors: Set<Collector>,
                              private val validators: Set<DispatchValidator>,
                              private val dispatchStore: DispatchStorage,
                              private val librarySettingsManager: LibrarySettingsManager,
                              private val connectivity: Connectivity,
                              private val consentManager: ConsentManager,
                              private val eventRouter: EventRouter)
    : ValidationChangedListener,
        UserConsentPreferencesUpdatedListener {

    private val scope = CoroutineScope(coroutineDispatcher)
    private val settings
        get() = librarySettingsManager.librarySettings

    /**
     * Entry point for all dispatch tracking. Contains business logic for passing dispatches through
     * the system, and informs any [Listener] instances registered with the [EventRouter]
     */
    fun track(dispatch: Dispatch) {
        if (settings.disableLibrary) {
            return
        }

        scope.launch(Logger.exceptionHandler) {
            // Collection
            dispatch.addAll(collect())

            // Validation - Drop
            if (shouldDrop(dispatch)) {
                scope.launch(Logger.exceptionHandler) {
                    eventRouter.onDispatchDropped(dispatch)
                }
                return@launch
            }

            // Dispatch Ready
            scope.launch(Logger.exceptionHandler) {
                eventRouter.onDispatchReady(dispatch)
            }

            // Validation - Queue
            if (shouldQueue(dispatch)) {
                dispatchStore.enqueue(dispatch)
                scope.launch(Logger.exceptionHandler) {
                    eventRouter.onDispatchQueued(dispatch)
                }
                return@launch
            }
            // Dispatch Send
            val queue = dequeue(dispatch)
            sendDispatches(queue)
        }
    }

    /**
     * @return Map of data from [Collector] instances
     */
    suspend fun collect(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()
        collectors.filter { it.enabled }.forEach {
            try {
                data.putAll(it.collect())
            } catch (ex: Exception) {
                Logger.dev(BuildConfig.TAG, "Failed to collect data from ${it.name}")
            }
        }
        return data
    }

    /**
     * Checks all [DispatchValidator] instances for whether this Dispatch should be queued.
     * @return true if dispatch should be queued; else false
     */
    fun shouldQueue(dispatch: Dispatch?, override: Class<out DispatchValidator>? = null): Boolean {
        return validators.filter { it.enabled }.fold(false) { input, validator ->
            input ||
                    if (override != null && override.isInstance(validator)) {
                        false
                    } else validator.shouldQueue(dispatch).also { queueing ->
                        if (queueing) {
                            Logger.qa(BuildConfig.TAG, "Queueing dispatch requested by: ${validator.name}")
                            if (validator.name == "BATCHING_VALIDATOR") {
                                attemptSendRemoteCommand(dispatch)
                            }
                        }
                    }
        }
    }

    /**
     * Checks all [DispatchValidator] instances for whether this Dispatch should be dropped.
     * @return true if dispatch should be dropped; else false
     */
    fun shouldDrop(dispatch: Dispatch): Boolean {
        return validators.filter { it.enabled }.fold(false) { input, validator ->
            input || validator.shouldDrop(dispatch).also { dropping ->
                if (dropping) Logger.qa(BuildConfig.TAG, "Dropping dispatch requested by: ${validator.name}")
            }
        }
    }

    /**
     * Pops all currently queued items off the queue, and adds the [dispatch] if one is supplied.
     */
    fun dequeue(dispatch: Dispatch?): List<Dispatch> {
        // check if remote commands should be triggered, only on incoming dispatch
        attemptSendRemoteCommand(dispatch)
        var queue = dispatchStore.dequeue(-1)
        dispatch?.let {
            queue = queue.plus(it)
        }

        return queue
    }

    /**
     * Handles the business logic for sending dispatches. Breaks a list into batches, or sends
     * individually where appropriate.
     */
    fun sendDispatches(dispatches: List<Dispatch>) {
        scope.launch(Logger.exceptionHandler) {
            when {
                dispatches.count() == 1 -> eventRouter.onDispatchSend(dispatches.first())
                dispatches.count() > 1 -> {
                    settings.batching.batchSize.let { batchSize ->
                        when {
                            batchSize > 1 -> {
                                dispatches.chunked(batchSize).forEach { batch ->
                                    eventRouter.onBatchDispatchSend(batch)
                                }
                            }
                            else -> {
                                dispatches.forEach { dispatch ->
                                    eventRouter.onDispatchSend(dispatch)
                                }
                            }
                        }
                    }
                }
            }
            librarySettingsManager.fetchLibrarySettings()
        }
    }

    /**
     * If batching is enabled, attempt to send remote commands. If Consent Manager
     * is enabled, verify consent is granted. If disabled, check connectivity.
     */
    private fun attemptSendRemoteCommand(dispatch: Dispatch?) {
        if (settings.batching.batchSize > 1) {
            dispatch?.let {
                when (consentManager.enabled) {
                    true -> {
                        if (consentManager.userConsentStatus == ConsentStatus.CONSENTED && connectivity.isConnected()) {
                            eventRouter.onProcessRemoteCommand(it)
                        }
                    }
                    false -> {
                        if (connectivity.isConnected()) {
                            eventRouter.onProcessRemoteCommand(it)
                        }
                    }
                }
            }
        }
    }

    /**
     * Executes each [DispatchValidator.shouldQueue] method again to see if it is safe to send any
     * queued dispatches. If so, then they will be dequeued and dispatched.
     */
    override fun onRevalidate(override: Class<out DispatchValidator>?) {
        Logger.dev(BuildConfig.TAG, "Revalidation requested.")

        if (!shouldQueue(null, override)) {
            val dispatches = dequeue(null)
            sendDispatches(dispatches)

            if (dispatches.count() > 1) {
                dispatches.forEach {
                    attemptSendRemoteCommand(it)
                }
            }
        }
    }

    /**
     * Checks the [policy] for whether or not to purge any queued [Dispatch] instances.
     */
    override fun onUserConsentPreferencesUpdated(userConsentPreferences: UserConsentPreferences, policy: ConsentManagementPolicy) {
        if (policy.shouldDrop()) {
            dispatchStore.clear()
        }

        if (dispatchStore.count() > 0 && !policy.shouldQueue()) {
            onRevalidate(null)
        }
    }
}