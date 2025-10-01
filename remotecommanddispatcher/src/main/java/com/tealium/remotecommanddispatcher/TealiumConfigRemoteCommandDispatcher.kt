package com.tealium.remotecommanddispatcher

import com.tealium.core.TealiumConfig

const val REMOTE_COMMAND_CONFIG_REFRESH = "remote_command_config_refresh"
const val REMOTE_COMMAND_REMOTE_API_ENABLED = "remote_command_remote_api_enabled"

/**
 * Optional config flag for the remote command dispatcher - determines the minimum number of minutes
 * required to elapse before attempting to fetch new remote command config from a remote source.
 *
 */
var TealiumConfig.remoteCommandConfigRefresh: Long?
    get() = options[REMOTE_COMMAND_CONFIG_REFRESH] as? Long
    set(value) {
        value?.let {
            options[REMOTE_COMMAND_CONFIG_REFRESH] = it
        }
    }

/**
 * Enables/Disables `remote_api` events from being sent.
 *
 * A value of `true` will enable remote_api events, required for RemoteCommands to function correctly.
 * This is typically set automatically when the RemoteCommandDispatcher is added to the configuration.
 *
 * Default: false
 */
var TealiumConfig.remoteAPIEnabled: Boolean?
    get() = options[REMOTE_COMMAND_REMOTE_API_ENABLED] as? Boolean
    set(value) {
        value?.let {
            options[REMOTE_COMMAND_REMOTE_API_ENABLED] = it
        }
    }