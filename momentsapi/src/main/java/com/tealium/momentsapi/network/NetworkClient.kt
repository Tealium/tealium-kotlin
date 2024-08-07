package com.tealium.momentsapi.network

import com.tealium.momentsapi.ResponseListener
import java.net.URL

interface NetworkClient {
    fun get(url: URL, referrer: String, listener: ResponseListener<String>)
}