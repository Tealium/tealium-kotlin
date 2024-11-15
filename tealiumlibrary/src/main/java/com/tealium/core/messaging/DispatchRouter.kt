package com.tealium.core.messaging

import com.tealium.core.Collector
import com.tealium.core.settings.LibrarySettingsManager
import com.tealium.core.Logger
import com.tealium.core.Transformer
import com.tealium.core.consent.ConsentManagementPolicy
import com.tealium.core.consent.UserConsentPreferences
import com.tealium.core.persistence.QueueingDao
import com.tealium.core.validation.BatchingValidator
import com.tealium.core.validation.BatteryValidator
import com.tealium.core.validation.ConnectivityValidator
import com.tealium.core.validation.DispatchValidator
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.GenericDispatch
import com.tealium.tealiumlibrary.BuildConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.ceil

internal class DispatchRouter(
    coroutineDispatcher: CoroutineDispatcher,
    private val collectors: Set<Collector>,
    private val transformers: Set<Transformer>,
    private val validators: Set<DispatchValidator>,
    private val dispatchStore: QueueingDao<String, Dispatch>,
    private val librarySettingsManager: LibrarySettingsManager,
    private val eventRouter: EventRouter
) : ValidationChangedListener,
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
            transform(dispatch)

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
            val queueResult = shouldQueue(dispatch)

            if (queueResult.shouldProcessRemoteCommand) {
                processRemoteCommand(dispatch)
            } else {
                dispatch.addAll(mapOf(TEALIUM_RC_PROCESSED to false))
            }

            if (queueResult.shouldQueue) {
                dispatch.addAll(mapOf(Dispatch.Keys.WAS_QUEUED to true))
                dispatchStore.enqueue(dispatch)
                scope.launch(Logger.exceptionHandler) {
                    eventRouter.onDispatchQueued(dispatch)
                }
                return@launch
            }

            // Dispatch Send
            batchedDequeue(dispatch)
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
     *  Calls the [Transformer.transform] method of each [Transformer] to enrich the [Dispatch]
     */
    suspend fun transform(dispatch: Dispatch) {
        transformers.filter { it.enabled }.forEach {
            try {
                it.transform(dispatch)
            } catch (ex: Exception) {
                Logger.dev(BuildConfig.TAG, "Failed to transform data from ${it.name}")
            }
        }
    }

    /**
     * Checks all [DispatchValidator] instances for whether this Dispatch should be queued.
     * @return true if dispatch should be queued; else false
     */
    fun shouldQueue(
        dispatch: Dispatch?,
        override: Class<out DispatchValidator>? = null
    ): QueueResult {
        val initialResult = QueueResult(false, true)
        return validators.filter { it.enabled }
            .fold(initialResult) { input, validator ->
                val shouldQueue = shouldQueue(dispatch, validator, override)
                if (shouldQueue) {
                    Logger.qa(
                        BuildConfig.TAG,
                        "Queueing dispatch requested by: ${validator.name}"
                    )
                }
                val remoteCommandProcessingAllowed =
                    !shouldQueue || remoteCommandProcessingAllowed(validator)

                input.copy(
                    shouldQueue = input.shouldQueue || shouldQueue,
                    shouldProcessRemoteCommand = input.shouldProcessRemoteCommand && remoteCommandProcessingAllowed
                )
            }
    }

    private fun shouldQueue(
        dispatch: Dispatch?,
        validator: DispatchValidator,
        override: Class<out DispatchValidator>?
    ): Boolean =
        if (override != null && override.isInstance(validator)) {
            false
        } else validator.shouldQueue(dispatch)

    private fun remoteCommandProcessingAllowed(validator: DispatchValidator): Boolean =
        validator.javaClass == BatchingValidator::class.java
                || validator.javaClass == ConnectivityValidator::class.java
                || validator.javaClass == BatteryValidator::class.java

    /**
     * Checks all [DispatchValidator] instances for whether this Dispatch should be dropped.
     * @return true if dispatch should be dropped; else false
     */
    fun shouldDrop(dispatch: Dispatch): Boolean {
        return validators.filter { it.enabled }.fold(false) { input, validator ->
            input || validator.shouldDrop(dispatch).also { dropping ->
                if (dropping) Logger.qa(
                    BuildConfig.TAG,
                    "Dropping dispatch requested by: ${validator.name}"
                )
            }
        }
    }

    /**
     * Pops all currently queued items off the queue, and adds the [dispatch] if one is supplied.
     */
    @Deprecated("Use [batchedDequeue]")
    fun dequeue(dispatch: Dispatch?): List<Dispatch> {
        // check if remote commands should be triggered, only on incoming dispatch
        processRemoteCommand(dispatch)
        var queue = dispatchStore.dequeue(-1)
        dispatch?.let {
            queue = queue.plus(it)
        }

        return queue
    }

    /**
     * Pops all currently queued items off the queue in batches
     */
    fun batchedDequeue(dispatch: Dispatch?) {
        val queueSize = dispatchStore.count()
        if (queueSize == 0) {
            dispatch?.let {
                sendDispatches(listOf(it))
            }
            return
        }

        val batchSize = settings.batching.batchSize
        val batches = ceil(queueSize.toDouble() / batchSize).toInt()
        Logger.dev(
            BuildConfig.TAG,
            "Sending ${queueSize + if (dispatch != null) 1 else 0} events in batches of $batchSize"
        )

        var batchNo = 0
        var batch: List<Dispatch> = dispatchStore.dequeue(batchSize)
        while (batch.isNotEmpty()) {
            batchNo++

            if (batchNo >= batches && dispatch != null) {
                batch = batch.plus(dispatch)
            }
            sendDispatches(batch)

            batch = dispatchStore.dequeue(batchSize)
        }
    }

    /**
     * Handles the business logic for sending dispatches. Breaks a list into batches, or sends
     * individually where appropriate.
     */
    fun sendDispatches(dispatches: List<Dispatch>) {
        val rcDispatches = dispatches.filter { it[TEALIUM_RC_PROCESSED] == false }
        dispatches.forEach { dispatch ->
            dispatch.remove(TEALIUM_RC_PROCESSED)
        }

        scope.launch(Logger.exceptionHandler) {
            rcDispatches.forEach(::processRemoteCommand)
        }
        scope.launch(Logger.exceptionHandler) {
            val batchSize = settings.batching.batchSize
            dispatches.chunked(batchSize).forEach { batch ->
                if (batch.count() == 1) {
                    eventRouter.onDispatchSend(batch.first())
                } else {
                    eventRouter.onBatchDispatchSend(batch)
                }
            }
            librarySettingsManager.fetchLibrarySettings()
        }
    }



    /**
     * Sends the [dispatch] for processing of RemoteCommands.
     */
    private fun processRemoteCommand(dispatch: Dispatch?) {
        if (dispatch == null) return
        try {
            eventRouter.onProcessRemoteCommand(dispatch)
        } catch (e: Exception) {
            Logger.dev(BuildConfig.TAG, "Error processing dispatch for RemoteCommands: ${e.message}")
        }
    }

    /**
     * Executes each [DispatchValidator.shouldQueue] method again to see if it is safe to send any
     * queued dispatches. If so, then they will be dequeued and dispatched.
     */
    override fun onRevalidate(override: Class<out DispatchValidator>?) {
        Logger.dev(BuildConfig.TAG, "Revalidation requested.")

        if (!shouldQueue(null, override).shouldQueue) {
            batchedDequeue(null)
        }
    }

    fun sendQueuedEvents() {
        onRevalidate(BatchingValidator::class.java)
    }

    /**
     * Checks the [policy] for whether or not to purge any queued [Dispatch] instances.
     */
    override fun onUserConsentPreferencesUpdated(
        userConsentPreferences: UserConsentPreferences,
        policy: ConsentManagementPolicy
    ) {
        if (policy.shouldDrop()) {
            dispatchStore.clear()
        }

        if (dispatchStore.count() > 0 && !policy.shouldQueue()) {
            onRevalidate(null)
        }
    }

    internal data class QueueResult(
        val shouldQueue: Boolean,
        val shouldProcessRemoteCommand: Boolean
    )

    companion object {
        const val TEALIUM_RC_PROCESSED = "tealium_remote_command_processed"
    }
}