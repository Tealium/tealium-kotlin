package com.tealium.media.v2

import com.tealium.media.QoE
import com.tealium.media.toMap

interface MediaSessionDataProvider {
    val metadata: MediaMetadata
    val dataLayer: MediaSessionStorage
    val delegate: MediaSessionDelegate?

    fun getTrackingData(): Map<String, Any>
}

class DefaultMediaSessionDataProvider(
    override val metadata: MediaMetadata,
    override val delegate: MediaSessionDelegate?,
    override val dataLayer: MediaSessionStorage = InMemoryMediaSessionStorage()
) : MediaSessionDataProvider {
    override fun getTrackingData(): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            putAll(metadata.toMap())

            delegate?.let {
                it.getPlayhead()?.let { playhead ->
                    put("playhead", playhead)
                }
                it.getQoe()?.let { qoe ->
                    putAll(qoe.toMap())
                }
            }

            putAll(dataLayer.sessionData)
        }
    }
}

interface MediaSessionDelegate {
    fun getPlayhead(): Int?
    fun getQoe(): QoE?
}