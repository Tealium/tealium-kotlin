package com.tealium.core.network

import org.json.JSONObject

interface NetworkClientListener {
    fun onNetworkResponse(status: Int, response: String)

    fun onNetworkError(message: String)
}

interface NetworkClient {
    var connectivity: Connectivity

    var networkClientListener: NetworkClientListener?

    suspend fun post(payload: String, urlString: String, gzip: Boolean)

    suspend fun ifModified(urlString: String, timestamp: Long): Boolean?

    suspend fun get(urlString: String): String?

    fun validUrl(urlString: String): Boolean
}
