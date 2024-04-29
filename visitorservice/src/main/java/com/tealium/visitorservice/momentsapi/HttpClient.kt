package com.tealium.visitorservice.momentsapi

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.tealium.core.Logger
import com.tealium.tealiumlibrary.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

interface NetworkClient {
    fun isConnected(): Boolean
    suspend fun get(url:URL, referer: String, listener: ResponseListener<String>)
}

class HttpClient(private val context: Context) : NetworkClient {

    private val connectivityManager: ConnectivityManager
        get() = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val activeNetworkCapabilities: NetworkCapabilities?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            } catch (ex: java.lang.Exception) {
                Logger.qa(BuildConfig.TAG, "Error retrieving active network capabilities, ${ex.message}")
                null
            }
        } else null

    override fun isConnected(): Boolean {
        return activeNetworkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
    }

    override suspend fun get(url: URL, referer: String, listener: ResponseListener<String>) = coroutineScope {
        if (!isConnected()) {
            listener.failure(ErrorCode.NOT_CONNECTED, ErrorCode.NOT_CONNECTED.message)
        }

        withContext(Dispatchers.IO) {
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"
                setRequestProperty("Referer", referer)
                val reader: BufferedReader?
                try {
                    when(responseCode) {
                        HttpsURLConnection.HTTP_OK -> {
                            reader = inputStream.bufferedReader(Charsets.UTF_8)
                            val response = reader.readText()
                            listener.success(response)
                        }
                        else -> listener.failure(ErrorCode.fromInt(responseCode), ErrorCode.fromInt(responseCode).message)
                    }
                } catch (ex: Exception) {
                    listener.failure(ErrorCode.UNKNOWN_ERROR, ex.message ?: ErrorCode.UNKNOWN_ERROR.message)
                }
            }
        }
    }

}