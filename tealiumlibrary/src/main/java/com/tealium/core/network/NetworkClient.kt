package com.tealium.core.network

import org.json.JSONObject
import java.net.HttpURLConnection

interface NetworkClientListener {
    fun onNetworkResponse(status: Int, response: String)

    fun onNetworkError(message: String)
}

interface NetworkClient {
    var connectivity: Connectivity

    var networkClientListener: NetworkClientListener?

    suspend fun post(payload: String, urlString: String, gzip: Boolean, headers: JSONObject?)

    suspend fun ifModified(urlString: String, timestamp: Long): Boolean?

    suspend fun get(urlString: String): String?

    fun validUrl(urlString: String): Boolean

    fun addHeaders(headers: JSONObject, connection: HttpURLConnection)
}
