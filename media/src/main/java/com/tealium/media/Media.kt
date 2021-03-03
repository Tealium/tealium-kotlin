package com.tealium.media

import android.app.Activity
import com.tealium.core.*
import com.tealium.core.messaging.ActivityObserverListener
import com.tealium.media.segments.Ad
import com.tealium.media.segments.AdBreak
import com.tealium.media.segments.Chapter
import com.tealium.media.sessions.*
import com.tealium.media.sessions.Session
import java.util.*

/**
 * Media module provides an easy way to track a media session and its components, such as ad breaks,
 * ads, and chapters. Tracking type options include Significant Events, Milestone, Heartbeat,
 * Heartbeat + Milestone, and Summary.
 */
class Media(private val context: TealiumContext,
            private val mediaDispatcher: MediaDispatcher = MediaSessionDispatcher(context)) : Module, ActivityObserverListener {

    override val name: String = MODULE_NAME
    override var enabled: Boolean = true

    var currentSession: Session? = null
        private set

    private var endSessionTimer: Timer = Timer("End Media", true)
    private var timerTask: TimerTask? = null
    private var activityCount = 0

    private val backgroundSessionTrackingEnabled: Boolean = context.config.mediaBackgroundSessionEnabled
            ?: false
    private val backgroundEndSessionInterval: Long = context.config.mediaBackgroundSessionEndInterval
            ?: DEFAULT_END_SESSION_INTERVAL

    /**
     * Begins a session for given [MediaContent] based on the [TrackingType] provided
     */
    fun startSession(media: MediaContent) {
        // if a session exists and is in background, resume it
        if (currentSession?.isBackgrounded == true) {
            resumeSession()
            return
        }

        currentSession = when (media.trackingType) {
            TrackingType.HEARTBEAT -> HeartbeatSession(media, mediaDispatcher)
            TrackingType.MILESTONE -> MilestoneSession(media, mediaDispatcher)
            TrackingType.HEARTBEAT_MILESTONE -> HeartbeatMilestoneSession(media, mediaDispatcher)

            TrackingType.SUMMARY -> SummarySession(media, mediaDispatcher)
            else -> SignificantEventsSession(media, mediaDispatcher)
        }
        currentSession?.let {
            Logger.dev(BuildConfig.TAG, "Starting Media Session for: ${media.name}.")
            it.startSession()
        }
    }

    fun resumeSession() {
        currentSession?.isBackgrounded = false
        currentSession?.resumeSession()
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
     * Records end of MediaContent
     */
    fun endContent() {
        currentSession?.endContent()
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
     * Starts an ad segment with given [Ad]
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
     * Starts a chapter segment with given [Chapter]
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

    /**
     * Records play event for media session
     */
    fun play() = currentSession?.play()

    /**
     * Records pause event for media session
     */
    fun pause() = currentSession?.pause()

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

    override fun onActivityPaused(activity: Activity?) {
        // do nothing
    }

    override fun onActivityResumed(activity: Activity?) {
        ++activityCount

        if (currentSession?.isBackgrounded == true) {
            cancelTimerTask()
            resumeSession()
        }
    }

    override fun onActivityStopped(activity: Activity?, isChangingConfiguration: Boolean) {
        --activityCount

        // background media tracking is not enabled, start timer to end session
        if (activityCount == 0 && !isChangingConfiguration && shouldStartEndSessionTimer()) {
            currentSession?.isBackgrounded = true
            endSessionTimer.schedule(createTimerTask(), backgroundEndSessionInterval)
        }
    }

    private fun shouldStartEndSessionTimer(): Boolean {
        return !backgroundSessionTrackingEnabled && currentSession?.isBackgrounded == false
    }

    private fun createTimerTask(): TimerTask {
        timerTask = object : TimerTask() {
            override fun run() {
                endSession()
            }
        }

        return timerTask as TimerTask
    }

    private fun cancelTimerTask() {
        timerTask?.cancel()
        timerTask = null
    }

    companion object : ModuleFactory {
        const val MODULE_NAME = "MEDIA_SERVICE"
        const val DEFAULT_HEARTBEAT_INTERVAL = 10000L // ten seconds
        const val DEFAULT_END_SESSION_INTERVAL = 60000L // one minute

        override fun create(context: TealiumContext): Module {
            return Media(context)
        }

        fun timeMillisToSeconds(time: Long): Double {
            return time.toDouble() / 1000
        }
    }
}