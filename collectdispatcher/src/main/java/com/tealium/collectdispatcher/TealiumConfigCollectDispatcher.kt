package com.tealium.collectdispatcher

import com.tealium.core.TealiumConfig

const val COLLECT_OVERRIDE_URL = "override_collect_url"

/**
 * Sets the URL to send event data to. Defaults are:
 * https://collect.tealiumiq.com/event for individual events, and
 * https://collect.tealiumiq.com/bulk-event for batches of multiple events.
 */
var TealiumConfig.overrideCollectUrl: String?
    get() = options[COLLECT_OVERRIDE_URL] as? String
    set(value) {
        value?.let {
            options[COLLECT_OVERRIDE_URL] = it
        }
    }