package com.tealium.tagmanagementdispatcher

import com.tealium.core.messaging.Messenger
import java.lang.Exception

class WebViewExceptionMessenger(private val ex: Exception): Messenger<WebViewExceptionListener>(WebViewExceptionListener::class) {

    override fun deliver(listener: WebViewExceptionListener) {
        listener.onWebViewLoadFailed(ex)
    }
}