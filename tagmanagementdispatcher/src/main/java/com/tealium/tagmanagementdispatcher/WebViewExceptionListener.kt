package com.tealium.tagmanagementdispatcher

import com.tealium.core.messaging.ExternalListener
import java.lang.Exception

/**
 * Listener class to receive notification that the WebView has failed to be created.
 */
interface WebViewExceptionListener: ExternalListener {
    fun onWebViewLoadFailed(ex: Exception)
}