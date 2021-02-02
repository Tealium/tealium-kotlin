package com.tealium.media.sessions

import com.tealium.media.Media
import com.tealium.media.MediaContent
import com.tealium.media.MediaEvent
import com.tealium.media.MediaDispatcher

/**
 * Combination of Heartbeat and Milestone sessions. Sends recorded media details for standard
 * events, milestones, and pings every 10 seconds
 */
class HeartbeatMilestoneSession(private val mediaContent: MediaContent,
                                private val mediaDispatcher: MediaDispatcher,
                                private val interval: Long = Media.DEFAULT_HEARTBEAT_INTERVAL) : MilestoneSession(mediaContent, mediaDispatcher) {

    override fun ping() {
        if (delta()?.rem(interval) == 0L) {
            mediaDispatcher.track(MediaEvent.HEARTBEAT, mediaContent)
        }
        super.ping()
    }
}