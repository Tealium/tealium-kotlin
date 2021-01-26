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
const val AUTOTRACKING_BLACKLIST_FILENAME = "autotracking_blacklist_filename"
const val AUTOTRACKING_BLACKLIST_URL = "autotracking_blacklist_url"

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

/**
 * Local asset filename to use for blacklisting activity names. Expected usage is to allow easy
 * filtering of a specific subset of activities by their name when autotracking screen view events.
 *
 * Content of the asset is expected to be a JSON Array of String values, where the value of the strings
 * will be compared to the activity name using a case-insensitive `contains` operation.
 */
var TealiumConfig.autoTrackingBlacklistFilename: String?
    get() = options[AUTOTRACKING_BLACKLIST_FILENAME] as? String
    set(value) {
        value?.let {
            options[AUTOTRACKING_BLACKLIST_FILENAME] = it
        } ?: run {
            options.remove(AUTOTRACKING_BLACKLIST_FILENAME)
        }
    }

/**
 * Remote URL to use for blacklisting activity names. Expected usage is to allow easy
 * filtering of a specific subset of activities by their name when autotracking screen view events.
 * The file will be fetched once per app launch, during initialization.
 *
 * Content of the file is expected to be a JSON Array of String values, where the value of the strings
 * will be compared to the activity name using a case-insensitive `contains` operation.
 */
var TealiumConfig.autoTrackingBlacklistUrl: String?
    get() = options[AUTOTRACKING_BLACKLIST_URL] as? String
    set(value) {
        value?.let {
            options[AUTOTRACKING_BLACKLIST_URL] = it
        } ?: run {
            options.remove(AUTOTRACKING_BLACKLIST_URL)
        }
    }