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
import com.tealium.remotecommanddispatcher.remotecommands.JsonRemoteCommand
import com.tealium.remotecommands.RemoteCommand
import com.tealium.remotecommands.RemoteCommandRequest
import java.util.*

//import com.tealium.remotecommanddispatcher.remotecommands.RemoteCommand

interface RemoteCommandDispatcherListener : DispatcherListener {
}

/**
 * The RemoteCommandDispatcher processes and evaluates client-provided commands via
 * Tag Management or JSON-controlled
 */
class RemoteCommandDispatcher(private val context: TealiumContext,
                              private val afterDispatchSendCallbacks: AfterDispatchSendCallbacks,
                              private val client: NetworkClient = HttpClient(context.config)) : Dispatcher, RemoteCommandListener {

    private val webViewCommands = mutableMapOf<String, RemoteCommand>()
    private val jsonCommands = mutableMapOf<String, RemoteCommand>()

    /**
     * Adds remote commands to be evaluated when triggered
     */
    fun add(remoteCommand: RemoteCommand) {
        when (remoteCommand) {
            is JsonRemoteCommand -> {
                jsonCommands[remoteCommand.commandName] = remoteCommand
                remoteCommand.filename?.let {
                    remoteCommand.remoteCommandConfigRetriever = RemoteCommandConfigRetriever(context.config, remoteCommand.commandName, filename = it)
                } ?: run {
                    remoteCommand.remoteUrl?.let {
                        remoteCommand.remoteCommandConfigRetriever = RemoteCommandConfigRetriever(context.config, remoteCommand.commandName, remoteUrl = it)
                    } ?: run {
                        Logger.dev(BuildConfig.TAG, "No filename or remote url found for JSON Remote command: ${remoteCommand.commandName}")
                    }
                }
            }
            else -> webViewCommands[remoteCommand.commandName] = remoteCommand
        }
    }

    /**
     * Remove remote command from being processed
     *
     * @param commandId id of command to be removed
     */
    fun remove(commandId: String) {
        webViewCommands.remove(commandId)
        jsonCommands.remove(commandId)
    }

    /**
     * Removes all remote commands
     */
    fun removeAll() {
        webViewCommands.clear()
        jsonCommands.clear()
    }

    private fun loadHttpCommand(id: String): RemoteCommand? {
        var httpRemoteCommand: RemoteCommand? = null
        if (HttpRemoteCommand.NAME == id) {
            httpRemoteCommand = HttpRemoteCommand(client)
        }

        httpRemoteCommand?.let {
            webViewCommands[HttpRemoteCommand.NAME] = httpRemoteCommand
        }
        return httpRemoteCommand
    }

    private fun invokeTagManagementRequest(request: RemoteCommandRequest?) {
        request?.commandId?.let { id ->
            loadHttpCommand(id)
            webViewCommands[id]?.let { command ->
                Logger.dev(BuildConfig.TAG, "Detected Remote Command $id with payload ${request.response?.requestPayload}")
//                request.response?.javascriptResponse?.let { js ->
//                    afterDispatchSendCallbacks.onEvaluateJavascript(js)
//                }
                command.invoke(request)
            } ?: run {
                Logger.dev(BuildConfig.TAG, "" +
                        "No Remote Command found with id: $id")
            }
        }
    }

    private fun invokeJsonRequest(request: RemoteCommandRequest) {
        jsonCommands.forEach { (key, command) ->
            command.invoke(request)
        }
    }

    private fun parseJsonRemoteCommand(remoteCommand: JsonRemoteCommand, dispatch: Dispatch) {
        remoteCommand.remoteCommandConfigRetriever?.remoteCommandConfig?.let { config ->
            config.mappings?.let { mappings ->
                // map the dispatch with the lookup
                val mappedDispatch = RemoteCommandParser.mapDispatch(dispatch, mappings)
                val eventName = dispatch[CoreConstant.TEALIUM_EVENT] as? String
                config.apiConfig?.get("config")?.let {
                    mappedDispatch[Settings.CONFIG] = it
                }
                config.apiCommands?.get(eventName)?.let {
                    mappedDispatch[Settings.COMMAND_NAME] = it
                } ?: run {
                    return
                }

                Logger.dev(BuildConfig.TAG, "Processing Remote Command: ${remoteCommand.commandName} with command name: ${mappedDispatch[Settings.COMMAND_NAME]}")
                invokeJsonRequest(RemoteCommandRequest(remoteCommand.commandName, JsonUtils.jsonFor(mappedDispatch)))
            }
        }
    }

    override fun onProcessRemoteCommand(dispatch: Dispatch) {
        jsonCommands.forEach { (key, command) ->
            parseJsonRemoteCommand(command as JsonRemoteCommand, dispatch)
        }
    }

    override fun onRemoteCommandSend(handler: RemoteCommand.ResponseHandler, url: String) {
        val request = RemoteCommandRequest(handler, url)
        invokeTagManagementRequest(request)
    }

    override suspend fun onDispatchSend(dispatch: Dispatch) {
        jsonCommands.forEach { (key, command) ->
            parseJsonRemoteCommand(command as JsonRemoteCommand, dispatch)
        }
    }

    override suspend fun onBatchDispatchSend(dispatches: List<Dispatch>) {
        // do nothing - individual dispatch sent through onProcessRemoteCommand without batching
    }

    override val name = "REMOTE_COMMAND_DISPATCHER"
    override var enabled: Boolean = true

    companion object : DispatcherFactory {
        override fun create(context: TealiumContext, callbacks: AfterDispatchSendCallbacks): Dispatcher {
            return RemoteCommandDispatcher(context, callbacks)
        }
    }
}

val Dispatchers.RemoteCommands: DispatcherFactory
    get() = RemoteCommandDispatcher

val Tealium.remoteCommands: RemoteCommandDispatcher?
    get() = modules.getModule(RemoteCommandDispatcher::class.java)