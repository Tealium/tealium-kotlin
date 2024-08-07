package com.tealium.momentsapi

import com.tealium.core.TealiumConfig

const val MOMENTS_API_REGION = "moments_api_region"
const val MOMENTS_API_REFERRER = "moments_api_referrer"

/**
 * Sets the region used in Moments API engine. A region is required to enable
 * Moments API Service.
 */
var TealiumConfig.momentsApiRegion: MomentsApiRegion?
    get() = options[MOMENTS_API_REGION] as? MomentsApiRegion
    set(value) {
        value?.let {
            options[MOMENTS_API_REGION] = it
        }
    }

var TealiumConfig.momentsApiReferrer: String?
    get() = options[MOMENTS_API_REFERRER] as? String
    set(value) {
        value?.let {
            options[MOMENTS_API_REFERRER] = it
        }
    }