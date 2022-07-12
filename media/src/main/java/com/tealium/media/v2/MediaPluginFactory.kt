package com.tealium.media.v2

import com.tealium.media.MediaDispatcher

interface MediaPluginFactory {
    fun create(
        mediaSessionDataProvider: MediaSessionDataProvider,
        events: MediaSessionEvents,
        tracker: MediaDispatcher
    ): MediaSessionPlugin
}

interface ConfigurableMediaPluginFactory<T> {
    fun configure(
        config: T
    ): MediaPluginFactory
}

// Example
class BasicMediaPlugin(
    private val mediaSessionDataProvider: MediaSessionDataProvider,
    private val events: MediaSessionEvents,
    private val tracker: MediaDispatcher
) : MediaSessionPlugin {

    companion object Factory : MediaPluginFactory {
        override fun create(
            mediaSessionDataProvider: MediaSessionDataProvider,
            events: MediaSessionEvents,
            tracker: MediaDispatcher
        ): MediaSessionPlugin {
            return BasicMediaPlugin(
                mediaSessionDataProvider,
                events,
                tracker
            )
        }
    }
}

// Example
class ConfigurableMediaPlugin(
    private val mediaSessionDataProvider: MediaSessionDataProvider,
    private val events: MediaSessionEvents,
    private val tracker: MediaDispatcher,
    private val interval: Int
) : MediaSessionPlugin {

    companion object Factory : ConfigurableMediaPluginFactory<Int> {

        override fun configure(config: Int): MediaPluginFactory {
            return object : MediaPluginFactory {
                override fun create(
                    mediaSessionDataProvider: MediaSessionDataProvider,
                    events: MediaSessionEvents,
                    tracker: MediaDispatcher
                ): MediaSessionPlugin {
                    return ConfigurableMediaPlugin(
                        mediaSessionDataProvider,
                        events,
                        tracker,
                        config
                    )
                }
            }
        }
    }
}