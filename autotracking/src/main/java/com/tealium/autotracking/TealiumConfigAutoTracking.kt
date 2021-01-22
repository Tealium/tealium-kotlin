package com.tealium.autotracking

import com.tealium.core.ModuleFactory
import com.tealium.core.Modules
import com.tealium.core.Tealium
import com.tealium.core.TealiumConfig

val Modules.AutoTracking: ModuleFactory
    get() = com.tealium.autotracking.AutoTracking

val Tealium.autoTracking: AutoTracking?
    get() = modules.getModule(AutoTracking::class.java)

const val AUTOTRACKING_MODE = "autotracking_mode"
const val AUTOTRACKING_COLLECTOR_DELEGATE = "autotracking_collector_delegate"

/**
 * Sets the URL to use when requesting the latest Visitor Profile.
 */
var TealiumConfig.autoTrackingMode: AutoTrackingMode
    get() = options[AUTOTRACKING_MODE] as? AutoTrackingMode ?: AutoTrackingMode.FULL
    set(value) {
        value.let {
            options[AUTOTRACKING_MODE] = it
        }
    }

/**
 * Delegate class to collect additional context data for each activity. This delegate, if provided,
 * is called for all activities.
 */
var TealiumConfig.autoTrackingCollectorDelegate: ActivityDataCollector?
    get() = options[AUTOTRACKING_COLLECTOR_DELEGATE] as? ActivityDataCollector
    set(value) {
        value?.let {
            options[AUTOTRACKING_COLLECTOR_DELEGATE] = it
        } ?: run {
            options.remove(AUTOTRACKING_COLLECTOR_DELEGATE)
        }
    }