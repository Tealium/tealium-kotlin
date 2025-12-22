package com.tealium.tagmanagementdispatcher

import com.tealium.tagmanagementdispatcher.internal.Delayed
import com.tealium.tagmanagementdispatcher.internal.Immediate
import com.tealium.tagmanagementdispatcher.internal.MainThreadIdle
import com.tealium.tagmanagementdispatcher.internal.UserTriggered

/**
 * Configurable policy class to have some control over when the TagManagement [android.webkit.WebView]
 * begins loading.
 *
 * @see immediate
 * @see delayed
 * @see userTriggered
 * @see onMainThreadIdle
 */
interface WebViewInitPolicy {

    /**
     * Implementations are expected to invoke the given [onReady] listener when desired, however
     * subsequent calls to [subscribe] should always invoke the [onReady] callback.
     *
     * @param onReady
     *  The callback to invoke when the [WebViewInitPolicy] determines that the TagManagement
     *  [android.webkit.WebView] should begin loading.
     */
    fun subscribe(onReady: WebViewInitPolicyReadyListener)

    /**
     * Listener class for subscribing to [WebViewInitPolicy] implementations
     */
    fun interface WebViewInitPolicyReadyListener {

        /**
         * Called when the [WebViewInitPolicy] is ready, and the [android.webkit.WebView] instance
         * is safe to be constructed.
         */
        fun onWebViewInitPolicyReady()
    }

    /**
     * Specialized implementation of [WebViewInitPolicy] that allows for manual control over the init
     * policy. Users should call [ready] when it is appropriate to start the WebView init.
     */
    interface ManualWebViewInitPolicy: WebViewInitPolicy {

        /**
         * Call this method to notify that the [WebViewInitPolicy] is now ready and safe to initialize.
         */
        fun ready()
    }

    companion object {
        /**
         * The default [WebViewInitPolicy], which will begin initializing the [android.webkit.WebView]
         * as soon as the [TagManagement] dispatcher is initialized.
         */
        @JvmStatic
        fun immediate() : WebViewInitPolicy =
            Immediate

        /**
         * Delays the initialization by [delayMillis] from the current time, as taken from
         * [System.currentTimeMillis]
         *
         * Note. long delays will also delay the processing of events
         */
        @JvmStatic
        fun delayed(delayMillis: Long): WebViewInitPolicy =
            Delayed(delayMillis)

        /**
         * Waits for the Main thread to become idle before starting initialization.
         *
         * Idleness is determined by a callback from [android.os.MessageQueue.addIdleHandler] on the
         * [android.os.Looper.getMainLooper].
         *
         * note. will not be appropriate for apps whose main thread is never idle
         */
        @JvmStatic
        fun onMainThreadIdle() : WebViewInitPolicy =
            MainThreadIdle()

        /**
         * Delegates the initialization time to the user. Users are expected to call [ready] when they
         * determine that the [android.webkit.WebView] should begin loading.
         *
         * Note. long delays will also delay the processing of events
         */
        @JvmStatic
        fun userTriggered(): ManualWebViewInitPolicy =
            UserTriggered()
    }
}