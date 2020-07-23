package com.tealium.remotecommanddispatcher

import androidx.core.text.htmlEncode
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.lang.RuntimeException
import java.net.URLDecoder
import java.util.*
import java.util.regex.Pattern

/**
 * The object used to request a remote command to the remote command dispatcher.
 * A response object must be set in order for the command to be invoked.
 */
data class RemoteCommandRequest(var commandId: String? = null,
                                var payload: JSONObject? = null, // comes from tiq or payload of track call when using json rc
                                var response: Response? = null) {

    companion object {
        fun jsonRequest(remoteCommand: RemoteCommand, mappedPayload: JSONObject?): RemoteCommandRequest {
            return RemoteCommandRequest().apply {
                commandId = remoteCommand.commandId
                payload = mappedPayload
                response = Response(remoteCommand.commandId, requestPayload = mappedPayload)
            }
        }

        fun tagManagementRequest(request: String): RemoteCommandRequest {
            val remoteCommandRequest = RemoteCommandRequest()
            val argsIndex = request.indexOf(ARG)
            var requestArgs: JSONObject? = null

            if (!isRequest(request)) {
                return remoteCommandRequest
            }

            if (argsIndex == -1) {
                remoteCommandRequest.commandId = request.substring(TEALIUM_PREFIX.length).toLowerCase(Locale.ROOT)
            } else {
                remoteCommandRequest.commandId = request.substring(TEALIUM_PREFIX.length, argsIndex).toLowerCase(Locale.ROOT)
                val encodedJSONSuffix = request.substring(argsIndex + ARG.length)
                val decodedJson: String

                try {
                    decodedJson = URLDecoder.decode(encodedJSONSuffix, "UTF-8")
                    println("DECODE JSON: $decodedJson")
                } catch (ex: UnsupportedEncodingException) {
                    throw RuntimeException(ex)
                }

                requestArgs = JSONObject(decodedJson) // if (decodedJson.isNotEmpty()) JSONObject(decodedJson) else JSONObject()
            }
            remoteCommandRequest.commandId?.let {
                if (RemoteCommand.isCommandNameValid(it)) {
                    requestArgs?.let { args ->
                        val config = args.optJSONObject(METAKEY_CONFIG)
                        val responseId = config?.optString(CONFIG_RESPONSE_ID, null)
                        remoteCommandRequest.payload = requestArgs.optJSONObject(METAKEY_PAYLOAD)
                        remoteCommandRequest.response = createResponse(it, responseId, remoteCommandRequest.payload)
                    }
                }
            }
            return remoteCommandRequest
        }

        private fun createResponse(commandName: String, id: String?, args: JSONObject?): Response {
            return object : Response(commandName, id, args) {
                override fun send() {
                    responseId?.let {
                        body?.let {
                            evalJavascript = "try {" +
                                    "	utag.mobile.remote_api.response[\"$commandId\"][\"$responseId\"]($status, ${JSONObject.quote(body)});" +
                                    "} catch(err) {" +
                                    "	console.error(err);" +
                                    "};"
                        } ?: run {
                            evalJavascript = "try {" +
                                    "	utag.mobile.remote_api.response[\"$commandId\"][\"$responseId\"]($status);" +
                                    "} catch(err) {" +
                                    "	console.error(err);" +
                                    "};"
                        }
                    }
                }
            }
        }

        fun isRequest(url: String): Boolean {
            return PROTOCOL_PREFIX.matcher(url).matches()
        }

        const val TEALIUM_PREFIX = "tealium://"
        const val ARG = "?request="
        const val CONFIG_RESPONSE_ID = "response_id"
        const val METAKEY_CONFIG = "config"
        const val METAKEY_PAYLOAD = "payload"

        private val PROTOCOL_PREFIX = Pattern.compile("^$TEALIUM_PREFIX.+", Pattern.CASE_INSENSITIVE);
    }
}