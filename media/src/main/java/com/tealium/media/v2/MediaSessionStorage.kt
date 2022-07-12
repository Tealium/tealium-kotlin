package com.tealium.media.v2

import java.util.*

interface MediaSessionStorage {
    fun put(key: String,  data: Any)
    val sessionData: Map<String, Any>
}

class InMemoryMediaSessionStorage(
    initData: Map<String, Any>? = null
): MediaSessionStorage {

    private val _sessionData = Collections.synchronizedMap(initData?.toMutableMap() ?: mutableMapOf()) //mutableMapOf<String, Any>()

    override fun put(key: String, data: Any) {
        _sessionData[key] = data
    }

    override val sessionData: Map<String, Any>
        get() = _sessionData.toMap()
}