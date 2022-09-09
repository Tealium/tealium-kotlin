package com.tealium.transformations.internal.impl

import android.webkit.JavascriptInterface
import com.tealium.core.Logger
import com.tealium.transformations.internal.JsConsole

object BaseJsConsoleImpl: JsConsole {
    override fun log(msg: String?) {
        msg?.let {
            Logger.dev("console", it)
        }
    }

    override fun info(msg: String?) {
        msg?.let {
            Logger.qa("console", it)
        }
    }

    override fun error(msg: String?) {
        msg?.let {
            Logger.prod("console", it)
        }
    }
}