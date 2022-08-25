package com.tealium.transformations.internal

interface Utag {
    fun DB(msg: String?)
    fun transform(event: String, data: String, scope: String): String
}