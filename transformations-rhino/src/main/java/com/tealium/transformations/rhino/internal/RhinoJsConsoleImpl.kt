package com.tealium.transformations.rhino.internal

import android.webkit.JavascriptInterface
import com.tealium.core.Logger
import com.tealium.transformations.internal.JsConsole
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.JSConstructor
import org.mozilla.javascript.annotations.JSFunction

class RhinoJsConsoleImpl
    @JSConstructor constructor(): ScriptableObject(), JsConsole {

    override fun getClassName(): String {
        return "Console"
    }

    @JSFunction
    override fun log(msg: String?) {
        msg?.let {
            Logger.dev("console", it)
        }
    }

    @JSFunction
    override fun info(msg: String?) {
        msg?.let {
            Logger.qa("console", it)
        }
    }

    @JSFunction
    override fun error(msg: String?) {
        msg?.let {
            Logger.prod("console", it)
        }
    }
}