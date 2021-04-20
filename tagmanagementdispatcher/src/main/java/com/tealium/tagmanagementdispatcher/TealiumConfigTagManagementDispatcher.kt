package com.tealium.tagmanagementdispatcher

import com.tealium.core.TealiumConfig

const val TAG_MANAGEMENT_OVERRIDE_URL = "override_tag_management_url"
const val TAG_MANAGEMENT_REMOTE_API_ENABLED = "tag_management_remote_api_enabled"

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

/**
 * Enables/Disables `remote_api` events from being sent to the Tealium WebView
 *
 * A value of `false` will stop TagManagement based RemoteCommands from functioning correctly.
 */
var TealiumConfig.remoteApiEnabled: Boolean?
    get() = options[TAG_MANAGEMENT_REMOTE_API_ENABLED] as? Boolean
    set(value) {
        value?.let {
            options[TAG_MANAGEMENT_REMOTE_API_ENABLED] = it
        }
    }