package com.tealium.autotracking

import android.app.Activity
import com.tealium.autotracking.internal.ActivityAutoTracker
import com.tealium.core.Module
import com.tealium.core.ModuleFactory
import com.tealium.core.TealiumContext

class AutoTracking(
        private val context: TealiumContext,
        private val activityTracker: ActivityTracker = ActivityAutoTracker(context,
                context.config.autoTrackingMode,
                context.config.autoTrackingCollectorDelegate)
) : Module {
    override val name: String = MODULE_NAME
    override var enabled: Boolean = true

    init {
        context.events.subscribe(activityTracker)
    }

    /**
     * Manually tracks a screen view event for an Activity
     */
    @JvmOverloads
    fun trackActivity(activity: Activity, data: Map<String, Any>? = null) = activityTracker.trackActivity(activity, data)

    /**
     * Manually tracks a screen view event where the screen implements [ActivityDataCollector]
     */
    @JvmOverloads
    fun trackActivity(activityDataCollector: ActivityDataCollector, data: Map<String, Any>? = null) = activityTracker.trackActivity(activityDataCollector, data)

    companion object : ModuleFactory {
        private const val MODULE_NAME = "AUTOTRACKING"

        override fun create(context: TealiumContext): Module {
            return AutoTracking(context)
        }
    }
}