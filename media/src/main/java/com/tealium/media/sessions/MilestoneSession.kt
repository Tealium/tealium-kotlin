package com.tealium.media.sessions

import com.tealium.core.Logger
import com.tealium.media.*
import com.tealium.media.segments.AdBreak
import java.util.*

/**
 * Session sends recorded media details for standard events, as well as milestones reached for
 * 10%, 25%, 50%, 75%, 90%, 100% of video content played.
 */
open class MilestoneSession(private val mediaContent: MediaContent,
                            private val mediaDispatcher: MediaDispatcher) : SignificantEventsSession(mediaContent, mediaDispatcher) {

    private val contentDuration: Long? = mediaContent.duration?.toLong()
    private var timer: Timer? = null
    val totalContentPlayed: Double
        get() {
            return lastPlayTimestamp?.let {
                val timeElapsed = Media.timeMillisToSeconds(System.currentTimeMillis() - it)
                totalPlaybackTime += timeElapsed
                totalPlaybackTime
            } ?: totalPlaybackTime
        }
    private var totalPlaybackTime: Double = 0.0
    private var lastPlayTimestamp: Long? = null
    private var startSeekPosition: Int? = null

    private val milestonesAchieved = mutableSetOf<Milestone>()

    override fun ping() {
        checkMilestone()?.let {
            mediaContent.milestone = it
            sendMilestone()
            if (it == Milestone.ONE_HUNDRED) {
                endContent()
            }
        }
    }

    override fun stopPing() {
        timer?.cancel()
    }

    override fun endSession() {
        cancelTimer()
        super.endSession()
    }

    override fun endContent() {
        cancelTimer()
        super.endContent()
    }

    override fun play() {
        if (lastPlayTimestamp == null) {
            lastPlayTimestamp = System.currentTimeMillis()
        }
        startTimer()
        super.play()
    }

    override fun pause() {
        processPause()
        cancelTimer()
        super.pause()
    }

    override fun startAdBreak(adBreak: AdBreak) {
        processPause()
        cancelTimer()
        super.startAdBreak(adBreak)
    }

    override fun endAdBreak() {
        if (lastPlayTimestamp == null) {
            lastPlayTimestamp = System.currentTimeMillis()
        }
        super.endAdBreak()
    }

    override fun startSeek(position: Int) {
        startSeekPosition = position
        cancelTimer()
        super.startSeek(0)
    }

    override fun endSeek(position: Int) {
        startSeekPosition?.let {
            totalPlaybackTime += position - it
        }

        milestonesAchieved.clear()
        startTimer()
        super.endSeek(0)
    }

    override fun sendMilestone() {
        mediaDispatcher.track(MediaEvent.MILESTONE, mediaContent)
    }

    private fun startTimer() {
        timer = Timer("Milestone", true).apply {
            scheduleAtFixedRate(
                    // check timeElapsed every 1 second
                    object : TimerTask() {
                        override fun run() {
                            ping()
                        }
                    }, 0, DEFAULT_MILESTONE_INTERVAL
            )
        }
    }

    private fun checkMilestone(): Milestone? {
        val milestone = when (percentageContentPlayed()) {
            in 8.0..12.0 -> Milestone.TEN
            in 23.0..27.0 -> Milestone.TWENTY_FIVE
            in 48.0..52.0 -> Milestone.FIFTY
            in 73.0..77.0 -> Milestone.SEVENTY_FIVE
            in 88.0..92.0 -> Milestone.NINETY
            in 97.0..100.0 -> Milestone.ONE_HUNDRED
            else -> null
        }
        milestone?.let {
            return if (milestonesAchieved.add(it)) it else null
        }
        return null
    }

    private fun percentageContentPlayed(): Double {
        contentDuration?.let { length ->
            return ((totalContentPlayed / length) * 100)
        }

        Logger.dev(BuildConfig.TAG, "Media Content duration required to send milestones")
        return 0.0
    }

    private fun cancelTimer() {
        timer?.cancel()
        timer = null
    }

    private fun processPause() {
        lastPlayTimestamp?.let {
            val timeElapsed = Media.timeMillisToSeconds(System.currentTimeMillis() - it)
            totalPlaybackTime += timeElapsed
            lastPlayTimestamp = null
        }
    }

    companion object {
        const val DEFAULT_MILESTONE_INTERVAL = 1000L
    }
}