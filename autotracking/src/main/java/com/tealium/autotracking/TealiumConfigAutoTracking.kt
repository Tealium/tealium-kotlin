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
const val AUTOTRACKING_BLOCKLIST_FILENAME = "autotracking_blocklist_filename"
const val AUTOTRACKING_BLOCKLIST_URL = "autotracking_blocklist_url"

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
 * Local asset filename to use for blocklisting activity names. Expected usage is to allow easy
 * filtering of a specific subset of activities by their name when autotracking screen view events.
 *
 * Content of the asset is expected to be a JSON Array of String values, where the value of the strings
 * will be compared to the activity name using a case-insensitive `contains` operation.
 */
var TealiumConfig.autoTrackingBlocklistFilename: String?
    get() = options[AUTOTRACKING_BLOCKLIST_FILENAME] as? String
    set(value) {
        value?.let {
            options[AUTOTRACKING_BLOCKLIST_FILENAME] = it
        } ?: run {
            options.remove(AUTOTRACKING_BLOCKLIST_FILENAME)
        }
    }

/**
 * Remote URL to use for blocklisting activity names. Expected usage is to allow easy
 * filtering of a specific subset of activities by their name when autotracking screen view events.
 * The file will be fetched once per app launch, during initialization.
 *
 * Content of the file is expected to be a JSON Array of String values, where the value of the strings
 * will be compared to the activity name using a case-insensitive `contains` operation.
 */
var TealiumConfig.autoTrackingBlocklistUrl: String?
    get() = options[AUTOTRACKING_BLOCKLIST_URL] as? String
    set(value) {
        value?.let {
            options[AUTOTRACKING_BLOCKLIST_URL] = it
        } ?: run {
            options.remove(AUTOTRACKING_BLOCKLIST_URL)
        }
    }