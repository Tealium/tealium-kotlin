package com.tealium.media.v2

import com.tealium.core.Module
import com.tealium.core.ModuleFactory
import com.tealium.core.TealiumContext
import com.tealium.media.MediaSessionDispatcher
import com.tealium.media.QoE
import com.tealium.media.v2.messaging.MediaMessageDispatcher
import com.tealium.media.v2.messaging.MediaMessageRouter

class MediaModule(
    private val context: TealiumContext
) : Module {

    private var _session: MediaSession? = null
    val session: MediaSession?
        get() = _session

    private var mediaMessageDispatcher: MediaMessageRouter = MediaMessageDispatcher()

    fun createSession(
        metadata: MediaMetadata,
        plugins: Set<MediaPluginFactory>,
        mediaSessionDelegate: MediaSessionDelegate? = null
    ): MediaSession {
        val storage = InMemoryMediaSessionStorage()
        val tracker = MediaSessionDispatcher(context)

        return MediaSession(
            plugins.map {
                it.create(
                    DefaultMediaSessionDataProvider(
                        metadata, mediaSessionDelegate, storage
                    ), MediaMessageDispatcher(), tracker
                )
            }.toSet(),
            mediaMessageDispatcher,
        ).also {
            _session = it
        }
    }

    override val name: String = "MediaModule"
    override var enabled: Boolean = true

    companion object : ModuleFactory {
        override fun create(context: TealiumContext): Module {
            return MediaModule(context)
        }
    }
}