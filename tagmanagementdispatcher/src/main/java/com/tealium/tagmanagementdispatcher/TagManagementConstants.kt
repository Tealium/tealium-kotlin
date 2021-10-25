package com.tealium.tagmanagementdispatcher

object TagManagementConstants {
    const val EVENT = "event"
}

object TagManagementRemoteCommand {
    const val PREFIX = "tealium://"
    const val TIQ_CONFIG = "remote_command_config_tiq"
}

enum class PageStatus {
    LOADED_SUCCESS,
    LOADED_ERROR,
    LOADING,
    INIT
}