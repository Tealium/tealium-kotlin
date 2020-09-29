package com.tealium.remotecommanddispatcher

import com.tealium.core.TealiumConfig
import com.tealium.remotecommands.RemoteCommand

interface CommandsManager {
    fun add(remoteCommand: RemoteCommand, filename: String? = null, remoteUrl: String? = null)
    fun remove(commandId: String)
    fun removeAll()
    fun getRemoteCommand(commandId: String): RemoteCommand?
    fun getRemoteCommandConfigRetriever(commandId: String): RemoteCommandConfigRetriever?
    fun getJsonRemoteCommands(): List<RemoteCommand>
}

class RemoteCommandsManager(private val config: TealiumConfig) : CommandsManager {
    private val allCommands = mutableMapOf<String, RemoteCommand>()
    private val commandsConfigRetriever = mutableMapOf<String, RemoteCommandConfigRetriever>()

    override fun add(remoteCommand: RemoteCommand, filename: String?, remoteUrl: String?) {
        allCommands[remoteCommand.commandName] = remoteCommand
        if (!filename.isNullOrEmpty() || !remoteUrl.isNullOrEmpty()) {
            commandsConfigRetriever[remoteCommand.commandName] = RemoteCommandConfigRetriever(config, remoteCommand.commandName, filename, remoteUrl)
        }
    }

    override fun remove(commandId: String) {
        allCommands.remove(commandId)
    }

    override fun removeAll() {
        allCommands.clear()
    }

    override fun getRemoteCommand(commandId: String): RemoteCommand? {
        return allCommands[commandId]
    }

    override fun getRemoteCommandConfigRetriever(commandId: String): RemoteCommandConfigRetriever? {
        return commandsConfigRetriever[commandId]
    }

    override fun getJsonRemoteCommands(): List<RemoteCommand> {
        val jsonRemoteCommands = mutableListOf<RemoteCommand>()
        commandsConfigRetriever.forEach { (key, command) ->
            allCommands[key]?.let {
                jsonRemoteCommands.add(it)
            }
        }
        return jsonRemoteCommands
    }
}