package com.tealium.core

import com.tealium.core.collection.SessionCollector
import com.tealium.core.collection.TealiumCollector
import com.tealium.core.consent.ConsentManager
import com.tealium.core.messaging.*
import com.tealium.core.network.Connectivity
import com.tealium.core.network.ConnectivityRetriever
import com.tealium.core.network.HttpClient
import com.tealium.core.network.NetworkClient
import com.tealium.core.persistence.*
import com.tealium.core.persistence.DatabaseHelper
import com.tealium.core.persistence.DispatchStorage
import com.tealium.core.persistence.PersistentStorage
import com.tealium.core.settings.LibrarySettingsManager
import com.tealium.core.validation.BatchingValidator
import com.tealium.core.validation.BatteryValidator
import com.tealium.core.validation.ConnectivityValidator
import com.tealium.core.validation.DispatchValidator
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.Dispatcher
import com.tealium.tealiumlibrary.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList

/**
 * @param key - uniquely identifies this Tealium instance.
 * @param config - the object that defines how to appropriately configure this instance.
 * @param onReady - completion block that signifies when this instance has completed finished initializing.
 */
class Tealium @JvmOverloads constructor(val key: String, val config: TealiumConfig, private val onReady: (Tealium.() -> Unit)? = null) {

    private val singleThreadedBackground = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val backgroundScope = CoroutineScope(singleThreadedBackground)
    private val initialized = AtomicBoolean(false)

    // Can be initialised late to improved startup performance
    private lateinit var collectors: Set<Collector>
    private lateinit var validators: Set<DispatchValidator>
    private lateinit var dispatchers: Set<Dispatcher>
    private lateinit var dispatchStore: DispatchStorage
    private lateinit var connectivity: Connectivity
    private lateinit var dispatchRouter: DispatchRouter
    private lateinit var dispatchSendCallbacks: AfterDispatchSendCallbacks
    private lateinit var context: TealiumContext

    private val networkClient: NetworkClient = HttpClient(config)
    private val librarySettingsManager: LibrarySettingsManager
    private val activityObserver: ActivityObserver
    // Only instantiates if there is an event triggered before the modules are initialized.
    private val dispatchBufferDelegate = lazy {
        InMemoryPersistence()
    }
    private val dispatchBuffer: Persistence by dispatchBufferDelegate

    // Dependencies for publicly accessible objects.
    private val databaseHelper: DatabaseHelper
    private val eventRouter = EventDispatcher()
    private val sessionManager = SessionManager(config, eventRouter)
    private lateinit var deepLinkHandler: DeepLinkHandler

    // Are publicly accessible, therefore need to be initialized on creation.
    /**
     * Provides access to the Tealium Logger system.
     */
    val logger: Logging

    /**
     * Provides access to the different modules that are in use, either by name or by class.
     * Note. the modules themselves are initialized on a background thread, so to safely access them
     * once they are ready, use the [onReady] completion block.
     *
     * ```
     * Tealium("name", config) {
     *  modules.getModule(Xyz::class.java)?.doXyz()
     * }
     * ```
     */
    lateinit var modules: ModuleManager
        private set

    /**
     * Provides access to subscribe/unsubscribe to Tealium event listeners.
     */
    val events: Subscribable = MessengerService(eventRouter, backgroundScope)

    /**
     * Persistent storage location for data that should appear on all subsequent [Dispatch] events.
     * Data will be collected from here and merged into each [Dispatch] along with any other defined
     * [Collector] data.
     */
    val dataLayer: DataLayer

    /**
     * Object representing the current Tealium session in progress.
     */
    val session: Session = sessionManager.currentSession

    /**
     * Unique string that identifies the user, used by other Tealium Services. This value is
     * generated upon first launch and remains in the [dataLayer] under the key "tealium_visitor_id"
     *
     * Subsequent launches will take this key from the [dataLayer] so amending it there will affect
     * attribution of events.
     */
    val visitorId: String

    /**
     * Provides access for users to manage their consent preferences.
     * It is disabled by default; enabling will cause any events to be queued until a consent status
     * and/or opted-in category list is provided.
     */
    val consentManager: ConsentManager

