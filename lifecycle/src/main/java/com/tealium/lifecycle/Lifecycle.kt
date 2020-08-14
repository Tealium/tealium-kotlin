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
class Lifecycle(private val context: TealiumContext) : Module, ActivityObserverListener {

    private val config: TealiumConfig = context.config
    var isAutoTracking = config.isAutoTrackingEnabled ?: true

    private var lastResume = LifecycleDefaults.TIMESTAMP_INVALID
    private var lastPause = LifecycleDefaults.TIMESTAMP_INVALID
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

        trackLaunchEvent(System.currentTimeMillis(), data)
    }

    /**
     * Gathers all lifecycle data and sends it on the Wake event.
     * Use this only if you have set AutoTracking to disabled.
     */
    fun trackWakeEvent(data: Map<String, Any>? = null) {
        if (isAutoTracking) {
            throw UnsupportedOperationException("Lifecycle Autotracking is enable, cannot manually track lifecycle event")
        }

        trackWakeEvent(System.currentTimeMillis(), data)
    }

    /**
     * Gathers all lifecycle data and sends it on the Sleep event.
     * Use this only if you have set AutoTracking to disabled.
     */
    fun trackSleepEvent(data: Map<String, Any>? = null) {
        if (isAutoTracking) {
            throw UnsupportedOperationException("Lifecycle Autotracking is enable, cannot manually track lifecycle event")
        }

        trackSleepEvent(System.currentTimeMillis(), data)
    }

    private fun trackLaunchEvent(timestamp: Long, data: Map<String, Any>? = null) {
        val isFirstLaunch = lifecycleService.isFirstLaunch(timestamp)
        val currentVersion = getPackageContext().versionName?.toString() ?: ""
        val didUpdate = lifecycleService.didUpdate(timestamp, currentVersion)

        lifecycleSharedPreferences.incrementLaunch()
        lifecycleSharedPreferences.incrementWake()

        val state: MutableMap<String, Any> = lifecycleService.getCurrentState(timestamp)

        data?.let {
            state.putAll(it)
        }

        state[LifecycleStateKey.LIFECYCLE_TYPE] = LifecycleEvent.LAUNCH
        lifecycleSharedPreferences.setLastLaunch(timestamp)
        onForegrounding(LifecycleEvent.LAUNCH, state, timestamp)
        lifecycleSharedPreferences.lastLifecycleEvent = LifecycleEvent.LAUNCH

        state[LifecycleStateKey.LIFECYCLE_PRIORSECONDSAWAKE] = lifecycleSharedPreferences.priorSecondsAwake

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
        lifecycleSharedPreferences.incrementWake()

        val state: MutableMap<String, Any> = lifecycleService.getCurrentState(timestamp)

        data?.let {
            state.putAll(it)
        }

        state[LifecycleStateKey.LIFECYCLE_TYPE] = LifecycleEvent.WAKE
        onForegrounding(LifecycleEvent.WAKE, state, timestamp)
        lifecycleSharedPreferences.lastLifecycleEvent = LifecycleEvent.WAKE

        val dispatch = TealiumEvent("wake", state)
        context.track(dispatch)
    }

    private fun trackSleepEvent(timestamp: Long, data: Map<String, Any>? = null) {
        val foregroundStart: Long = if (lifecycleSharedPreferences.timestampLastWake > LifecycleDefaults.TIMESTAMP_INVALID) lifecycleSharedPreferences.timestampLastWake
        else LifecycleDefaults.TIMESTAMP_INVALID
        val secondsAwakeDelta: Int = ((timestamp - foregroundStart) / 1000L).toInt()
        lifecycleSharedPreferences.incrementSleep()
        lifecycleSharedPreferences.updateSecondsAwake(secondsAwakeDelta)

        val state: MutableMap<String, Any> = lifecycleService.getCurrentState(timestamp)

        data?.let {
            state.putAll(it)
        }

        lifecycleSharedPreferences.lastLifecycleEvent = LifecycleEvent.SLEEP

        state[LifecycleStateKey.LIFECYCLE_TYPE] = LifecycleEvent.SLEEP
        state[LifecycleStateKey.LIFECYCLE_SECONDSAWAKE] = (secondsAwakeDelta).toString()

        lifecycleSharedPreferences.setLastSleep(timestamp)

        val dispatch = TealiumEvent("sleep", state)
        context.track(dispatch)
    }

    private fun onForegrounding(eventName: String, data: MutableMap<String, Any>, timestamp: Long) {
        val lastWake = lifecycleSharedPreferences.timestampLastWake
        lifecycleSharedPreferences.setLastWake(timestamp)

        if (lastWake == LifecycleDefaults.TIMESTAMP_INVALID) {
            // First Launch
            data[LifecycleStateKey.LIFECYCLE_ISFIRSTWAKEMONTH] = (true).toString()
            data[LifecycleStateKey.LIFECYCLE_ISFIRSTWAKETODAY] = (true).toString()
        }

        if (lifecycleService.didDetectCrash(eventName)) {
            data[LifecycleStateKey.LIFECYCLE_DIDDETECTCRASH] = (true).toString()
            data[LifecycleStateKey.LIFECYCLE_TOTALCRASHCOUNT] = lifecycleSharedPreferences.countTotalCrash
        }

        val isFirstWakeResult = lifecycleService.isFirstWake(lastWake, timestamp)

        if (lifecycleService.isFirstWakeMonth(isFirstWakeResult)) {
            data[LifecycleStateKey.LIFECYCLE_ISFIRSTWAKEMONTH] = true.toString()
        }

        if (lifecycleService.isFirstWakeToday(isFirstWakeResult)) {
            data[LifecycleStateKey.LIFECYCLE_ISFIRSTWAKETODAY] = true.toString()
        }
    }

    private fun getPackageContext(): PackageInfo {
        val packageName = config.application.packageName
        return config.application.packageManager.getPackageInfo(packageName, 0)
    }

    override fun onActivityResumed(activity: Activity?) {
        if (!isAutoTracking) {
            return
        }

        val eventData = mapOf<String, Any>(LifecycleStateKey.AUTOTRACKED to true)

        handler.removeCallbacksAndMessages(null)

        val oldResume = lastResume
        lastResume = SystemClock.elapsedRealtime()

        if (oldResume == LifecycleDefaults.TIMESTAMP_INVALID) {
            trackLaunchEvent(System.currentTimeMillis(), eventData)
            return
        }

        if (lastResume - lastPause > LifecycleDefaults.SLEEP_THRESHOLD) {
            trackWakeEvent(System.currentTimeMillis(), eventData)
        }
    }

    override fun onActivityPaused(activity: Activity?) {
        if (!isAutoTracking) {
            return
        }

        val eventData = mapOf<String, Any>(LifecycleStateKey.AUTOTRACKED to true)

        if (lastResume == LifecycleDefaults.TIMESTAMP_INVALID) {
            trackLaunchEvent(lifecycleSharedPreferences.timestampLastLaunch, eventData)
        }

        lifecycleSharedPreferences.lastLifecycleEvent = LifecycleEvent.PAUSE
        lastPause = SystemClock.elapsedRealtime()

        handler.postDelayed({
            trackSleepEvent(System.currentTimeMillis(), null)
        }, LifecycleDefaults.SLEEP_THRESHOLD)
    }

    override fun onActivityStopped(activity: Activity?, isChangingConfiguration: Boolean) {
        // do nothing
    }

    companion object : ModuleFactory {
        const val MODULE_NAME = "LIFECYCLE"

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