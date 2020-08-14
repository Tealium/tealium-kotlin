package com.tealium.remotecommanddispatcher.remotecommands

import com.tealium.core.Logger
import com.tealium.remotecommanddispatcher.BuildConfig
import com.tealium.remotecommanddispatcher.RemoteCommandRequest
import org.json.JSONObject
import java.util.*
import java.util.regex.Pattern

abstract class RemoteCommand(open var commandId: String,
                             open val description: String? = null) {

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

    open class Response(val commandId: String,
                   val responseId: String? = null,
                   val requestPayload: JSONObject = JSONObject(),
                   var status: Int = STATUS_OK,
                   var body: String? = null,
                   var evalJavascript: String? = null,
                   var sent: Boolean = false) {
        companion object {
            const val STATUS_EXCEPTION_THROWN = 555
            const val STATUS_BAD_REQUEST = 400
            const val STATUS_NOT_FOUND = 404
            const val STATUS_OK = 200
        }

        open fun send() {
            sent = true
        }
    }
}