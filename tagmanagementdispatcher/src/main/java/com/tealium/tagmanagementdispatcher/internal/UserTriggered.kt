package com.tealium.tagmanagementdispatcher.internal

import android.os.Handler
import android.os.Looper
import com.tealium.tagmanagementdispatcher.WebViewInitPolicy

/**
 * Delegates the initialization time to the user. Users are expected to call [ready] when they
 * determine that the [android.webkit.WebView] should begin loading.
 *
 * Note. long delays will also delay the processing of events
 */
class UserTriggered: WebViewInitPolicy.ManualWebViewInitPolicy {
    private val subscribers: MutableList<WebViewInitPolicy.WebViewInitPolicyReadyListener> = mutableListOf()
    private var started: Boolean = false
    private val main = Handler(Looper.getMainLooper())

    override fun ready() {
        main.post {
            if (started) return@post

            started = true
            subscribers.forEach { onReady ->
                onReady.onWebViewInitPolicyReady()
            }
            subscribers.clear()
        }
    }

    override fun subscribe(onReady: WebViewInitPolicy.WebViewInitPolicyReadyListener) {
        main.post {
            if (started) {
                onReady.onWebViewInitPolicyReady()
                return@post
            }

            subscribers.add(onReady)
        }
    }
}