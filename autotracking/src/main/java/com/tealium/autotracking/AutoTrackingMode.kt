package com.tealium.autotracking

/**
 * Specifies the tracking mode in use.
 *
 * [FULL] will track screen view events for all Activities.
 * [ANNOTATED] will track screen view events only for Activities annotated with [Autotracked]
 * [NONE] will disable automatically tracked events
 */
enum class AutoTrackingMode {
    FULL, ANNOTATED, NONE
}