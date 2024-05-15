package com.tealium.core.network

import android.webkit.URLUtil
import com.tealium.core.Logger
import com.tealium.core.TealiumConfig
import com.tealium.tealiumlibrary.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.GZIPOutputStream
import javax.net.ssl.HttpsURLConnection

class HttpClient(
    var config: TealiumConfig,
    override var connectivity: Connectivity = ConnectivityRetriever.getInstance(config.application),
    override var networkClientListener: NetworkClientListener? = null
) : NetworkClient {

    private val dateFormat: SimpleDateFormat =
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ROOT)

    init {
        dateFormat.timeZone = TimeZone.getTimeZone("GMT")
    }

    private val isConnected: Boolean
        get() = if (!connectivity.isConnected()) {
            networkClientListener?.onNetworkError("No network connection.")
            false
        } else true

    override suspend fun post(payload: String, urlString: String, gzip: Boolean): Unit =
        coroutineScope {
            withContext(Dispatchers.IO) {
                try {
                    with(URL(urlString).openConnection() as HttpURLConnection) {
                        if (isActive && isConnected) {
                            try {
                                doOutput = true
                                setRequestProperty("Content-Type", "application/json")
                                val dataOutputStream = when (gzip) {
                                    true -> {
                                        setRequestProperty("Content-Encoding", "gzip")
                                        DataOutputStream(GZIPOutputStream(outputStream))
                                    }
                                    false -> {
                                        DataOutputStream(outputStream)
                                    }
                                }
                                dataOutputStream.write(payload.toByteArray(Charsets.UTF_8))
                                dataOutputStream.flush()
                                dataOutputStream.close()
                            } catch (e: Exception) {
                                networkClientListener?.onNetworkError(e.toString())
                            }
                            val code = responseCode
                            val message = responseMessage
                            networkClientListener?.onNetworkResponse(code, message)
                        }
                    }
                } catch (e: ConnectException) {
                    Logger.prod(BuildConfig.TAG, "Could not connect to host: $e.")
                    networkClientListener?.onNetworkError(e.toString())
                } catch (e: Exception) {
                    Logger.prod(BuildConfig.TAG, "An unknown exception occurred: $e.")
                    networkClientListener?.onNetworkError(e.toString())
                }
                Unit
            }
        }

    /**
     * Checks if the resource has been modified from the timestamp.
     */
    override suspend fun ifModified(urlString: String, timestamp: Long): Boolean? = coroutineScope {
        withContext(Dispatchers.IO) {
            try {
                with(URL(urlString).openConnection() as HttpURLConnection) {
                    var isModified: Boolean? = null
                    if (isActive && isConnected) {
                        requestMethod = "HEAD"
                        // e.g. Wed, 21 Oct 2015 07:28:00 GMT
                        setRequestProperty("If-Modified-Since", dateFormat.format(Date(timestamp)))
                        if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                            Logger.dev(
                                BuildConfig.TAG,
                                "Resource not modified, not fetching resource."
                            )
                            isModified = false
                        }
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            isModified = true
                        }
                        networkClientListener?.onNetworkResponse(responseCode, responseMessage)
                    }
                    isModified
                }
            } catch (e: Exception) {
                Logger.prod(BuildConfig.TAG, "An unknown exception occurred: $e.")
                networkClientListener?.onNetworkError(e.toString())
                null
            }
        }
    }

    override suspend fun getResourceEntity(urlString: String, etag: String?): ResourceEntity? = coroutineScope {
        withContext(Dispatchers.IO) {
            try {
                with(URL(urlString).openConnection() as HttpURLConnection) {
                    if (!isActive) {
                        return@with ResourceEntity(
                            status = ResponseStatus.Cancelled
                        )
                    }
                    if (!isConnected) {
                        return@with ResourceEntity(
                            status = ResponseStatus.NotConnected
                        )
                    }

                    requestMethod = "GET"
                    setRequestProperty("If-None-Match", etag)

                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        ResourceEntity(
                            inputStream.bufferedReader().readText(),
                            getHeaderField("etag"),
                            ResponseStatus.Success
                        )
                    } else {
                        ResourceEntity(
                            status = ResponseStatus.Non200Response(responseCode)
                        )
                    }
                }
            } catch (e: Exception) {
                Logger.prod(BuildConfig.TAG, "An unknown exception occurred: $e.")
                networkClientListener?.onNetworkError(e.toString())
                ResourceEntity(
                    status = ResponseStatus.UnknownError(e)
                )
            }
        }
    }

    override suspend fun get(urlString: String): String? = coroutineScope {
        withContext(Dispatchers.IO) {
            try {
                with(URL(urlString).openConnection() as HttpURLConnection) {
                    if (isActive && isConnected) {
                        requestMethod = "GET"
                        if (responseCode == HttpsURLConnection.HTTP_OK) {
                            inputStream.bufferedReader().readText()
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                Logger.prod(BuildConfig.TAG, "An unknown exception occurred: $e.")
                networkClientListener?.onNetworkError(e.toString())
                null
            }
        }
    }

    override fun validUrl(urlString: String): Boolean {
        return URLUtil.isValidUrl(urlString)
    }
}