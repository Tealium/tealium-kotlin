package com.tealium.remotecommanddispatcher.remotecommands

import android.net.Uri
import com.tealium.core.Logger
import com.tealium.core.network.*
import com.tealium.remotecommanddispatcher.BuildConfig
import com.tealium.remotecommands.RemoteCommand
import kotlinx.coroutines.*
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

    private val utf8 = Charset.forName("utf-8")

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

    private suspend fun execute(response: Response, urlString: String, method: String) = withContext(Dispatchers.IO) {
        if (isActive && client.connectivity.isConnected()) {
            try {
                with(URL(urlString).openConnection() as HttpURLConnection) {
                    val headers: JSONObject? = response.requestPayload.optJSONObject(HEADERS)?.let {
                        addHeaders(it, this)
                        it
                    }
                    this.requestMethod = method
                    this.doInput = true

                    if (POST == method.toLowerCase(Locale.ROOT) || PUT == method.toLowerCase(Locale.ROOT)) {
                        this.doOutput = true
                        val outputStream = this.outputStream
                        outputStream.write(
                                parseEntity(
                                        response.requestPayload.opt(BODY),
                                        headers?.optString("Content-Type") ?: ""))
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
                    status = Response.STATUS_EXCEPTION_THROWN
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

    private fun parseEntity(body: Any?, contentType: String): ByteArray {

        return body?.let { Formatters.formatterFor(contentType).format(it, utf8) } ?: byteArrayOf()
    }

    private fun addHeaders(headers: JSONObject, connection: HttpURLConnection) {
        val i = headers.keys()
        while (i.hasNext()) {
            val key = i.next()
            val value = headers.optString(key, "")
            connection.setRequestProperty(key, value)
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

    internal object Formatters {
        private val formatters: MutableMap<String, Formatter> = mutableMapOf()

        fun formatterFor(contentType: String): Formatter {
            return formatters[contentType] ?: run {
                return when(contentType) {
                    "application/x-www-form-urlencoded" -> FormsUrlFormatter()
                    "application/json" -> DefaultFormatter()
                    else -> DefaultFormatter()
                }.also { formatters[contentType] = it }
            }
        }
    }

    internal interface Formatter {
        fun format(payload: Any, charset: Charset): ByteArray?
    }

    private class DefaultFormatter: Formatter {
        override fun format(payload: Any, charset: Charset): ByteArray? {
            return payload.toString().toByteArray(charset)
        }
    }

    private class FormsUrlFormatter: Formatter {
        override fun format(payload: Any, charset: Charset): ByteArray? {
            return when (payload) {
                is JSONObject -> {
                    val keys = payload.keys()
                    val builder = Uri.Builder()

                    while (keys.hasNext()) {
                        val key = keys.next()
                        builder.appendQueryParameter(key, payload.optString(key, ""))
                    }
                    builder.build().encodedQuery?.toByteArray(charset)
                }
                is String -> URLEncoder.encode(payload, charset.name()).toByteArray(charset)
                else -> URLEncoder.encode(payload.toString(), charset.name()).toByteArray(charset)
            }
        }
    }
}