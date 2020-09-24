package com.tealium.remotecommanddispatcher

import com.tealium.core.*
import com.tealium.core.messaging.AfterDispatchSendCallbacks
import com.tealium.core.messaging.RemoteCommandListener
import com.tealium.core.network.HttpClient
import com.tealium.core.network.NetworkClient
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.Dispatcher
import com.tealium.dispatcher.DispatcherListener
import com.tealium.remotecommanddispatcher.remotecommands.HttpRemoteCommand
import com.tealium.remotecommands.RemoteCommand
import com.tealium.remotecommands.RemoteCommandRequest

interface RemoteCommandDispatcherListener : DispatcherListener {
}

/**
 * The RemoteCommandDispatcher processes and evaluates client-provided commands via
 * Tag Management or JSON-controlled
 */
class RemoteCommandDispatcher(private val context: TealiumContext,
                              private val client: NetworkClient = HttpClient(context.config)) : Dispatcher, RemoteCommandListener {

//    private val webViewCommands = mutableMapOf<String, RemoteCommand>()
//    private val jsonCommands = mutableMapOf<String, RemoteCommand>()
    private val allRemoteCommands = mutableMapOf<String, RemoteCommand>()
    //do i need this? where else to store RC Config? RemoteCommandConfigStore? Dunno yet.
    private val jsonCommands = mutableMapOf<RemoteCommand, RemoteCommandConfig>()

    /**
     * Adds remote commands to be evaluated when triggered
     */
    fun add(remoteCommand: RemoteCommand, fileName: String? = null, remoteUrl: String? = null) {
        allRemoteCommands[remoteCommand.commandName] = remoteCommand

        if (!fileName.isNullOrEmpty() || !remoteUrl.isNullOrEmpty()) {
            val config = fetch(remoteCommand.commandName, fileName, remoteUrl)
            jsonCommands[remoteCommand] = config
        }

//        when (remoteCommand) {
//            is JsonRemoteCommand -> {
//                jsonCommands[remoteCommand.commandName] = remoteCommand
//                remoteCommand.filename?.let {
//                    remoteCommand.remoteCommandConfigRetriever = RemoteCommandConfigRetriever(context.config, remoteCommand.commandName, filename = it)
//                } ?: run {
//                    remoteCommand.remoteUrl?.let {
//                        remoteCommand.remoteCommandConfigRetriever = RemoteCommandConfigRetriever(context.config, remoteCommand.commandName, remoteUrl = it)
//                    } ?: run {
//                        Logger.dev(BuildConfig.TAG, "No filename or remote url found for JSON Remote command: ${remoteCommand.commandName}")
//                    }
//                }
//            }
//            else ->
//        }
    }

    /**
     * Remove remote command from being processed
     *
     * @param commandId id of command to be removed
     */
    fun remove(commandId: String) {
        val command = allRemoteCommands.remove(commandId)
        jsonCommands.remove(command)
    }

    /**
     * Removes all remote commands
     */
    fun removeAll() {
        allRemoteCommands.clear()
        jsonCommands.clear()
    }

    private fun loadHttpCommand(id: String): RemoteCommand? {
        var httpRemoteCommand: RemoteCommand? = null
        if (HttpRemoteCommand.NAME == id) {
            httpRemoteCommand = HttpRemoteCommand(client)
        }

        httpRemoteCommand?.let {
            allRemoteCommands[HttpRemoteCommand.NAME] = httpRemoteCommand
        }
        return httpRemoteCommand
    }

    private fun invokeTagManagementRequest(request: RemoteCommandRequest?) {
        request?.commandId?.let { id ->
            loadHttpCommand(id)
            allRemoteCommands[id]?.let { command ->
                Logger.dev(BuildConfig.TAG, "Detected Remote Command $id with payload ${request.response?.requestPayload}")
                command.invoke(request)
            } ?: run {
                Logger.dev(BuildConfig.TAG, "" +
                        "No Remote Command found with id: $id")
            }
        }
    }

    private fun parseJsonRemoteCommand(remoteCommand: RemoteCommand, dispatch: Dispatch) {
        jsonCommands[remoteCommand]?.let {remoteCommandConfig ->
            remoteCommandConfig.mappings?.let { mappings ->
                // map the dispatch with the lookup
                val mappedDispatch = RemoteCommandParser.mapDispatch(dispatch, mappings)
                val eventName = dispatch[CoreConstant.TEALIUM_EVENT] as? String
                remoteCommandConfig.apiConfig?.let {
                    mappedDispatch[Settings.CONFIG] = it
                }
                remoteCommandConfig.apiCommands?.get(eventName)?.let {
                    mappedDispatch[Settings.COMMAND_NAME] = it
                } ?: run {
                    return
                }

                Logger.dev(BuildConfig.TAG, "Processing Remote Command: ${remoteCommand.commandName} with command name: ${mappedDispatch[Settings.COMMAND_NAME]}")
                remoteCommand.invoke(RemoteCommandRequest(remoteCommand.commandName, JsonUtils.jsonFor(mappedDispatch)))
            }
        }
    }

    // should I retrieve config this way instead? I'd have to store
    // names/id of json remote commands somewhere
    private fun fetch(commandName: String, fileName: String? = null, remoteUrl: String? = null): RemoteCommandConfig {

        //needs filename or remote url
         val retriever = RemoteCommandConfigRetriever(context.config, commandName, filename = fileName, remoteUrl = remoteUrl)
        return retriever.remoteCommandConfig
    }

    override fun onProcessRemoteCommand(dispatch: Dispatch) {
        allRemoteCommands.forEach { (key, command) ->
            parseJsonRemoteCommand(command, dispatch)
        }
    }

    override fun onRemoteCommandSend(request: RemoteCommandRequest) {
        invokeTagManagementRequest(request)
    }

    override suspend fun onDispatchSend(dispatch: Dispatch) {
        allRemoteCommands.forEach { (key, command) ->
            parseJsonRemoteCommand(command, dispatch)
        }
    }

    override suspend fun onBatchDispatchSend(dispatches: List<Dispatch>) {
        // do nothing - individual dispatch sent through onProcessRemoteCommand without batching
    }

    override val name = "REMOTE_COMMAND_DISPATCHER"
    override var enabled: Boolean = true

    companion object : DispatcherFactory {
        override fun create(context: TealiumContext, callbacks: AfterDispatchSendCallbacks): Dispatcher {
            return RemoteCommandDispatcher(context)
        }
    }
}

val Dispatchers.RemoteCommands: DispatcherFactory
    get() = RemoteCommandDispatcher

val Tealium.remoteCommands: RemoteCommandDispatcher?
    get() = modules.getModule(RemoteCommandDispatcher::class.java)