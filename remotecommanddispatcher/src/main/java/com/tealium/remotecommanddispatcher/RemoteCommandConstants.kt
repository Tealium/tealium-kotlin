package com.tealium.remotecommanddispatcher

object Settings {
    const val DLE_PREFIX = "https://tags.tiqcdn.com/dle"

    const val CONFIG = "config"
    const val MAPPINGS = "mappings"
    const val COMMANDS = "commands"
    const val STATICS = "statics"
    const val COMMAND_NAME = "command_name"

    const val ALL_EVENTS = "all_events"
    const val ALL_VIEWS = "all_views"

    const val KEYS_SEPARATION_DELIMITER = "keys_separation_delimiter"
    const val KEYS_EQUALITY_DELIMITER = "keys_equality_delimiter"

    const val DEFAULT_SEPARATION_DELIMITER = ","
    const val DEFAULT_EQUALITY_DELIMITER = ":"
}

object Key {
    const val REMOTE_COMMANDS = "remote_commands"
}