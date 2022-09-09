package com.tealium.transformations.j2v8.internal

import android.webkit.JavascriptInterface
import com.tealium.transformations.internal.JsConsole
import com.tealium.transformations.internal.impl.BaseJsConsoleImpl

class J2v8JsConsoleImpl(
    private val delegate: JsConsole = BaseJsConsoleImpl
): JsConsole by delegate