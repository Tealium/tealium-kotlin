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

    /**
     * Begins a session for given [MediaContent] based on the [TrackingType] provided
     */
    fun startSession(media: MediaContent) {
        Logger.dev(BuildConfig.TAG, "Starting Media Session for: ${media.name}")
        currentSession = when (media.trackingType) {
            TrackingType.HEARTBEAT -> HeartbeatSession(media, mediaDispatcher)
            TrackingType.MILESTONE -> MilestoneSession(media, mediaDispatcher)
            TrackingType.HEARTBEAT_MILESTONE -> HeartbeatMilestoneSession(media, mediaDispatcher)

            TrackingType.SUMMARY -> SummarySession(media, mediaDispatcher)
            else -> SignificantEventsSession(media, mediaDispatcher)
        }
        currentSession?.startSession()
    }

    /**
     * Records end of current media session
     */
    fun endSession() {
        Logger.dev(BuildConfig.TAG, "End of Media Session.")
        currentSession?.endSession()
        currentSession = null
    }

    /**
     * Starts an ad break segment with give [AdBreak] segment
     */
    fun startAdBreak(adBreak: AdBreak) = currentSession?.startAdBreak(adBreak)

    /**
     * Ends latest ad break for the current session
     */
    fun endAdBreak() = currentSession?.endAdBreak()

    /**
     * Starts an ad segment with give [Ad]
     */
    fun startAd(ad: Ad) = currentSession?.startAd(ad)

    /**
     * Records an ad click for current ad playing
     */
    fun clickAd() = currentSession?.clickAd()

    /**
     * Records an ad skip for current ad playing
     */
    fun skipAd() = currentSession?.skipAd()

    /**
     * Ends latest ad for current session
     */
    fun endAd() = currentSession?.endAd()

    /**
     * Starts a chapter segment with give [Chapter]
     */
    fun startChapter(chapter: Chapter) = currentSession?.startChapter(chapter)

    /**
     * Records a chapter skip for latest chapter playing
     */
    fun skipChapter() = currentSession?.skipChapter()

    /**
     * Ends latest chapter in current session
     */
    fun endChapter() = currentSession?.endChapter()

    /**
     * Records buffering start
     */
    fun startBuffer() = currentSession?.startBuffer()

    /**
     * Records buffering complete
     */
    fun endBuffer() = currentSession?.endBuffer()

    /**
     * Records seek started at given position
     *
     * @param position media position in seconds where seek started
     */
    fun startSeek(position: Int) = currentSession?.startSeek(position)

    /**
     * Records seek ended at given position
     *
     * @param position media position in seconds where seek ended
     */
    fun endSeek(position: Int) = currentSession?.endSeek(position)

    fun play() = currentSession?.play()

    fun pause() = currentSession?.pause()

    fun stop() = currentSession?.stop()

    /**
     * Sends a custom event with current media content status
     *
     * @param event name of custom media event, this is the eventName used for
     * [TealiumEvent]
     */
    fun custom(event: String) = currentSession?.custom(event)

    /**
     * Updates the QoE bitrate value of the current session
     *
     * @param rate
     */
    fun updateBitrate(rate: Int) = currentSession?.updateBitrate(rate)

    /**
     * Updates the QoE dropped frames value of the current session
     *
     * @param frames
     */
    fun updateDroppedFrames(frames: Int) = currentSession?.updateDroppedFrames(frames)

    /**
     * Updates the QoE playback speed value of the current session
     *
     * @param speed
     */
    fun updatePlaybackSpeed(speed: Double) = currentSession?.updatePlaybackSpeed(speed)

    /**
     * Updates the player state for current media session
     *
     * @param state latest [PlayerState]
     */
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