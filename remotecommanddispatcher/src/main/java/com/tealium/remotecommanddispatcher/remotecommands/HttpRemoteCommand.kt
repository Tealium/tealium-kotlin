package com.tealium.remotecommanddispatcher.remotecommands

import android.net.Uri
import com.tealium.core.Logger
import com.tealium.core.network.*
import com.tealium.remotecommanddispatcher.BuildConfig
import com.tealium.remotecommands.RemoteCommand
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.*
import kotlin.text.StringBuilder

class HttpRemoteCommand(private val client: NetworkClient) : RemoteCommand(NAME, DESCRIPTION) {

    override fun onInvoke(response: Response) {
        val url = response.requestPayload.optString(URL, "")
        val method = response.requestPayload.optString(METHOD, "")

        if (url.isNullOrEmpty() || method.isNullOrEmpty()) {
            response.apply {
                status = Response.STATUS_BAD_REQUEST
                body = "Missing required keys \"$METHOD\" or \"$URL\""
            }.send()
            return
        }

        val credentials = insertAuthCredentials(url, response.requestPayload)

        if (credentials.isNotEmpty()) {
            val urlString = appendParameters(credentials, response.requestPayload)
            runBlocking {
                execute(response, urlString, method)
            }
        }
    }

    private suspend fun execute(response: Response, urlString: String, method: String) = coroutineScope {
        if (isActive && client.connectivity.isConnected()) {
            try {
                with(URL(urlString).openConnection() as HttpURLConnection) {
                    addHeaders(response, this)
                    this.requestMethod = method
                    this.doInput = true

                    if (POST == method.toLowerCase(Locale.ROOT) || PUT == method.toLowerCase(Locale.ROOT)) {
                        this.doOutput = true
                        val outputStream = this.outputStream
                        outputStream.write(parseEntity(response.requestPayload))
                        outputStream.flush()
                        outputStream.close()
                    }

                    val input = BufferedReader(InputStreamReader(this.inputStream))
                    val responseEntity = StringBuilder()

                    input.forEachLine {
                        responseEntity.append(it).append('\n')
                    }

                    input.close()

                    response.status = this.responseCode
                    response.body = responseEntity.toString()
                    response.send()

                }
            } catch (e: Exception) {
                Logger.dev(BuildConfig.TAG, "Unknown exception occurred")
                response.apply {
                    status = 555 // TODO - make public Response.STATUS_EXCEPTION_THROWN
                    body = e.toString()
                }
                response.send()
            }
        }
    }

    private fun insertAuthCredentials(url: String, obj: JSONObject): String {
        val auth = obj.optJSONObject("authenticate")
        auth?.let {
            val username = auth.optString("username")
            val password = auth.optString("password")

            if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
                return url
            }

            val prefix = when {
                url.startsWith("http://") -> "http://"
                url.startsWith("https://") -> "https://"
                else -> {
                    Logger.dev(BuildConfig.TAG, "Unsupported URL protocol.")
                    return ""
                }
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

    private fun parseEntity(json: JSONObject): ByteArray {
        val utf8 = Charset.forName("utf-8")

        return when (json.opt(BODY)) {
            is JSONObject -> {
                val temp = json.opt(BODY) as JSONObject
                val keys = temp.keys()
                val builder = Uri.Builder()

                while (keys.hasNext()) {
                    val key = keys.next()
                    builder.appendQueryParameter(key, temp.optString(key, ""))
                }
                builder.build().encodedQuery?.let {
                    it.toByteArray(utf8)
                } ?: run {
                    byteArrayOf()
                }
            }
            is String -> URLEncoder.encode(json.opt(BODY) as String, "utf8").toByteArray(utf8)
            null -> byteArrayOf()
            else -> URLEncoder.encode(json.opt(BODY) as String, "utf-8").toByteArray(utf8)
        }
    }

    private fun addHeaders(response: Response, connection: HttpURLConnection) {
        val headers = response.requestPayload.optJSONObject(HEADERS)
        headers?.let {
            val i = it.keys()
            while (i.hasNext()) {
                val key = i.next()
                val value = it.optString(key, "")
                connection.setRequestProperty(key, value)
            }
        }
    }

    companion object {
        const val NAME = "_http"
        const val DESCRIPTION = "Perform a native HTTP operation"

        const val HEADERS = "headers"
        const val URL = "url"
        const val METHOD = "method"
        const val BODY = "body"

        const val POST = "post"
        const val PUT = "put"
    }
}