package com.tealium.remotecommanddispatcher

import com.tealium.core.Logger
import java.util.*
import java.util.regex.Pattern

abstract class RemoteCommand(var commandId: String,
                             val description: String? = null) {

    init {
        if (!isCommandNameValid(commandId)) {
            throw IllegalArgumentException("Invalid remote command name. The commandId $commandId must conform to URL-domain naming specification.")
        }
        commandId = commandId.toLowerCase(Locale.ROOT)
    }

    /**
     * Execution block for the Remote Command.
     * <p/>
     * This method will need to call [Response.send] for the TealiumIQ
     * callback can be invoked. If an exception is thrown inside of this
     * implementation, the library will perform the callback, so please only
     * call send() on completion.
     *
     * @param response a [Response] object. It's methods possess request
     *                              arguments and response capabilities.
     */
    abstract fun onInvoke(response: Response)

    /**
     * Performs the [RemoteCommand.onInvoke], and
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

    companion object {
        fun isCommandNameValid(commandName: String): Boolean {
            if (commandName.isEmpty()) {
                return false
            }

            return Pattern.matches("^[\\w-]*$", commandName)
        }
    }
}