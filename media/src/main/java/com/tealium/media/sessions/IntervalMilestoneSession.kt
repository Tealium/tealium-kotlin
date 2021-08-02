package com.tealium.media.sessions

import com.tealium.media.Media
import com.tealium.media.MediaContent
import com.tealium.media.MediaEvent
import com.tealium.media.MediaDispatcher

/**
 * Combination of Interval and Milestone sessions. Sends recorded media details for standard
 * events, milestones, and pings every 10 seconds
 */
class IntervalMilestoneSession(
    private val mediaContent: MediaContent,
    private val mediaDispatcher: MediaDispatcher
) : MilestoneSession(mediaContent, mediaDispatcher) {

    private val interval: Long = Media.DEFAULT_SESSION_INTERVAL
    private var intervalCount = 0

    override fun ping() {
        if (totalContentPlayed.times(1000).div(interval) > intervalCount) {
            intervalCount++
            mediaDispatcher.track(MediaEvent.INTERVAL, mediaContent)
        }
        super.ping()
    }
}