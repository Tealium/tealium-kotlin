package com.tealium.visitorservice.momentsapi

interface ResponseListener<T> {
    fun success(data: T)
    fun failure(errorCode: ErrorCode, message: String)
//    fun failure(message: String)
}