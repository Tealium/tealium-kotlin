package com.tealium.transformations.internal.impl

import android.webkit.JavascriptInterface
import com.tealium.core.Logger
import com.tealium.transformations.internal.JsConsole
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.JSConstructor
import org.mozilla.javascript.annotations.JSFunction

class JsConsoleImpl
    @JSConstructor constructor(): ScriptableObject(), JsConsole {

    override fun getClassName(): String {
        return "Console"
    }

    @JavascriptInterface
    @JSFunction
    override fun log(msg: String?) {
        msg?.let {
            Logger.dev("console", it)
        }
    }

    @JavascriptInterface
    @JSFunction
    override fun info(msg: String?) {
        msg?.let {
            Logger.qa("console", it)
        }
    }

    @JavascriptInterface
    @JSFunction
    override fun error(msg: String?) {
        msg?.let {
            Logger.prod("console", it)
        }
    }
}