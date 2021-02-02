package com.tealium.media.sessions

import com.tealium.media.MediaContent
import com.tealium.media.MediaEvent
import com.tealium.media.MediaDispatcher
import com.tealium.media.Milestone
import java.util.*

/**
 * Session sends recorded media details for standard events, as well as milestones reached for
 * 10%, 25%, 50%, 75%, 90%, 100% of video content played.
 */
open class MilestoneSession(private val mediaContent: MediaContent,
                            private val mediaDispatcher: MediaDispatcher,
                            private val contentDuration: Long? = mediaContent.duration?.toLong()) : SignificantEventsSession(mediaContent, mediaDispatcher) {

    private var timer: Timer? = null
    private var startPauseTime: Long? = null
    private var pauseTime : Long = 0
    private val milestonesAchieved = mutableSetOf<Milestone>()

    override fun ping() {
        checkMilestone()?.let {
            mediaContent.milestone = it
            sendMilestone()
        }
    }

    override fun stopPing() {
        timer?.cancel()
    }

    override fun startSession() {
        startTimer()
        super.startSession()
    }

    override fun endSession() {
        timer?.cancel()
        super.endSession()
    }

    override fun play() {
        if (timer == null) {
            resumeTimer()
            startTimer()
        }
        super.play()
    }

    override fun pause() {
        pauseTimer()
        timer?.cancel()
        timer = null
        super.pause()
    }

    override fun sendMilestone() {
        mediaDispatcher.track(MediaEvent.MILESTONE, mediaContent)
    }

    private fun startTimer() {
        timer = Timer("Milestone").apply {
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
        val milestone = when (percentageElapsed()) {
            in 8.0..12.0 -> Milestone.TEN
            in 23.0..27.0 -> Milestone.TWENTY_FIVE
            in 48.0..52.0 -> Milestone.FIFTY
            in 73.0..77.0 -> Milestone.SEVENTY_FIVE
            in 88.0..92.0 -> Milestone.NINETY
            in 97.0..100.0 -> Milestone.ONE_HUNDRED
            else -> null
        }
        milestone?.let {
            return if(milestonesAchieved.add(it)) it else null
        }
        return null
    }

    private fun percentageElapsed(): Double {
        contentDuration?.let {length ->
            delta()?.let { timeDifference ->
                val elapsedSeconds = (timeDifference - pauseTime) / 1000.0
                return (elapsedSeconds / length) * 100
            }
        }

        return 0.0
    }

    open fun delta(): Long? {
        return mediaContent.startTime?.let {
            System.currentTimeMillis() - it
        }
    }

    private fun pauseTimer() {
        startPauseTime = System.currentTimeMillis()
    }

    private fun resumeTimer() {
        startPauseTime?.let {
            pauseTime += System.currentTimeMillis() - it
        }
    }

    companion object {
        const val DEFAULT_MILESTONE_INTERVAL = 1000L
    }
}