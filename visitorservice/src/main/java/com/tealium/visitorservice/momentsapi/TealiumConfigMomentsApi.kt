package com.tealium.visitorservice.momentsapi

import com.tealium.core.TealiumConfig

const val MOMENTS_API_REGION = "moments_api_region"

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