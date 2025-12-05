package com.tealium.tagmanagementdispatcher.internal

import android.os.Handler
import android.os.Looper
import com.tealium.tagmanagementdispatcher.WebViewInitPolicy

/**
 * Delays the initialization by [delayMillis] from the current time, as taken from
 * [System.currentTimeMillis]
 *
 * Note. long delays will also delay the processing of events
 */
class Delayed(
    val delayMillis: Long,
    private val timingProvider: () -> Long = System::currentTimeMillis,
    private val main: Handler = Handler(Looper.getMainLooper())
): WebViewInitPolicy {
    private val initTimeMillis = timingProvider.invoke()

    override fun subscribe(onReady: WebViewInitPolicy.WebViewInitPolicyReadyListener) {
        val currentTimeMillis = timingProvider.invoke()
        val elapsedMillis = currentTimeMillis - initTimeMillis

        main.postDelayed({
            onReady.onWebViewInitPolicyReady()
        }, delayMillis - elapsedMillis)
    }
}