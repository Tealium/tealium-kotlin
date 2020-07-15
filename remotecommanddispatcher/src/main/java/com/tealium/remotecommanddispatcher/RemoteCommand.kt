package com.tealium.remotecommanddispatcher

import com.tealium.core.Logger
import com.tealium.dispatcher.Dispatch
import java.util.*

abstract class RemoteCommand(var commandId: String,
                             val description: String? = null,
                             val type: String? = RemoteCommandType.WEBVIEW,
                             val filename: String? = null,
                             val remoteUrl: String? = null,
                             var settings: RemoteCommandSettings? = null) {

    init {
        if (!isCommandNameValid(commandId)) {
            throw IllegalArgumentException("Invalid remote command name. The commandId $commandId must conform to URL-domain naming specification.")
        }
        commandId = commandId.toLowerCase(Locale.ROOT)
    }

    /**
     * Execution block for the Remote Command.
     * <p/>
     * This method will need to call {@link Response#send()} for the TealiumIQ
     * callback can be invoked. If an exception is thrown inside of this
     * implementation, the library will perform the callback, so please only
     * call send() on completion.
     *
     * @param remoteCommandResponse a {@link Response} object. It's methods possess request
     *                              arguments and response capabilities.
     * @throws Throwable the library will call {@link Response#send()} with a status
     *                   of 555 and provide a stack-trace as the body.
     */
    abstract fun onInvoke(response: Response)

    /**
     * Performs the {@link RemoteCommand#onInvoke(RemoteCommand.Response)}, and
     * handles the Throwable if thrown.
     *
     * @param request
     */
    fun invoke(request: RemoteCommandRequest) {
        request.response?.let {
            Logger.dev(BuildConfig.TAG, "Invoking Remote Command ${it.commandId} with ${it.requestPayload}.")
            onInvoke(it)
        }
    }

    fun process(dispatch: Dispatch) {

    }

    companion object {
        fun isCommandNameValid(commandName: String): Boolean {
            if (commandName.isBlank() || commandName.isEmpty()) {
                return false
            }

            val regex = """^[\w-]*${'$'}""".toRegex()
            return regex.containsMatchIn(commandName)
        }
    }
}