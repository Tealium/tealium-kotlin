package com.tealium.remotecommanddispatcher

import com.tealium.core.*
import com.tealium.core.messaging.AfterDispatchSendCallbacks
import com.tealium.core.messaging.RemoteCommandListener
import com.tealium.core.network.HttpClient
import com.tealium.core.network.NetworkClient
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.Dispatcher
import com.tealium.dispatcher.DispatcherListener
import org.json.JSONException
import org.json.JSONObject

interface RemoteCommandDispatcherListener : DispatcherListener {

}

class RemoteCommandDispatcher(private val context: TealiumContext,
                              private val client: NetworkClient = HttpClient(context.config)) : Dispatcher, RemoteCommandListener {

    val webViewCommands = mutableMapOf<String, RemoteCommand>()
    val jsonCommands = mutableMapOf<String, RemoteCommand>()
    private val remoteCommandSettingsRetriever = RemoteCommandSettingsRetriever(context)

    init {
    }

    fun add(remoteCommand: RemoteCommand) {
        when (remoteCommand.type) {
            RemoteCommandType.WEBVIEW -> webViewCommands[remoteCommand.commandId] = remoteCommand
            else -> jsonCommands[remoteCommand.commandId] = remoteCommand
        }
    }

    fun remove(commandId: String) {
        if (webViewCommands.containsKey(commandId)) {
            webViewCommands.remove(commandId)
        }

        if (jsonCommands.containsKey(commandId)) {
            jsonCommands.remove(commandId)
        }
    }

    fun loadStockCommands(method: String): RemoteCommand? {
        var remoteCommand: RemoteCommand? = null
        if (HttpRemoteCommand.NAME == method) {
            remoteCommand = HttpRemoteCommand()
        }

        remoteCommand?.let {
            //where to add this?
        }
        return remoteCommand
    }

    private fun executeJsonRemoteCommand(jsonRemoteCommand: RemoteCommand, dispatch: Dispatch) {
        if (jsonCommands.containsKey(jsonRemoteCommand.commandId)) {

        }
    }

    private fun triggerRemoteDispatch(dispatch: Dispatch) {
        jsonCommands.forEach{ (key, command) ->
            command.filename?.let {
                remoteCommandSettingsRetriever.fetchLocalSettings(command)
            }?.let {jsonObject ->
                Logger.dev(BuildConfig.TAG, "Loaded local remote command with json: $jsonObject")
                // put the lookup in a map
                val mappingsLookup = getLookup(jsonObject, Settings.MAPPINGS)

                // map the dispatch with the lookup
                val mappedDispatch = RemoteCommandParser.mapDispatch(dispatch, mappingsLookup)

                // map the tealium_event
                val commandsLookup = getLookup(jsonObject, Settings.COMMANDS)
                val eventName = dispatch[CoreConstant.TEALIUM_EVENT] as? String
                commandsLookup[eventName]?.let {
                    mappedDispatch[Settings.COMMAND_NAME] = it
                } ?: run {
                    eventName?.let {
                        mappedDispatch[Settings.COMMAND_NAME] = it
                    }
                }

            }
        }
    }

    fun getLookup(lookup: JSONObject, lookupKey: String): MutableMap<String, String> {
        val lookupMap = mutableMapOf<String, String>()
        try {
            val config = lookup.getJSONObject(lookupKey)
            config.keys().forEach { key ->
                (config[key] as? String)?.let { value ->
                    lookupMap[key] = value
                }
            }
        } catch (e: JSONException) {
            Logger.dev(BuildConfig.TAG, "Expected $lookupKey in the Remote Command configuration file.") // where's my logger error
        }
        return lookupMap
    }

    private suspend fun executeHttpRemoteCommand(response: Response, url: String, method: String) {
        if ("POST" == method || "PUT" == method) {
            client.post(response.requestPayload.toString(), url, false)
        }
    }

    override fun onExecuteJsonRemoteCommand(dispatch: Dispatch) {
        if (dispatch is RemoteCommandDispatch) {
            triggerRemoteDispatch(dispatch)
        }
    }



//    override fun onRemoteCommandSend(url: String) {
//        val notFoundMsg = "No remote command found with id $"
//    }

    override suspend fun onDispatchSend(dispatch: Dispatch) {
//        TODO("Not yet implemented")
    }

    override suspend fun onBatchDispatchSend(dispatches: List<Dispatch>) {
//        TODO("Not yet implemented")
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