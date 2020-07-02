package com.tealium.tagmanagementdispatcher

import com.tealium.core.TealiumConfig

const val TAG_MANAGEMENT_OVERRIDE_URL = "override_tag_management_url"

/**
 * Sets the URL to use for the Tag Management module.
 * Default is "https://tags.tiqcdn.com/utag/{ACCOUNT_NAME}/{PROFILE_NAME}/{ENVIRONMENT}/mobile.html"
 */
var TealiumConfig.overrideTagManagementUrl: String?
    get() = options[TAG_MANAGEMENT_OVERRIDE_URL] as? String
    set(value) {
        value?.let {
            options[TAG_MANAGEMENT_OVERRIDE_URL] = it
        }
    }