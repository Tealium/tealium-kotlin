package com.tealium.hosteddatalayer

import com.tealium.core.TealiumConfig

const val HOSTED_DATA_LAYER_EVENT_MAPPINGS = "hosted_data_layer_event_mappings"
const val HOSTED_DATA_LAYER_MAX_CACHE_SIZE = "hosted_data_layer_max_cache_size"
const val HOSTED_DATA_LAYER_MAX_CACHE_TIME = "hosted_data_layer_max_cache_time"

/**
 * Sets the event mappings used by the Hosted Data Layer module when looking up Data Layer ids.
 * The key in this map should be equal to the value in the "tealium_event" key of a dispatch.
 * The value in this map should be equal to a key name within the dispatch which should be looked in
 * to retrieve the Data Layer Id.
 */
var TealiumConfig.hostedDataLayerEventMappings: Map<String, String>?
    get() = options[HOSTED_DATA_LAYER_EVENT_MAPPINGS] as? Map<String, String>
    set(value) {
        value?.let {
            options[HOSTED_DATA_LAYER_EVENT_MAPPINGS] = it
        }
    }

/**
 * Sets the cache size limit for how many local files are allowed to be stored on the device.
 */
var TealiumConfig.hostedDataLayerMaxCacheSize: Int?
    get() = options[HOSTED_DATA_LAYER_MAX_CACHE_SIZE] as? Int
    set(value) {
        value?.let {
            options[HOSTED_DATA_LAYER_MAX_CACHE_SIZE] = it
        }
    }

/**
 * Sets the max time, in minutes, that any Hosted Data Layer files should remain cached on the
 * device.
 */
var TealiumConfig.hostedDataLayerMaxCacheTimeMinutes: Long?
    get() = options[HOSTED_DATA_LAYER_MAX_CACHE_TIME] as? Long
    set(value) {
        value?.let {
            options[HOSTED_DATA_LAYER_MAX_CACHE_TIME] = it
        }
    }