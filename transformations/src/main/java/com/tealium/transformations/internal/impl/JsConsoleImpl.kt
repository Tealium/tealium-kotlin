package com.tealium.transformations.internal.impl

import android.webkit.JavascriptInterface
import com.tealium.core.Logger
import com.tealium.transformations.internal.JsConsole

class JsConsoleImpl : JsConsole {
    @JavascriptInterface
    override fun log(msg: String?) {
        msg?.let {
            Logger.dev("console", it)
        }
    }

    @JavascriptInterface
    override fun info(msg: String?) {
        msg?.let {
            Logger.qa("console", it)
        }
    }

    @JavascriptInterface
    override fun error(msg: String?) {
        msg?.let {
            Logger.prod("console", it)
        }
    }
}