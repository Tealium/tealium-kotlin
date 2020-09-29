package com.tealium.remotecommanddispatcher

import com.tealium.core.TealiumConfig
import com.tealium.remotecommanddispatcher.remotecommands.JsonRemoteCommand
import com.tealium.remotecommands.RemoteCommand

interface CommandsManager {
    fun add(remoteCommand: RemoteCommand, filename: String? = null, remoteUrl: String? = null)
    fun remove(commandId: String)
    fun removeAll()
    fun retrieve(commandId: String): RemoteCommand?
    fun getRemoteCommandConfig(commandId: String): RemoteCommandConfigRetriever?
    fun getJsonRemoteCommands(): List<RemoteCommand>

}

class RemoteCommandsManager(private val config: TealiumConfig): CommandsManager {
    private val allCommands = mutableMapOf<String, RemoteCommand>()
    private val jsonCommands = mutableMapOf<String, JsonRemoteCommand>()

    override fun add(remoteCommand: RemoteCommand, filename: String?, remoteUrl: String?) {
        allCommands[remoteCommand.commandName] = remoteCommand
        if (filename.isNullOrEmpty() || remoteUrl.isNullOrEmpty()) {
            jsonCommands[remoteCommand.commandName] = JsonRemoteCommand(config, remoteCommand.commandName, filename, remoteUrl)
        }
    }

    override fun remove(commandId: String) {
        allCommands.remove(commandId)
    }

    override fun removeAll() {
        allCommands.clear()
    }

    override fun retrieve(commandId: String): RemoteCommand? {
        return allCommands[commandId]
    }

    override fun getRemoteCommandConfig(commandId: String): RemoteCommandConfigRetriever? {
        return jsonCommands[commandId]?.remoteCommandConfigRetriever
    }

    override fun getJsonRemoteCommands(): List<RemoteCommand> {
        val jsonRemoteCommands = mutableListOf<RemoteCommand>()
        jsonCommands.forEach{(key, command) ->
            allCommands[key]?.let {
                jsonRemoteCommands.add(it)
            }
        }
        return jsonRemoteCommands
    }
}