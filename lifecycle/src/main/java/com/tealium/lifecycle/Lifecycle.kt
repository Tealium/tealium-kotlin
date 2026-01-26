package com.tealium.lifecycle

import android.app.Activity
import android.content.pm.PackageInfo
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.tealium.core.*
import com.tealium.core.messaging.ActivityObserverListener
import com.tealium.dispatcher.TealiumEvent
import java.lang.UnsupportedOperationException

/**
 * The Lifecycle module sends events relating to the overall lifecycle of the application - Launch,
 * Wake, Sleep.
 */
class Lifecycle(private val context: TealiumContext) : Collector, ActivityObserverListener {

    private val config: TealiumConfig = context.config
    var isAutoTracking = config.isAutoTrackingEnabled ?: true

    private var lastResume: Long? = null
    private var lastPause: Long? = null
    private var handler: Handler = Handler(Looper.getMainLooper())

    internal var lifecycleSharedPreferences = LifecycleSharedPreferences(config)
    internal var lifecycleService = LifecycleService(lifecycleSharedPreferences)

    /**
     * Gathers all lifecycle data and sends it on the Launch event.
     * Use this only if you have set AutoTracking to disabled.
     */
    fun trackLaunchEvent(data: Map<String, Any>? = null) {
        if (isAutoTracking) {
            throw UnsupportedOperationException("Lifecycle Autotracking is enable, cannot manually track lifecycle event")
        }

        val eventData = mutableMapOf<String, Any>(LifecycleStateKey.AUTOTRACKED to false)
        data?.let {
            eventData.putAll(it)
        }

        trackLaunchEvent(System.currentTimeMillis(), eventData.toMap())
    }

    /**
     * Gathers all lifecycle data and sends it on the Wake event.
     * Use this only if you have set AutoTracking to disabled.
     */
    fun trackWakeEvent(data: Map<String, Any>? = null) {
        if (isAutoTracking) {
            throw UnsupportedOperationException("Lifecycle Autotracking is enable, cannot manually track lifecycle event")
        }

        val eventData = mutableMapOf<String, Any>(LifecycleStateKey.AUTOTRACKED to false)
        data?.let {
            eventData.putAll(it)
        }

        trackWakeEvent(System.currentTimeMillis(), eventData.toMap())
    }

    /**
     * Gathers all lifecycle data and sends it on the Sleep event.
     * Use this only if you have set AutoTracking to disabled.
     */
    fun trackSleepEvent(data: Map<String, Any>? = null) {
        if (isAutoTracking) {
            throw UnsupportedOperationException("Lifecycle Autotracking is enable, cannot manually track lifecycle event")
        }

        val eventData = mutableMapOf<String, Any>(LifecycleStateKey.AUTOTRACKED to false)
        data?.let {
            eventData.putAll(it)
        }

        trackSleepEvent(System.currentTimeMillis(), eventData.toMap())
    }

    private fun trackLaunchEvent(timestamp: Long, data: Map<String, Any>? = null) {
        val isFirstLaunch = lifecycleSharedPreferences.timestampFirstLaunch == null
        if (isFirstLaunch) {
            lifecycleSharedPreferences.setFirstLaunchTimestamp(timestamp)
        }
        val currentVersion = getPackageContext().versionName?.toString() ?: ""
        val didUpdate = lifecycleService.didUpdate(timestamp, currentVersion)

        lifecycleSharedPreferences.registerLaunch(timestamp)

        val state: MutableMap<String, Any> = mutableMapOf()

        data?.let {
            state.putAll(it)
        }

        state[LifecycleStateKey.LIFECYCLE_TYPE] = LifecycleEvent.LAUNCH
        lifecycleService.lastLaunchString = LifecycleService.formatTimestamp(timestamp)
        onForegrounding(LifecycleEvent.LAUNCH, state, timestamp)
        lifecycleSharedPreferences.lastLifecycleEvent = LifecycleEvent.LAUNCH

        state[LifecycleStateKey.LIFECYCLE_PRIORSECONDSAWAKE] =
            lifecycleSharedPreferences.priorSecondsAwake
        lifecycleSharedPreferences.resetPriorSecondsAwake()

        if (isFirstLaunch) {
            state[LifecycleStateKey.LIFECYCLE_ISFIRSTLAUNCH] = (true).toString()
        }

        if (didUpdate) {
            state[LifecycleStateKey.LIFECYCLE_ISFIRSTLAUNCHUPDATE] = (true).toString()
        }

        val dispatch = TealiumEvent("launch", state)
        context.track(dispatch)
    }

    private fun trackWakeEvent(timestamp: Long, data: Map<String, Any>? = null) {
        val state: MutableMap<String, Any> = mutableMapOf()

        data?.let {
            state.putAll(it)
        }

        state[LifecycleStateKey.LIFECYCLE_TYPE] = LifecycleEvent.WAKE
        onForegrounding(LifecycleEvent.WAKE, state, timestamp)
        lifecycleSharedPreferences.registerWake(timestamp)
        lifecycleSharedPreferences.lastLifecycleEvent = LifecycleEvent.WAKE
        lifecycleService.lastWakeString = LifecycleService.formatTimestamp(timestamp)

        val dispatch = TealiumEvent("wake", state)
        context.track(dispatch)
    }

