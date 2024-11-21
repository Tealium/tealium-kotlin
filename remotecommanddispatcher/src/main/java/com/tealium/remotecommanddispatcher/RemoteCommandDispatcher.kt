package com.tealium.remotecommanddispatcher

import com.tealium.core.*
import com.tealium.core.messaging.AfterDispatchSendCallbacks
import com.tealium.core.messaging.RemoteCommandListener
import com.tealium.core.network.HttpClient
import com.tealium.core.network.NetworkClient
import com.tealium.core.DispatchType
import com.tealium.dispatcher.*
import com.tealium.remotecommanddispatcher.remotecommands.HttpRemoteCommand
import com.tealium.remotecommands.RemoteCommand
import com.tealium.remotecommands.RemoteCommandContext
import com.tealium.remotecommands.RemoteCommandRequest

interface RemoteCommandDispatcherListener : DispatcherListener {
}

/**
 * The RemoteCommandDispatcher processes and evaluates client-provided commands via
 * Tag Management or JSON-controlled
 */
class RemoteCommandDispatcher(private val context: TealiumContext,
                              private val client: NetworkClient = HttpClient(context.config),
                              private val manager: CommandsManager = RemoteCommandsManager(context.config)) : Dispatcher, RemoteCommandListener , Collector by manager {

    /**
     * Adds Remote Commands to be evaluated when triggered. If adding a JSON-controlled
     * Remote Command, provide either a filename or a remote URL where the JSON config
     * is located.
     *
     * @param remoteCommand command to be added to dispatcher
     * @param filename Optional filename for JSON controlled Remote Commands
     * @param remoteUrl Optional remote URL for JSON controlled Remote Commands
     */
    fun add(remoteCommand: RemoteCommand, filename: String? = null, remoteUrl: String? = null) {
        remoteCommand.context = remoteCommand.context ?: createRemoteCommandContext()
        manager.add(remoteCommand, filename, remoteUrl)
    }

    /**
     * Remove Remote Command from being processed.
     *
     * @param commandId id of command to be removed
     */
    fun remove(commandId: String) {
        manager.remove(commandId)
    }

    /**
     * Removes all Remote Commands.
     */
    fun removeAll() {
        manager.removeAll()
    }

    private fun loadHttpCommand(): RemoteCommand? {
        return manager.getRemoteCommand(HttpRemoteCommand.NAME) ?: HttpRemoteCommand(client).also {
            manager.add(it)
        }
    }

    private fun invokeTagManagementRequest(request: RemoteCommandRequest?) {
        request?.commandId?.let { id ->
            if (id == HttpRemoteCommand.NAME) loadHttpCommand()

            manager.getRemoteCommand(id)?.let { command ->
                Logger.dev(BuildConfig.TAG, "Detected Remote Command $id with payload ${request.response?.requestPayload}")
                command.invoke(request)
            } ?: run {
                Logger.dev(BuildConfig.TAG, "" +
                        "No Remote Command found with id: $id")
            }
        }
    }

    private fun parseJsonRemoteCommand(remoteCommand: RemoteCommand, dispatch: Dispatch) {
        manager.getRemoteCommandConfigRetriever(remoteCommand.commandName)?.remoteCommandConfig?.let { config ->

            val staticsDispatch = RemoteCommandParser.processStaticMappings(config.statics, dispatch.payload(), config.delimiters)

            config.mappings?.let { mappings ->
                // map the dispatch with the lookup
                val mappedDispatch = RemoteCommandParser.mapPayload(staticsDispatch, mappings)

                config.apiConfig?.let {
                    mappedDispatch.putAll(it)
                }

                val commands = RemoteCommandParser.extractCommandNames(config.apiCommands, dispatch.payload(), config.delimiters)

                if (commands.isEmpty()) {
                    return
                }

                mappedDispatch[Settings.COMMAND_NAME] = commands

                Logger.dev(
                    BuildConfig.TAG,
                    "Processing Remote Command: ${remoteCommand.commandName} with command name: ${mappedDispatch[Settings.COMMAND_NAME]}"
                )
                remoteCommand.invoke(
                    RemoteCommandRequest(
                        remoteCommand.commandName,
                        JsonUtils.jsonFor(mappedDispatch)
                    )
                )
            }
        }
    }

    private fun createRemoteCommandContext(): RemoteCommandContext {
        return object : RemoteCommandContext {
            override fun track(eventName: String, data: MutableMap<String, *>?) {
                context.track(TealiumEvent(eventName, data as Map<String, Any>))
            }

            override fun track(eventType: String?, eventName: String, data: MutableMap<String, *>?) {
                when (eventType) {
                    DispatchType.EVENT -> context.track(TealiumEvent(eventName, data as Map<String, Any>))
                    DispatchType.VIEW -> context.track(TealiumEvent(eventName, data as Map<String, Any>))
                }
            }
        }
    }

    override fun onProcessRemoteCommand(dispatch: Dispatch) {
        manager.getJsonRemoteCommands().forEach { jsonCommand ->
            parseJsonRemoteCommand(jsonCommand, dispatch)
        }
    }

    override fun onRemoteCommandSend(request: RemoteCommandRequest) {
        invokeTagManagementRequest(request)
    }

    override suspend fun onDispatchSend(dispatch: Dispatch) {
        manager.refreshConfig()
    }

    override suspend fun onBatchDispatchSend(dispatches: List<Dispatch>) {
        manager.refreshConfig()
    }

    override val name = "RemoteCommands"
    override var enabled: Boolean = true

    companion object : DispatcherFactory {
        const val MODULE_VERSION = BuildConfig.LIBRARY_VERSION

        override fun create(context: TealiumContext, callbacks: AfterDispatchSendCallbacks): Dispatcher {
            return RemoteCommandDispatcher(context)
        }
    }
}

val Dispatchers.RemoteCommands: DispatcherFactory
    get() = RemoteCommandDispatcher

val Tealium.remoteCommands: RemoteCommandDispatcher?
    get() = modules.getModule(RemoteCommandDispatcher::class.java)