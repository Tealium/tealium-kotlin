package com.tealium.media

import com.tealium.core.ModuleFactory
import com.tealium.core.Modules
import com.tealium.core.Tealium
import com.tealium.core.TealiumConfig

val Modules.Media: ModuleFactory
    get() = com.tealium.media.Media

val Tealium.media: Media?
    get() = modules.getModule(Media::class.java)

const val MEDIA_BACKGROUND_SESSION_ENABLED = "media_background_tracking_enabled"
const val MEDIA_BACKGROUND_SESSION_END_INTERVAL = "media_background_session_end_interval"

/**
 * Denotes whether to allow media module to continue tracking when media session
 * is in background. If set to true, media will continue to track media session
 * events. If set to false, media module will call endSession() for
 * current session after interval is reached
 *
 * Default is false
 */
var TealiumConfig.mediaBackgroundSessionEnabled: Boolean?
    get() = options[MEDIA_BACKGROUND_SESSION_ENABLED] as? Boolean
    set(value) {
        value?.let {
            options[MEDIA_BACKGROUND_SESSION_ENABLED] = it
        }
    }

/**
 * Sets the length of time in Seconds to use when [mediaBackgroundSessionEnabled] is
 * set to false. Media module will call endSession on current media session when interval
 * is reached.
 *
 * Default is 60000L (60 seconds)
 */
var TealiumConfig.mediaBackgroundSessionEndInterval: Long?
    get() = options[MEDIA_BACKGROUND_SESSION_END_INTERVAL] as? Long
    set(value) {
        value?.let {
            options[MEDIA_BACKGROUND_SESSION_END_INTERVAL] = it
        }
    }