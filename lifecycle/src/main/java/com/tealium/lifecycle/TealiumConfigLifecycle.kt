package com.tealium.lifecycle

import com.tealium.core.TealiumConfig

const val IS_LIFECYCLE_AUTOTRACKING = "is_lifecycle_autotracking"

/**
 * Sets whether or not to automatically send Lifecycle events. Set this value to false only if you
 * intend to trigger lifecycle events manually.
 */
var TealiumConfig.isAutoTrackingEnabled: Boolean?
    get() = options[IS_LIFECYCLE_AUTOTRACKING] as? Boolean
    set(value) {
        value?.let {
            options[IS_LIFECYCLE_AUTOTRACKING] = it
        }
    }