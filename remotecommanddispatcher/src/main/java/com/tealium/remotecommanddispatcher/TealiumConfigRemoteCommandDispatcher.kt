package com.tealium.remotecommanddispatcher

import com.tealium.core.TealiumConfig

const val REMOTE_COMMAND_CONFIG_REFRESH = "remote_command_config_refresh"

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