package com.tealium.momentsapi

interface ResponseListener<T> {
    fun success(data: T)
    fun failure(errorCode: ErrorCode, message: String)
}