package com.tealium.remotecommanddispatcher

import com.tealium.core.*
import com.tealium.core.messaging.AfterDispatchSendCallbacks
import com.tealium.core.messaging.RemoteCommandListener
import com.tealium.core.network.HttpClient
import com.tealium.core.network.NetworkClient
import com.tealium.core.validation.DispatchValidator
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.Dispatcher
import com.tealium.dispatcher.DispatcherListener

interface RemoteCommandDispatcherListener : DispatcherListener {
}

class RemoteCommandDispatcher(private val context: TealiumContext,
                              private val afterDispatchSendCallbacks: AfterDispatchSendCallbacks,
                              private val client: NetworkClient = HttpClient(context.config)) : Dispatcher, DispatchValidator, RemoteCommandListener {

    private val webViewCommands = mutableMapOf<String, RemoteCommand>()
    private val jsonCommands = mutableMapOf<String, RemoteCommand>()

    fun add(remoteCommand: RemoteCommand) {
        when (remoteCommand.type) {
            RemoteCommandType.WEBVIEW -> webViewCommands[remoteCommand.commandId] = remoteCommand
            else -> {
                jsonCommands[remoteCommand.commandId] = remoteCommand
                remoteCommand.filename?.let {
                    remoteCommand.remoteCommandConfigRetriever = RemoteCommandConfigRetriever(context.config, remoteCommand.commandId, filename = it)
                }
                remoteCommand.remoteUrl?.let {
                    remoteCommand.remoteCommandConfigRetriever = RemoteCommandConfigRetriever(context.config, remoteCommand.commandId, remoteUrl = it)
                }
            }
        }
    }

    fun remove(commandId: String) {
        webViewCommands.remove(commandId)
        jsonCommands.remove(commandId)
    }

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

    private fun processTagManagementRequest(request: RemoteCommandRequest?) {
        request?.commandId?.let { id ->
            loadHttpCommand(id)
            webViewCommands[id]?.let { command ->
                Logger.dev(BuildConfig.TAG, "Detected Remote Command $id with payload ${request.response?.requestPayload}")
                request.response?.evalJavascript?.let { js ->
                    //eventRouter.onEvaluateJavascript(it)
                    // OR
                    afterDispatchSendCallbacks.onEvaluateJavascript(js)
                }
                command.invoke(request)
            } ?: run {
                Logger.dev(BuildConfig.TAG, "" +
                        "No Remote Command found with id: $id")
            }
        }
    }

    private fun processJsonRequest(request: RemoteCommandRequest) {
        jsonCommands.forEach { (key, command) ->
            command.invoke(request)
        }
    }

    private fun parseJsonRemoteCommand(remoteCommand: RemoteCommand, dispatch: Dispatch) {
        remoteCommand.remoteCommandConfigRetriever?.remoteCommandConfig?.let { config ->
            config.mappings?.let { mappings ->
                // map the dispatch with the lookup
                val mappedDispatch = RemoteCommandParser.mapDispatch(dispatch, mappings)
                val eventName = dispatch[CoreConstant.TEALIUM_EVENT] as? String
                config.apiCommands?.get(eventName)?.let {
                    mappedDispatch[Settings.COMMAND_NAME] = it
                } ?: run {
                    return
                }

                Logger.dev(BuildConfig.TAG, "Processing Remote Command: ${remoteCommand.commandId} with command name: ${mappedDispatch[Settings.COMMAND_NAME]}")
                processJsonRequest(RemoteCommandRequest.jsonRequest(remoteCommand, JsonUtils.jsonFor(mappedDispatch)))
            }
        }
    }

    override fun onProcessRemoteCommand(dispatch: Dispatch) {
        jsonCommands.forEach { (key, command) ->
            parseJsonRemoteCommand(command, dispatch)
        }
    }

    override fun onRemoteCommandSend(url: String) {
        val request = RemoteCommandRequest.tagManagementRequest(url)
        request?.let {
            processTagManagementRequest(it)
        }
    }

    override suspend fun onDispatchSend(dispatch: Dispatch) {
        jsonCommands.forEach { (key, command) ->
            parseJsonRemoteCommand(command, dispatch)
        }
    }

    override suspend fun onBatchDispatchSend(dispatches: List<Dispatch>) {
        // do nothing - individual dispatch sent through onProcessRemoteCommand without batching
    }

    override fun shouldQueue(dispatch: Dispatch?): Boolean {
        return false
    }

    override fun shouldDrop(dispatch: Dispatch): Boolean {
        return false
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