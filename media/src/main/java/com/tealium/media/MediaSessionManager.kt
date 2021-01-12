package com.tealium.media

import com.tealium.core.*

class MediaSessionManager(private val context: TealiumContext) : Module {
    override val name: String = MODULE_NAME
    override var enabled: Boolean = true

    /**
     * Records start of new media session
     */
    fun start() {

    }

    /**
     * Records start of new media session
     */
    fun play() {

    }

    /**
     * Records media paused by user
     */
    fun pause() {

    }

    /**
     * Records media stopped by user
     */
    fun stop() {

    }

    /**
     * Records media session completed by user
     */
    fun close() {

    }

    companion object : ModuleFactory {
        const val MODULE_NAME = "MEDIA_MANAGER"
        override fun create(context: TealiumContext): Module {
            return MediaSessionManager(context);
        }
    }
}

val Tealium.mediaSessionManager: MediaSessionManager?
    get() = modules.getModule(MediaSessionManager::class.java)

val Modules.MediaManager: ModuleFactory
    get() = com.tealium.media.MediaSessionManager