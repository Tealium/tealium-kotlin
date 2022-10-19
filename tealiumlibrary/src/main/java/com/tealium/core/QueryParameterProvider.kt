package com.tealium.core

interface QueryParameterProvider {
    suspend fun provideParameters(): Map<String, List<String>>
}