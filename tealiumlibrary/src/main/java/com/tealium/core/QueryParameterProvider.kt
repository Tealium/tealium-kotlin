package com.tealium.core

interface QueryParameterProvider {
    fun provideParameters(): Map<String, List<String>>
}