package com.tealium.transformations.internal

interface JsConsole {
    fun log(msg: String?)
    fun info(msg: String?)
    fun error(msg: String?)
}