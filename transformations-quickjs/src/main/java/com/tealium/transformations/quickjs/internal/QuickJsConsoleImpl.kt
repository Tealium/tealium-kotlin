package com.tealium.transformations.quickjs.internal

import android.webkit.JavascriptInterface
import com.tealium.transformations.internal.JsConsole
import com.tealium.transformations.internal.impl.BaseJsConsoleImpl

class QuickJsConsoleImpl(
    private val delegate: JsConsole = BaseJsConsoleImpl
): JsConsole {
    @JavascriptInterface
    override fun log(msg: String?) = delegate.log(msg)

    @JavascriptInterface
    override fun info(msg: String?) = delegate.info(msg)

    @JavascriptInterface
    override fun error(msg: String?) = delegate.error(msg)
}