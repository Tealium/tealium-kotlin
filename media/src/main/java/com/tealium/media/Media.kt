package com.tealium.media

import com.tealium.core.*
import com.tealium.media.segments.Ad
import com.tealium.media.segments.AdBreak
import com.tealium.media.segments.Chapter
import com.tealium.media.sessions.*
import com.tealium.media.sessions.Session

/**
 * Media module provides an easy way to track a media session and its components, such as ad breaks,
 * ads, and chapters. Tracking type options include Significant Events, Milestone, Heartbeat,
 * Heartbeat + Milestone, and Summary.
 */
class Media(private val context: TealiumContext,
            private val mediaDispatcher: MediaDispatcher = MediaSessionDispatcher(context)) : Module {

    override val name: String = MODULE_NAME
    override var enabled: Boolean = true

    var currentSession: Session? = null
        private set

    fun startSession(media: MediaContent) {
        currentSession = when (media.trackingType) {
            TrackingType.HEARTBEAT -> HeartbeatSession(media, mediaDispatcher)
            TrackingType.MILESTONE -> MilestoneSession(media, mediaDispatcher)
            TrackingType.HEARTBEAT_MILESTONE -> HeartbeatMilestoneSession(media, mediaDispatcher)

            TrackingType.SUMMARY -> SummarySession(media, mediaDispatcher)
            else -> SignificantEventsSession(media, mediaDispatcher)
        }
        currentSession?.startSession()
    }

    fun endSession() {
        currentSession?.endSession()
        currentSession = null
    }

    fun startAdBreak(adBreak: AdBreak) = currentSession?.startAdBreak(adBreak)

    fun endAdBreak() = currentSession?.endAdBreak()

    fun clickAd() = currentSession?.clickAd()

    fun startAd(ad: Ad) = currentSession?.startAd(ad)

    fun endAd() = currentSession?.endAd()

    fun skipAd() = currentSession?.skipAd()

    fun startChapter(chapter: Chapter) = currentSession?.startChapter(chapter)

    fun endChapter() = currentSession?.endChapter()

    fun skipChapter() = currentSession?.skipChapter()

    fun startBuffer() = currentSession?.startBuffer()

    fun endBuffer() = currentSession?.endBuffer()

    fun startSeek() = currentSession?.startSeek()

    fun endSeek() = currentSession?.endSeek()

    fun play() = currentSession?.play()

    fun pause() = currentSession?.pause()

    fun stop() = currentSession?.stop()

    fun custom(event: String) = currentSession?.custom(event)

    fun updateBitrate(rate: Int) = currentSession?.updateBitrate(rate)

    fun updateDroppedFrames(frames: Int) = currentSession?.updateDroppedFrames(frames)

    fun updatePlaybackSpeed(speed: Double) = currentSession?.updatePlaybackSpeed(speed)

    fun updatePlayerState(state: PlayerState) = currentSession?.updatePlayerState(state)

    companion object : ModuleFactory {
        const val MODULE_NAME = "MEDIA_SERVICE"
        const val DEFAULT_HEARTBEAT_INTERVAL = 10000L
        override fun create(context: TealiumContext): Module {
            return Media(context)
        }
    }
}

val Tealium.media: Media?
    get() = modules.getModule(Media::class.java)

val Modules.Media: ModuleFactory
    get() = com.tealium.media.Media