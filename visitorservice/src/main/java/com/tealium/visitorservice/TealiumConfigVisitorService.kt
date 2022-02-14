package com.tealium.visitorservice

import com.tealium.core.TealiumConfig

const val VISITOR_SERVICE_OVERRIDE_URL = "override_visitor_service_url"
const val VISITOR_SERVICE_OVERRIDE_PROFILE = "override_visitor_service_profile"
const val VISITOR_SERVICE_REFRESH_INTERVAL = "override_visitor_refresh_interval"

/**
 * Sets the URL to use when requesting the latest Visitor Profile.
 */
var TealiumConfig.overrideVisitorServiceUrl: String?
    get() = options[VISITOR_SERVICE_OVERRIDE_URL] as? String
    set(value) {
        value?.let {
            options[VISITOR_SERVICE_OVERRIDE_URL] = it
        }
    }

/**
 * Overrides the profile to use when requesting the latest Visitor Profile.
 */
var TealiumConfig.overrideVisitorServiceProfile: String?
    get() = options[VISITOR_SERVICE_OVERRIDE_PROFILE] as? String
    set(value) {
        value?.let {
            options[VISITOR_SERVICE_OVERRIDE_PROFILE] = it
        }
    }

/**
 * Sets the length of time in Seconds to use between requesting an updated Visitor Profile
 */
var TealiumConfig.visitorServiceRefreshInterval: Long?
    get() = options[VISITOR_SERVICE_REFRESH_INTERVAL] as? Long
    set(value) {
        value?.let {
            options[VISITOR_SERVICE_REFRESH_INTERVAL] = it
        }
    }