package com.tealium.core

import android.net.Uri
import com.tealium.core.collection.ModuleCollector
import com.tealium.core.collection.SessionCollector
import com.tealium.core.collection.TealiumCollector
import com.tealium.core.consent.ConsentManager
import com.tealium.core.events.EventTrigger
import com.tealium.core.events.TimedEvents
import com.tealium.core.events.TimedEventsManager
import com.tealium.core.messaging.ActivityStatusChangedMessenger
import com.tealium.core.messaging.AfterDispatchSendCallbacks
import com.tealium.core.messaging.DispatchRouter
import com.tealium.core.messaging.DispatchSendCallbacks
import com.tealium.core.messaging.EventDispatcher
import com.tealium.core.messaging.EventRouter
import com.tealium.core.messaging.Listener
import com.tealium.core.messaging.MessengerService
import com.tealium.core.messaging.Subscribable
import com.tealium.core.network.Connectivity
import com.tealium.core.network.ConnectivityRetriever
import com.tealium.core.network.HttpClient
import com.tealium.core.network.NetworkClient
import com.tealium.core.persistence.DataLayer
import com.tealium.core.persistence.DatabaseHelper
import com.tealium.core.persistence.DefaultVisitorStorage
import com.tealium.core.persistence.DispatchStorage
import com.tealium.core.persistence.Expiry
import com.tealium.core.persistence.PersistentStorage
import com.tealium.core.settings.LibrarySettingsManager
import com.tealium.core.settings.LogLevelUpdateHandler
import com.tealium.core.validation.BatchingValidator
import com.tealium.core.validation.BatteryValidator
import com.tealium.core.validation.ConnectivityValidator
import com.tealium.core.validation.DispatchValidator
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.Dispatcher
import com.tealium.dispatcher.GenericDispatch
import com.tealium.tealiumlibrary.BuildConfig
import com.tealium.test.OpenForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @param key - uniquely identifies this Tealium instance.
 * @param config - the object that defines how to appropriately configure this instance.
 * @param onReady - completion block that signifies when this instance has completed finished initializing.
 */
