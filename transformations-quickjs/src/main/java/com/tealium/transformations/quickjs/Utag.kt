package com.tealium.transformations.quickjs

interface Utag {
    fun DB(msg: String?)
    fun transform(event: String, data: String, scope: String): String
}