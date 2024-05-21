package com.tealium.momentsapi.network

import com.tealium.core.TealiumContext
import com.tealium.core.network.Connectivity
import com.tealium.core.network.ConnectivityRetriever
import com.tealium.momentsapi.ErrorCode
import com.tealium.momentsapi.ResponseListener
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class HttpClient(
    private val context: TealiumContext,
    private val connectivityRetriever: Connectivity = ConnectivityRetriever.getInstance(
        context.config.application
    )
) : NetworkClient {

    override fun get(url: URL, referer: String, listener: ResponseListener<String>) {
        if (!connectivityRetriever.isConnected()) {
            listener.failure(ErrorCode.NOT_CONNECTED, ErrorCode.NOT_CONNECTED.message)
        }

        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"
            setRequestProperty("Referer", referer)
            var reader: BufferedReader? = null
            try {
                when (responseCode) {
                    HttpsURLConnection.HTTP_OK -> {
                        reader = inputStream.bufferedReader(Charsets.UTF_8)
                        val response = reader.readText()
                        listener.success(response)
                    }

                    else -> listener.failure(
                        ErrorCode.fromInt(responseCode), ErrorCode.fromInt(
                            responseCode
                        ).message
                    )
                }
            } catch (ex: Exception) {
                listener.failure(
                    ErrorCode.UNKNOWN_ERROR,
                    ex.message ?: ErrorCode.UNKNOWN_ERROR.message
                )
            } finally {
                reader?.close()
            }
        }
    }

}