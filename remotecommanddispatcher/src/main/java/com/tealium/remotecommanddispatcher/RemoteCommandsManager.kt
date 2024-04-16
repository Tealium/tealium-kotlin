package com.tealium.remotecommanddispatcher

import com.tealium.core.*
import com.tealium.remotecommands.RemoteCommand
import java.util.concurrent.ConcurrentHashMap

interface CommandsManager : Collector {
    fun add(remoteCommand: RemoteCommand, filename: String? = null, remoteUrl: String? = null)
    fun remove(commandId: String)
    fun removeAll()
    fun getRemoteCommand(commandId: String): RemoteCommand?
    fun getRemoteCommandConfigRetriever(commandId: String): RemoteCommandConfigRetriever?
    fun getJsonRemoteCommands(): List<RemoteCommand>
}

class RemoteCommandsManager(private val config: TealiumConfig) : CommandsManager {
    private val allCommands: MutableMap<String, RemoteCommand> = ConcurrentHashMap()
    private val commandsConfigRetriever: MutableMap<String, RemoteCommandConfigRetriever> =
        ConcurrentHashMap()

    override fun add(remoteCommand: RemoteCommand, filename: String?, remoteUrl: String?) {
        allCommands[remoteCommand.commandName] = remoteCommand
        if (!remoteUrl.isNullOrEmpty()) {
            commandsConfigRetriever[remoteCommand.commandName] =
                UrlRemoteCommandConfigRetriever(config, remoteCommand.commandName, remoteUrl)
        } else if (!filename.isNullOrEmpty()) {
            commandsConfigRetriever[remoteCommand.commandName] =
                AssetRemoteCommandConfigRetriever(config, filename)
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

    override suspend fun collect(): Map<String, Any> {
        return if (allCommands.isNotEmpty()) {
            mapOf(Key.REMOTE_COMMANDS to allCommands.map { (key, command) -> if (command.version != null) "${command.commandName}-${command.version}" else "${command.commandName}-0.0" })
        } else emptyMap()
    }

    override val name: String = ""
    override var enabled: Boolean = true
}