    init {
        librarySettingsManager = LibrarySettingsManager(config, networkClient, eventRouter = eventRouter, backgroundScope = backgroundScope)
        activityObserver = ActivityObserver(config, eventRouter)
        databaseHelper = DatabaseHelper(config)
        dataLayer = PersistentStorage(databaseHelper, "datalayer")
        migratePersistentData()
        visitorId = getOrCreateVisitorId()
        consentManager = ConsentManager(config, eventRouter, visitorId, librarySettingsManager.librarySettings)

        Logger.logLevel = when (config.environment) {
            Environment.DEV -> LogLevel.DEV
            Environment.QA -> LogLevel.QA
            Environment.PROD -> LogLevel.PROD
        }
        logger = Logger
        eventRouter.subscribe(Logger)
        eventRouter.subscribe(sessionManager)
        // Initialize everything else in the background.
        backgroundScope.launch {
            bootstrap()
        }
    }

    /**
     * Sends a [Dispatch]. It will have it's data merged with any [Collector]s that you have enabled.
     * Any [DispatchValidator]s will control whether or not this [Dispatch] should be queued or
     * dropped. And finally it will be distributed to all [Dispatcher]s that you have registered.
     *
     * As the [modules] are initialized on a background thread, calls to this method will be buffered
     * in-memory until the system is ready to send them.
     */
    fun track(dispatch: Dispatch) {
        if (librarySettingsManager.librarySettings.disableLibrary) {
            Logger.qa(BuildConfig.TAG, "Library is disabled. Cannot track new events.")
            return
        }

        if (dispatch.timestamp == null) {
            dispatch.timestamp = System.currentTimeMillis()
        }

        when (initialized.get()) {
            true -> {
                // needs to be done once we're fully initialised, else Session events might be missed
                // by any modules that listen. we should consider changing the implementation of [Dispatch]
                // to keep track of the time it was created, so we can use that.
                sessionManager.track(dispatch)
                dispatchRouter.track(dispatch)
            }
            false -> {
                logger.dev(BuildConfig.TAG, "Instance not yet initialized; buffering.")
                dispatchBuffer.enqueue(dispatch)
            }
        }
    }
    @Suppress("unused")
    fun sendQueuedDispatches() {
        if (librarySettingsManager.librarySettings.disableLibrary) {
            Logger.qa(BuildConfig.TAG, "Library is disabled. Cannot dispatch queued events.")
            return
        }

        backgroundScope.launch {
            eventRouter.onRevalidate(BatchingValidator::class.java)
        }
    }

    /**
     * Sets up all non-critical objects - should be called on a background thread.
     * Reports as ready once it's completed by calling [onInstanceReady].
     */
    private fun bootstrap() {
        connectivity = ConnectivityRetriever(config.application)
        dispatchStore = DispatchStorage(databaseHelper, "dispatches")

        dispatchSendCallbacks = DispatchSendCallbacks(eventRouter) // required by dispatchers.

        context = TealiumContext(config, visitorId, logger, dataLayer, networkClient, events, this)
        collectors = mutableSetOf(TealiumCollector(context), SessionCollector(session.id), dataLayer).union(initializeCollectors(config.collectors))
        validators = initializeValidators(config.validators)
        dispatchers = initializeDispatchers(config.dispatchers)
        val genericModules = setOf(consentManager).union(initializeModules(config.modules))

        val modulesList = collectors.union(validators)
                .union(dispatchers)
                .union(genericModules)
                .toList()

        modulesList.filterIsInstance<Listener>().forEach {
            eventRouter.subscribe(it)
        }
        modules = ModuleManager(modulesList)

        dispatchRouter = DispatchRouter(singleThreadedBackground,
                modules.getModulesForType(Collector::class.java),
                modules.getModulesForType(Transformer::class.java),
                modules.getModulesForType(DispatchValidator::class.java),
                dispatchStore,
                librarySettingsManager,
                eventRouter)
        eventRouter.subscribe(dispatchRouter)
        eventRouter.subscribe(dispatchStore)
        deepLinkHandler = DeepLinkHandler(context)
        eventRouter.subscribe(deepLinkHandler)
        onInstanceReady()
    }

    /**
     * Turns CollectorFactories into their implementations by supplying the necessary parameters.
     */
    private fun initializeCollectors(collectorFactories: Set<CollectorFactory>): Set<Collector> {
        return collectorFactories.map { factory ->
            factory.create(context)
        }.toSet()
    }

