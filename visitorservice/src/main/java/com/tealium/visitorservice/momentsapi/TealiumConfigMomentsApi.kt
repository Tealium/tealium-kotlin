package com.tealium.visitorservice.momentsapi

import com.tealium.core.TealiumConfig

const val MOMENTS_API_REGION = "moments_api_region"

var TealiumConfig.momentsApiRegion: MomentsApiRegion?
    get() = options[MOMENTS_API_REGION] as? MomentsApiRegion
    set(value) {
        value?.let {
            options[MOMENTS_API_REGION] = it
        }
    }