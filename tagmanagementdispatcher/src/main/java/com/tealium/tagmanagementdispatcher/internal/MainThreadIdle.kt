package com.tealium.tagmanagementdispatcher.internal

import android.os.Handler
import android.os.Looper
import com.tealium.tagmanagementdispatcher.WebViewInitPolicy

/**
 * Waits for the Main thread to become idle before starting initialization.
 *
 * Idleness is determined by a callback from [android.os.MessageQueue.addIdleHandler] on the
 * [android.os.Looper.getMainLooper].
 *
 * note. will not be appropriate for apps whose main thread is never idle
 */
class MainThreadIdle(
    private val main: Handler = Handler(Looper.getMainLooper())
): WebViewInitPolicy {
    private var started = false

    override fun subscribe(onReady: WebViewInitPolicy.WebViewInitPolicyReadyListener) {
        main.post {
            if (started) {
                onReady.onWebViewInitPolicyReady()
                return@post
            }

            Looper.myQueue().addIdleHandler {
                started = true
                onReady.onWebViewInitPolicyReady()
                false // remove idle handler
            }
        }
    }
}