package com.tealium.tagmanagementdispatcher.internal

import com.tealium.tagmanagementdispatcher.WebViewInitPolicy

/**
 * The default [com.tealium.tagmanagementdispatcher.WebViewInitPolicy], which will begin initializing the [android.webkit.WebView]
 * as soon as the [com.tealium.tagmanagementdispatcher.TagManagement] dispatcher is initialized.
 */
object Immediate: WebViewInitPolicy {
    override fun subscribe(onReady: WebViewInitPolicy.WebViewInitPolicyReadyListener) {
        onReady.onWebViewInitPolicyReady()
    }
}