package com.tealium.core.network

interface NetworkClientListener {
    fun onNetworkResponse(status: Int, response: String)

    fun onNetworkError(message: String)
}

interface NetworkClient {
    var connectivity: Connectivity

    var networkClientListener: NetworkClientListener?

    suspend fun post(payload: String, urlString: String, gzip: Boolean)

    suspend fun ifModified(urlString: String, timestamp: Long): Boolean?

//    suspend fun ifNoneMatch(urlString: String, etag: String): Boolean?

    suspend fun get(urlString: String): String?

    suspend fun getResourceEntity(urlString: String, etag: String? = null): ResourceEntity?

    fun validUrl(urlString: String): Boolean
}
