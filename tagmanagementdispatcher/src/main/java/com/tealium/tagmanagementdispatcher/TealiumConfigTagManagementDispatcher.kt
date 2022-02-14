package com.tealium.tagmanagementdispatcher

import com.tealium.core.TealiumConfig

const val TAG_MANAGEMENT_OVERRIDE_URL = "override_tag_management_url"
const val TAG_MANAGEMENT_REMOTE_API_ENABLED = "tag_management_remote_api_enabled"
const val TAG_MANAGEMENT_WEBVIEW_CREATION_RETRIES = "tag_management_webview_creation_retries"
const val TAG_MANAGEMENT_WEBVIEW_SHOULD_QUEUE_ON_FAILURE = "tag_management_webview_should_queue_on_failure"

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

/**
 * In some cases outside of our control, the WebView fails to be created. This value sets how many times it will try to be
 * created before being reported as failed.
 *
 * Default: 3
 */
var TealiumConfig.maxWebViewCreationRetries: Int?
    get() = options[TAG_MANAGEMENT_WEBVIEW_CREATION_RETRIES] as? Int
    set(value) {
        value?.let {
            options[TAG_MANAGEMENT_WEBVIEW_CREATION_RETRIES] = it
        }
    }

/**
 * Under normal operation, the TagManagement Dispatcher will request events be queued until the
 * WebView has completed loading - in relation to [TealiumConfig.maxWebViewCreationRetries], this
 * setting sets whether or not events should continue to be queued on the device.
 *
 * This can be useful if you have multiple Dispatchers, and it's more important for the events to be
 * sent by them, forgoing the TagManagement Dispatcher.
 *
 * Default: true
 */
var TealiumConfig.shouldQueueOnLoadFailure: Boolean?
    get() = options[TAG_MANAGEMENT_WEBVIEW_SHOULD_QUEUE_ON_FAILURE] as? Boolean
    set(value) {
        value?.let {
            options[TAG_MANAGEMENT_WEBVIEW_SHOULD_QUEUE_ON_FAILURE] = it
        }
    }