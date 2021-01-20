package com.tealium.media

import com.tealium.core.*
import com.tealium.dispatcher.TealiumEvent
import com.tealium.media.segments.Ad
import com.tealium.media.segments.AdBreak
import com.tealium.media.segments.Chapter


class Media(private val tealiumContext: TealiumContext,
            var currentSession: Session? = null) : Module {
    override val name: String = MODULE_NAME
    override var enabled: Boolean = true

    fun startSession(mediaSession: Session) {
        currentSession = mediaSession
        currentSession?.start()
        tealiumContext.track(TealiumEvent(MediaEvent.SESSION_START))
    }

    fun endSession() {
        currentSession?.end()
        // send session summary?
        tealiumContext.track(TealiumEvent(MediaEvent.SESSION_END))
        currentSession = null
    }

    fun startAdBreak(adBreak: AdBreak) {
        currentSession?.let {
            it.adBreakStart(adBreak)
            tealiumContext.track(TealiumEvent(MediaEvent.ADBREAK_START))
        }
    }

    fun endAdBreak() {
        currentSession?.let {
            tealiumContext.track(TealiumEvent(MediaEvent.ADBREAK_COMPLETE, currentSession?.adComplete()))
        }
    }

    fun clickAd() {
        currentSession?.let {
            tealiumContext.track(TealiumEvent(MediaEvent.AD_CLICK, currentSession?.adClick()))
        }
    }

    fun startAd(ad: Ad) {
        currentSession?.let {
            it.adStart(ad)
            tealiumContext.track(TealiumEvent(MediaEvent.AD_START))
        }
    }

    fun endAd() {
        currentSession?.let {
            tealiumContext.track(TealiumEvent(MediaEvent.AD_COMPLETE, currentSession?.adComplete()))
        }
    }

    fun skipAd() {
        currentSession?.let {
            tealiumContext.track(TealiumEvent(MediaEvent.AD_SKIP))
        }
    }

    fun startChapter(chapter: Chapter) {
        currentSession?.let {
            it.chapterStart(chapter)
            tealiumContext.track(TealiumEvent(MediaEvent.CHAPTER_START))
        }
    }

    fun endChapter() {
        currentSession?.let {
            tealiumContext.track(TealiumEvent(MediaEvent.CHAPTER_COMPLETE, currentSession?.chapterComplete()))
        }
    }

    fun skipChapter() {
        currentSession?.let {
            tealiumContext.track(TealiumEvent(MediaEvent.CHAPTER_SKIP))
        }
    }

    fun startBuffer() {
        currentSession?.let {
        tealiumContext.track(TealiumEvent(MediaEvent.BUFFER_START))
        }
    }

    fun endBuffer() {
        currentSession?.let {
            tealiumContext.track(TealiumEvent(MediaEvent.BUFFER_COMPLETE))
        }
    }

    fun startSeek() {
        currentSession?.let {
            tealiumContext.track(TealiumEvent(MediaEvent.SEEK_START))
        }
    }

    fun endSeek() {
        currentSession?.let {
        tealiumContext.track(TealiumEvent(MediaEvent.SEEK_COMPLETE))
        }
    }


    fun play() {
        currentSession?.let {
            tealiumContext.track(TealiumEvent(MediaEvent.PLAY))
        }
    }

    fun pause() {
        currentSession?.let {
            tealiumContext.track(TealiumEvent(MediaEvent.PAUSE))
        }
    }

    fun stop() {
        currentSession?.let {
            tealiumContext.track(TealiumEvent(MediaEvent.STOP))
        }
    }

    fun custom(event: String) {
        currentSession?.let {
            tealiumContext.track(TealiumEvent(event))
        }
    }

    fun updateBitrate(rate: Int) {
        currentSession?.let {
            it.updateBitrate(rate)
            tealiumContext.track(TealiumEvent(MediaEvent.BITRATE_CHANGE))
        }
    }

    fun updateDroppedFrames(frames: Int) {
        currentSession?.updateDroppedFrames(frames)
    }

    fun updatePlaybackSpeed(speed: Double) {
        currentSession?.updatePlaybackSpeed(speed)
    }

    fun updatePlayerState(state: PlayerState) {
        currentSession?.state?.let {
            tealiumContext.track(TealiumEvent(MediaEvent.PLAYER_STATE_STOP))
            currentSession?.updatePlayerState(state)
            tealiumContext.track(TealiumEvent(MediaEvent.PLAYER_STATE_START))
        } ?: run {
            currentSession?.updatePlayerState(state)
            tealiumContext.track(TealiumEvent(MediaEvent.PLAYER_STATE_START))
        }
    }

    companion object : ModuleFactory {
        const val MODULE_NAME = "MEDIA_SERVICE"
        override fun create(context: TealiumContext): Module {
            return Media(context)
        }
    }
}

val Tealium.media: Media?
    get() = modules.getModule(Media::class.java)

val Modules.Media: ModuleFactory
    get() = com.tealium.media.Media