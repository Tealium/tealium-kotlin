package com.tealium.momentsapi.network

import com.tealium.momentsapi.ResponseListener
import java.net.URL

interface NetworkClient {
    fun isConnected(): Boolean
    suspend fun get(url: URL, referer: String, listener: ResponseListener<String>)
}