    private fun trackSleepEvent(timestamp: Long, data: Map<String, Any>? = null) {
        val secondsAwakeDelta = calculateSecondsAwakeDelta(timestamp)

        lifecycleSharedPreferences.registerSleep(timestamp, secondsAwakeDelta)

        val state: MutableMap<String, Any> = mutableMapOf()

        data?.let {
            state.putAll(it)
        }

        lifecycleSharedPreferences.lastLifecycleEvent = LifecycleEvent.SLEEP

        state[LifecycleStateKey.LIFECYCLE_TYPE] = LifecycleEvent.SLEEP
        state[LifecycleStateKey.LIFECYCLE_SECONDSAWAKE] = secondsAwakeDelta.toString()

        lifecycleService.lastSleepString = LifecycleService.formatTimestamp(timestamp)

        val dispatch = TealiumEvent("sleep", state)
        context.track(dispatch)
    }

    private fun onForegrounding(eventName: String, data: MutableMap<String, Any>, timestamp: Long) {
        val lastWake = lifecycleSharedPreferences.timestampLastWake

        lifecycleService.lastWakeString = LifecycleService.formatTimestamp(timestamp)

        if (lastWake == null) {
            // First Launch
            data[LifecycleStateKey.LIFECYCLE_ISFIRSTWAKEMONTH] = (true).toString()
            data[LifecycleStateKey.LIFECYCLE_ISFIRSTWAKETODAY] = (true).toString()
        }

        if (lifecycleService.didDetectCrash(eventName)) {
            data[LifecycleStateKey.LIFECYCLE_DIDDETECTCRASH] = (true).toString()
            data[LifecycleStateKey.LIFECYCLE_TOTALCRASHCOUNT] = lifecycleSharedPreferences.countTotalCrash

            lifecycleSharedPreferences.incrementCrash()
        }

        data.putAll(addWakeState(lastWake, timestamp))
    }

    private fun getPackageContext(): PackageInfo {
        val packageName = config.application.packageName
        return config.application.packageManager.getPackageInfo(packageName, 0)
    }

    private fun addWakeState(lasWake: Long?, timestamp: Long): Map<String, Any> {
        val state = mutableMapOf<String, Any>()
        if (lasWake == null) {
            state[LifecycleStateKey.LIFECYCLE_ISFIRSTWAKEMONTH] = true.toString()
            state[LifecycleStateKey.LIFECYCLE_ISFIRSTWAKETODAY] = true.toString()
        } else {
            val isFirstWakeResult = FirstWakeType.fromTimestamps(lasWake, timestamp)
            if (isFirstWakeResult.isFirstWakeMonth)  {
                state[LifecycleStateKey.LIFECYCLE_ISFIRSTWAKEMONTH] = true.toString()
                state[LifecycleStateKey.LIFECYCLE_ISFIRSTWAKETODAY] = true.toString()
            } else if (isFirstWakeResult.isFirstWakeToday)  {
                state[LifecycleStateKey.LIFECYCLE_ISFIRSTWAKETODAY] = true.toString()
            }
        }

        return state
    }

    private fun calculateSecondsAwakeDelta(timestamp: Long): Int {
        val foregroundStart: Long = lifecycleSharedPreferences.timestampLastWake ?: timestamp
        return ((timestamp - foregroundStart) / 1000L).toInt()
    }

    override fun onActivityResumed(activity: Activity?) {
        if (!isAutoTracking) {
            return
        }

        val eventData = mapOf<String, Any>(LifecycleStateKey.AUTOTRACKED to true)

        handler.removeCallbacksAndMessages(null)

        val oldResume = lastResume
        lastResume = SystemClock.elapsedRealtime()

        if (oldResume == null) {
            trackLaunchEvent(System.currentTimeMillis(), eventData)
            return
        }

        val lastResume = lastResume ?: 0L
        val lastPause = lastPause ?: 0L

        if (lastResume - lastPause > LifecycleDefaults.SLEEP_THRESHOLD) {
            trackWakeEvent(System.currentTimeMillis(), eventData)
        }
    }

    override fun onActivityPaused(activity: Activity?) {
        if (!isAutoTracking) {
            return
        }

        val eventData = mapOf<String, Any>(LifecycleStateKey.AUTOTRACKED to true)

        if (lastResume == null) {
            val timestampLastLaunch = lifecycleSharedPreferences.timestampLastLaunch ?: System.currentTimeMillis()
            trackLaunchEvent(timestampLastLaunch, eventData)
        }

        lifecycleSharedPreferences.lastLifecycleEvent = LifecycleEvent.SLEEP
        lastPause = SystemClock.elapsedRealtime()

        handler.postDelayed({
            trackSleepEvent(System.currentTimeMillis(), eventData)
        }, LifecycleDefaults.SLEEP_THRESHOLD)
    }

    override fun onActivityStopped(activity: Activity?, isChangingConfiguration: Boolean) {
        // do nothing
    }

    override suspend fun collect(): Map<String, Any> {
        return lifecycleService.getCurrentState(System.currentTimeMillis()).toMap()
    }

    companion object : ModuleFactory {
        const val MODULE_NAME = "Lifecycle"
        const val MODULE_VERSION = BuildConfig.LIBRARY_VERSION

        override fun create(context: TealiumContext): Module {
            return Lifecycle(context)
        }
    }

    override val name: String = MODULE_NAME
    override var enabled: Boolean = true
}

val Modules.Lifecycle: ModuleFactory
    get() = com.tealium.lifecycle.Lifecycle

/**
 * Returns the Lifecycle module for this Tealium instance.
 */
val Tealium.lifecycle: Lifecycle?
    get() = modules.getModule(Lifecycle::class.java)