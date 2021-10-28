package com.tealium.autotracking.push

import com.tealium.core.ModuleFactory
import com.tealium.core.Modules
import com.tealium.core.Tealium
import com.tealium.core.TealiumConfig

val Modules.PushTracking: ModuleFactory
    get() = com.tealium.autotracking.push.PushTracking

val Tealium.pushTracking: PushTracking?
    get() = modules.getModule(PushTracking::class.java)

const val AUTOTRACKING_PUSH_ENABLED = "autotracking_push_enabled"

/**
 * Determines whether or not to enabled push notification tracking.
 *
 * Default value is: false
 */
var TealiumConfig.autoTrackingPushEnabled: Boolean?
    get() = options[AUTOTRACKING_PUSH_ENABLED] as? Boolean
    set(value) {
        value?.let {
            options[AUTOTRACKING_PUSH_ENABLED] = it
        } ?: run {
            options.remove(AUTOTRACKING_PUSH_ENABLED)
        }
    }