@OpenForTesting
class Tealium private constructor(
    val key: String,
    val config: TealiumConfig,
    private val onReady: (Tealium.() -> Unit)? = null
) : TimedEvents {

    private val singleThreadedBackground =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
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

    private val networkClient: NetworkClient = HttpClient(config)

    // Only instantiates if there is an event triggered before the modules are initialized.
    private val dispatchBufferDelegate = lazy {
        LinkedList<Dispatch>()
    }
    private val dispatchBuffer: Queue<Dispatch> by dispatchBufferDelegate

    // Dependencies for publicly accessible objects.
    private val databaseHelper: DatabaseHelper = DatabaseHelper(config)
    private val eventRouter = EventDispatcher(isReady = false)
    private val activityManager: ActivityManager = ActivityManager.getInstance(config.application)
    private val activityListener: ActivityManager.ActivityLifecycleListener = createActivityListener(eventRouter)
    private val sessionManager = SessionManager(config, eventRouter)
    private val deepLinkHandler: DeepLinkHandler
    private val timedEvents: TimedEventsManager
    private val librarySettingsManager: LibrarySettingsManager = LibrarySettingsManager(
        config,
        networkClient,
        eventRouter = eventRouter,
        backgroundScope = backgroundScope
    )


    // Are publicly accessible, therefore need to be initialized on creation.
    /**
     * Provides access to the Tealium Logger system.
     */
    val logger: Logging = Logger

    private val _modules: MutableModuleManager = MutableModuleManager(emptyList())

    /**
     * Provides access to the different modules that are in use, either by name or by class.
     * Note. the modules themselves are initialized on a background thread, so to safely access them
     * once they are ready, use the [onReady] completion block.
     *
     * ```
     * Tealium.create("name", config) {
     *  modules.getModule(Xyz::class.java)?.doXyz()
     * }
     * ```
     */
    val modules: ModuleManager
        get() = _modules

    /**
     * Provides access to subscribe/unsubscribe to Tealium event listeners.
     */
    val events: Subscribable = MessengerService(eventRouter, backgroundScope)

    private val _dataLayer: PersistentStorage = PersistentStorage(
        databaseHelper,
        "datalayer",
        eventRouter = eventRouter,
        backgroundScope = backgroundScope,
        sessionId = sessionManager.currentSession.id,
    )

    /**
     * Persistent storage location for data that should appear on all subsequent [Dispatch] events.
     * Data will be collected from here and merged into each [Dispatch] along with any other defined
     * [Collector] data.
     */
    val dataLayer: DataLayer
        get() = _dataLayer

    /**
     * Object representing the current Tealium session in progress.
     */
    val session: Session
        get() = sessionManager.currentSession.copy()

    private val visitorIdProvider: VisitorIdProvider = VisitorIdProvider(
        config = config,
        visitorStorage = DefaultVisitorStorage(databaseHelper),
        dataLayer = dataLayer,
        onVisitorIdUpdated = {
            backgroundScope.launch {
                eventRouter.onVisitorIdUpdated(it)
            }
        }
    )

    /**
     * Unique string that identifies the user, used by other Tealium Services. This value is
     * generated upon first launch and remains in the [dataLayer] under the key "tealium_visitor_id"
     *
     * Subsequent launches will take this key from the [dataLayer] so amending it there will affect
     * attribution of events.
     */
    val visitorId: String
        get() = visitorIdProvider.currentVisitorId

    private val context = TealiumContext(
        config,
        visitorId,
        logger,
        dataLayer,
        networkClient,
        events as MessengerService,
        this
    )

    /**
     * Provides access for users to manage their consent preferences.
     * It is disabled by default; enabling will cause any events to be queued until a consent status
     * and/or opted-in category list is provided.
     */
    val consentManager: ConsentManager =
        ConsentManager(context, eventRouter, librarySettingsManager.librarySettings)

    init {
        migratePersistentData()
        if (sessionManager.isNewSessionOnLaunch) {
            _dataLayer.clearSessionData(session.id)
        }

        Logger.logLevel = config.logLevel
            ?: librarySettingsManager.initialSettings?.logLevel
                    ?: config.overrideDefaultLibrarySettings?.logLevel
                    ?: when (config.environment) {
                Environment.DEV -> LogLevel.DEV
                Environment.QA -> LogLevel.QA
                Environment.PROD -> LogLevel.PROD
            }

        // Subscribe user event listeners
        eventRouter.subscribeAll(config.events.toList())

        deepLinkHandler = DeepLinkHandler(context, backgroundScope)
        eventRouter.subscribeAll(
            listOf(
                Logger,
                sessionManager,
                deepLinkHandler,
                LogLevelUpdateHandler(config.logLevel)
            )
        )
        timedEvents = TimedEventsManager(context)

        config.visitorIdentityKey?.let {
            dataLayer.subscribe(visitorIdProvider)
        }

        activityManager.subscribe(activityListener)

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

        val dispatchCopy = GenericDispatch(dispatch)

        when (initialized.get()) {
            true -> {
                // needs to be done once we're fully initialised, else Session events might be missed
                // by any modules that listen. we should consider changing the implementation of [Dispatch]
                // to keep track of the time it was created, so we can use that.
                sessionManager.track(dispatchCopy)
                dispatchRouter.track(dispatchCopy)
            }

            false -> {
                logger.dev(BuildConfig.TAG, "Instance not yet initialized; buffering.")
                dispatchBuffer.add(dispatchCopy)
            }
        }
    }

    fun gatherTrackData(): Map<String, Any> {
        return runBlocking {
            dispatchRouter.collect()
        }
    }

    @Suppress("unused")
    fun sendQueuedDispatches() {
        if (librarySettingsManager.librarySettings.disableLibrary) {
            Logger.qa(BuildConfig.TAG, "Library is disabled. Cannot dispatch queued events.")
            return
        }

        backgroundScope.launch {
            dispatchRouter.sendQueuedEvents()
        }
    }

    /**
     * Sets up all non-critical objects - should be called on a background thread.
     * Reports as ready once it's completed by calling [onInstanceReady].
     */
    private fun bootstrap() {
        connectivity = ConnectivityRetriever.getInstance(config.application)
        dispatchStore = DispatchStorage(databaseHelper, "dispatches")

        dispatchSendCallbacks = DispatchSendCallbacks(eventRouter) // required by dispatchers.

        collectors =
            mutableSetOf(TealiumCollector(context), SessionCollector(session.id), dataLayer).union(
                initializeCollectors(config.collectors)
            )
        validators = initializeValidators(config.validators)
        dispatchers = initializeDispatchers(config.dispatchers)
        val genericModules =
            setOf(consentManager, timedEvents).union(initializeModules(config.modules))

        val modulesList = collectors.union(validators)
            .union(dispatchers)
            .union(genericModules)
            .toList()
        val moduleCollector = ModuleCollector(modulesList)

        modulesList.filterIsInstance<Listener>().let {
            eventRouter.subscribeAll(it)
        }
        modulesList.forEach {
            _modules.add(it)
        }

        dispatchRouter = DispatchRouter(
            singleThreadedBackground,
            modules.getModulesForType(Collector::class.java).union(setOf(moduleCollector)),
            modules.getModulesForType(Transformer::class.java),
            modules.getModulesForType(DispatchValidator::class.java),
            dispatchStore,
            librarySettingsManager,
            eventRouter
        )
        eventRouter.subscribeAll(listOf(dispatchRouter, dispatchStore))

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
            BatchingValidator(dispatchStore, librarySettingsManager.librarySettings) {
                backgroundScope.launch {
                    delay(1000)
                    dispatchRouter.sendQueuedEvents()
                }
            }
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
     * Marks this instance as ready. Should be called once all features/modules are completely ready.
     * It will call the [onReady] completion block if one was supplied, and then finally it will also
     * debuffer any [Dispatch]es that might have been made whilst this instance was initializing.
     */
    private fun onInstanceReady() {
        eventRouter.setReady()

        initialized.set(true)
        onReady?.invoke(this)

        logger.qa(
            BuildConfig.TAG,
            "Tealium instance initialized with the following modules: $modules"
        )

        if (dispatchBufferDelegate.isInitialized() && dispatchBuffer.size > 0) {
            logger.dev(BuildConfig.TAG, "Dispatching buffered events.")

            while (!dispatchBuffer.isEmpty()) {
                dispatchBuffer.poll()?.let { queuedDispatch ->
                    track(queuedDispatch)
                }
            }
        }
    }

    /**
     * Removes current and regenerates a new visitor ID.
     *
     * [consentManager] Consent Status is unaffected by this method; consider whether you may need
     * reset the consent status also.
     */
    fun resetVisitorId() {
        backgroundScope.launch {
            visitorIdProvider.resetVisitorId()
        }
    }

    /**
     * Removes all known Visitor Ids for this device.
     *
     */
    fun clearStoredVisitorIds() {
        backgroundScope.launch {
            visitorIdProvider.clearStoredVisitorIds()
        }
    }

    /**
     * Adds the supplied Trace ID to the data layer for the current session.
     */
    @Suppress("unused")
    fun joinTrace(id: String) {
        backgroundScope.launch {
            deepLinkHandler.joinTrace(id)
        }
    }

    /**
     * Removes the Trace ID from the data layer if present
     */
    @Suppress("unused")
    fun leaveTrace() {
        backgroundScope.launch {
            deepLinkHandler.leaveTrace()
        }
    }

    /**
     * Kills the visitor session remotely to test end of session events (does not terminate the SDK session
     * or reset the session ID).
     */
    @Suppress("unused")
    fun killTraceVisitorSession() {
        backgroundScope.launch {
            deepLinkHandler.killTraceVisitorSession()
        }
    }

    /**
     * Handles a deep link Uri.The link and the query parameters are added to the data layer
     * for the current session.
     */
    @Suppress("unused")
    fun handleDeepLink(uri: Uri) {
        backgroundScope.launch {
            deepLinkHandler.handleDeepLink(uri)
        }
    }

    override fun startTimedEvent(name: String, data: Map<String, Any>?): Long? =
        timedEvents.startTimedEvent(name, data)

    override fun stopTimedEvent(name: String): Long? = timedEvents.stopTimedEvent(name)

    override fun cancelTimedEvent(name: String) = timedEvents.cancelTimedEvent(name)

    override fun addEventTrigger(vararg trigger: EventTrigger) =
        timedEvents.addEventTrigger(*trigger)

    override fun removeEventTrigger(name: String) = timedEvents.removeEventTrigger(name)

    /**
     * Migrates persistent data from the Tealium Android (Java) library if present
     * */
    private fun migratePersistentData() {
        val hashCode = (config.accountName + '.' +
                config.profileName + '.' +
                config.environment.environment).hashCode()
        val legacySharedPreferences = config.application.getSharedPreferences(
            "tealium.datasources.${
                Integer.toHexString(hashCode)
            }", 0
        )
        if (legacySharedPreferences.all.isEmpty()) {
            return
        }
        legacySharedPreferences.all.forEach { item ->
            val key = item.key
            val value = item.value

            when (value) {
                is String -> dataLayer.putString(key, value, Expiry.FOREVER)
                is Boolean -> dataLayer.putBoolean(key, value, Expiry.FOREVER)
                is Float -> dataLayer.putDouble(key, value.toDouble(), Expiry.FOREVER)
                is Double -> dataLayer.putDouble(key, value, Expiry.FOREVER)
                is Int -> dataLayer.putInt(key, value, Expiry.FOREVER)
                is Long -> dataLayer.putLong(key, value, Expiry.FOREVER)
                is Set<*> -> {
                    val list = value.filterIsInstance<String>()
                    dataLayer.putStringArray(key, list.toTypedArray(), Expiry.FOREVER)
                }
            }
        }
        legacySharedPreferences.edit().clear().apply()
    }

    /**
     * Returns this instance to an uninitialized state and triggers the shutdown handler for any
     * modules that need to take action when shutting down.
     */
    private fun shutdown() {
        initialized.set(false)
        activityManager.unsubscribe(activityListener)
        eventRouter.onInstanceShutdown(key, WeakReference(this))
    }

    companion object {
        private val instances = mutableMapOf<String, Tealium>()

        /**
         * Creates a Tealium instance, and stores it for future use retrievable using the provided
         * [name].
         */
        fun create(
            name: String,
            config: TealiumConfig,
            onReady: (Tealium.() -> Unit)? = null
        ): Tealium {
            val instance = Tealium(name, config, onReady)
            instances[name] = instance
            return instance
        }

        /**
         * Removes the instance stored at the given [name].
         */
        fun destroy(name: String) {
            instances[name]?.shutdown()
            instances.remove(name)
        }

        /**
         * Returns the [Tealium] instance stored at the given [name], otherwise null.
         */
        operator fun get(name: String): Tealium? {
            return instances[name]
        }

        /**
         * Returns the names of each of the stored [Tealium] instances.
         */
        fun names(): Set<String> {
            return instances.keys.toSet()
        }

        private fun createActivityListener(eventRouter: EventRouter): ActivityManager.ActivityLifecycleListener {
            return ActivityManager.ActivityLifecycleListener {
                eventRouter.send(ActivityStatusChangedMessenger(it))
            }
        }
    }
}
