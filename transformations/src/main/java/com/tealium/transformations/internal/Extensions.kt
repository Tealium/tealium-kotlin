package com.tealium.transformations.internal

import app.cash.quickjs.QuickJs
import org.json.JSONObject

fun <T> QuickJs.evaluateJson(js: String, file: String? = null, clazz: Class<T>): T? {
    val result = if (file != null) {
        evaluate(js, file)
    } else {
        evaluate(js)
    }

    return when (clazz) {
        JSONObject::class.java -> JSONObject(result as? String) as? T?
        else -> result as? T?
    }
}