    /**
     * Instantiates the built in validators and joins them with any supplied custom validators.
     */
    private fun initializeValidators(customValidators: Set<DispatchValidator>): Set<DispatchValidator> {
        customValidators.forEach { it.enabled = true }
        return setOf<DispatchValidator>(
                BatteryValidator(config, librarySettingsManager.librarySettings, events),
                ConnectivityValidator(connectivity, librarySettingsManager.librarySettings),
                BatchingValidator(dispatchStore, librarySettingsManager.librarySettings, eventRouter)
        ).union(customValidators)
    }

    /**
     * Turns DispatcherFactories into their implementations by supplying the necessary parameters.
     */
    private fun initializeDispatchers(dispatcherFactories: Set<DispatcherFactory>): Set<Dispatcher> {
        return dispatcherFactories.map { factory ->
            factory.create(context, dispatchSendCallbacks)
        }.toSet()
    }

    /**
     * Turns ModuleFactories into their implementations by supplying the necessary parameters.
     */
    private fun initializeModules(moduleFactories: Set<ModuleFactory>): Set<Module> {
        return moduleFactories.map { factory ->
            factory.create(context)
        }.toSet()
    }

    /**
     * Retrieves the visitorId from the datalayer if it exists, otherwise it will generate and store
     * a replacement.
     */
    private fun getOrCreateVisitorId(): String {
        return dataLayer.getString("tealium_visitor_id")
                ?: UUID.randomUUID().toString().replace("-", "").also {
                    dataLayer.putString("tealium_visitor_id", it, Expiry.FOREVER)
                }
    }

    /**
     * Marks this instance as ready. Should be called once all features/modules are completely ready.
     * It will call the [onReady] completion block if one was supplied, and then finally it will also
     * debuffer any [Dispatch]es that might have been made whilst this instance was initializing.
     */
    private fun onInstanceReady() {
        initialized.set(true)
        onReady?.invoke(this)

        logger.qa(BuildConfig.TAG, "Tealium instance initialized with the following modules: $modules")

        if (dispatchBufferDelegate.isInitialized() && dispatchBuffer.count > 0) {
            logger.dev(BuildConfig.TAG, "Dispatching buffered events.")

            dispatchBuffer.dequeue().forEach { queuedDispatch ->
                track(queuedDispatch)
            }
        }
    }

    /**
     * Adds the supplied Trace ID to the data layer for the current session.
     */
    @Suppress("unused")
    fun joinTrace(id: String) {
        deepLinkHandler.joinTrace(id)
    }

    /**
     * Removes the Trace ID from the data layer if present
     */
    @Suppress("unused")
    fun leaveTrace() {
        deepLinkHandler.leaveTrace()
    }


    /**
     * Kills the visitor session remotely to test end of session events (does not terminate the SDK session
     * or reset the session ID).
     */
    @Suppress("unused")
    fun killTraceVisitorSession() {
        deepLinkHandler.killTraceVisitorSession()
    }

    /**
     * Migrates persistent data from the Tealium Android (Java) library if present
     * */
    private fun migratePersistentData() {
        val hashCode = (config.accountName + '.' +
                    config.profileName + '.' +
                    config.environment.environment).hashCode()
        val legacySharedPreferences = config.application.getSharedPreferences("tealium.datasources.${Integer.toHexString(hashCode)}", 0)
        if (legacySharedPreferences.all.isEmpty()) {
            return
        }
        legacySharedPreferences.all.forEach { item ->
            val key = item.key
            val value = item.value

            (value as? String)?.let {
                dataLayer.putString(key, it, Expiry.FOREVER)
            }
            (value as? Boolean)?.let {
                dataLayer.putBoolean(key, it, Expiry.FOREVER)
            }?:
            (value as? Double)?.let {
                dataLayer.putDouble(key, it, Expiry.FOREVER)
            }?:
            (value as? Int)?.let {
                dataLayer.putInt(key, it, Expiry.FOREVER)
            }?:
            (value as? Long)?.let {
                dataLayer.putLong(key, it, Expiry.FOREVER)
            }?:
            if (value is Set<*>) {
                value.firstOrNull()?.let {
                    when(it) {
                        is String -> {
                            val stringArray: MutableList<String> = ArrayList<String>()
                            value.forEach { anyValue ->
                                anyValue?.let { stringValue ->
                                    if (stringValue is String) {
                                        stringArray.add(stringValue)
                                    }
                                }
                            }
                            dataLayer.putStringArray(key, stringArray.toTypedArray())
                        }
                    }
                }
            }
        }
        legacySharedPreferences.edit().clear().apply()
    }
}
