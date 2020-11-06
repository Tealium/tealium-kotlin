package com.tealium.collectdispatcher

import com.tealium.core.TealiumConfig

const val COLLECT_OVERRIDE_DOMAIN = "override_collect_domain"
const val COLLECT_OVERRIDE_URL = "override_collect_url"
const val COLLECT_OVERRIDE_BATCH_URL = "override_collect_batch_url"

/**
 * Sets the Domain to send event data to. Use this in preference to either [overrideCollectUrl] or
 * [overrideCollectBatchUrl] to override only the domain portion of Tealium Collect endpoint.
 *
 * Default value is `collect.tealiumiq.com` and this will be used to build the full URL for both
 * individual and batched events
 */
var TealiumConfig.overrideCollectDomain: String?
    get() = options[COLLECT_OVERRIDE_DOMAIN] as? String
    set(value) {
        value?.let {
            options[COLLECT_OVERRIDE_DOMAIN] = it
        }
    }

/**
 * Sets the URL to send individual event data to. Default is:
 * https://collect.tealiumiq.com/event
 *
 * See [overrideCollectBatchUrl] if batching is enabled.
 */
var TealiumConfig.overrideCollectUrl: String?
    get() = options[COLLECT_OVERRIDE_URL] as? String
    set(value) {
        value?.let {
            options[COLLECT_OVERRIDE_URL] = it
        }
    }

/**
 * Sets the Bulk URL to send event data to. Defaults is:
 * https://collect.tealiumiq.com/bulk-event
 */
var TealiumConfig.overrideCollectBatchUrl: String?
    get() = options[COLLECT_OVERRIDE_BATCH_URL] as? String
    set(value) {
        value?.let {
            options[COLLECT_OVERRIDE_BATCH_URL] = it
        }
    }