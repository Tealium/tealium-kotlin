package com.tealium.remotecommanddispatcher

import com.tealium.core.TealiumConfig

const val REMOTE_COMMAND_CONFIG_TIQ = "remote_command_config_tiq"

/**
 * Optional config flag for the remote command dispatcher.
 *
 * Set this key to true in TealiumConfig to use TiQ for remote commands.
 */
var TealiumConfig.remoteCommandUseTiQ: Boolean?
    get() = options[REMOTE_COMMAND_CONFIG_TIQ] as? Boolean
    set(value) {
        value?.let {
            options[REMOTE_COMMAND_CONFIG_TIQ] = it
        }
    }