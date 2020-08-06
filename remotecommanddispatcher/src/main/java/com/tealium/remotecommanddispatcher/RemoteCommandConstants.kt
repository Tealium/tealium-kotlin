package com.tealium.remotecommanddispatcher

enum class RemoteCommandType(val type: String) {
    WEBVIEW("webview"),
    JSON("json")
}

object Settings {
    const val DLE_PREFIX = "https://tags.tiqcdn.com/dle"

    const val CONFIG = "config"
    const val MAPPINGS = "mappings"
    const val COMMANDS = "commands"
    const val COMMAND_NAME = "command_name"
}