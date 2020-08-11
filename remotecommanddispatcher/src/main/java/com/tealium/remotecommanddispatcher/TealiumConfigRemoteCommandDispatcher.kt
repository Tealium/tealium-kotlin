package com.tealium.remotecommanddispatcher

import com.tealium.core.TealiumConfig

const val REMOTE_COMMAND_CONFIG_REFRESH = "remote_command_config_refresh"

/**
 * Optional config flag for the remote command dispatcher.
 *
 * Set this key to  in TealiumConfig to use TiQ for remote commands.
 */
var TealiumConfig.remoteCommandConfigRefresh: Long?
    get() = options[REMOTE_COMMAND_CONFIG_REFRESH] as? Long
    set(value) {
        value?.let {
            options[REMOTE_COMMAND_CONFIG_REFRESH] = it
        }
    }