package com.tealium.remotecommanddispatcher

import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.util.*
import kotlin.text.StringBuilder

class HttpRemoteCommand : RemoteCommand(NAME, DESCRIPTION) {
    override fun onInvoke(response: Response) {
        val url = response.requestPayload?.optString(URL, null)
        val method = response.requestPayload?.optString(METHOD, null)

        if (url == null || method == null) {
            response.apply {
                status = Response.STATUS_BAD_REQUEST
                body = "Missing required keys \"${METHOD}\" or \"$URL\""
            }.send()
            return
        }

        val urlString = appendParameters(insertAuthCredentials(url, response.requestPayload),
                response.requestPayload)

//        execute(response, urlString, method.toUpperCase(Locale.ROOT))
    }

    private fun insertAuthCredentials(url: String, obj: JSONObject): String {
        val auth = obj.optJSONObject("authenticate")
        auth?.let {
            val username = auth.optString("username")
            val password = auth.optString("password")

            if (username == null || password == null) {
                return url
            }

            var prefix = ""
            if (url.startsWith("http://")) {
                prefix = "http://"
            } else if (url.startsWith("https://")) {
                prefix = "https://"
            } else {
                // TODO: user Logger instead???
                throw JSONException("Unsupported URL protocol.")
            }

            return "$prefix${URLEncoder.encode(username, "UTF-8")}:${URLEncoder.encode(password, "UTF-8")}${url.substring(prefix.length)}"

        } ?: run {
            return url
        }
    }

    private fun appendParameters(url: String, obj: JSONObject): String {
        val parameters = obj.optJSONObject("parameters")
        val sb = StringBuilder()

        parameters?.let { params ->
            val keys = params.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = params.get(key).toString()

                if (sb.isNotEmpty()) {
                    sb.append('&')
                }

                sb.append(URLEncoder.encode(key, "UTF-8")).append(URLEncoder.encode(value, "UTF-8"))
            }

            sb.insert(0, if (url.indexOf('?') > 0) '&' else '?')
            return sb.insert(0, url).toString()

        } ?: run {
            return url
        }
    }



    companion object {
        val NAME = "_http"
        val DESCRIPTION = "Perform a native HTTP operation"

        const val HEADERS = "headers"
        const val URL = "url"
        const val METHOD = "method"
        const val BODY = "body"
    }
}