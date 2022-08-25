package com.tealium.transformations.internal

interface Storage {
    fun save(key: String, data: Any, expiry: String?)
    fun read(key: String): Any?
}