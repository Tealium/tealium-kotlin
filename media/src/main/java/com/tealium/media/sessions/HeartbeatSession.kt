package com.tealium.media.sessions

import com.tealium.media.*
import java.util.*

/**
 * Session sends recorded standard events, as well as a "ping" every 10 seconds to record media
 * is playing
 */
class HeartbeatSession(private val mediaContent: MediaContent,
                       private val mediaDispatcher: MediaDispatcher) : SignificantEventsSession(mediaContent, mediaDispatcher) {

    private val interval: Long = Media.DEFAULT_HEARTBEAT_INTERVAL
    private var timer: Timer? = null

    override fun ping() {
        mediaDispatcher.track(MediaEvent.HEARTBEAT, mediaContent)
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
            startTimer()
        }
        super.play()
    }

    override fun pause() {
        timer?.cancel()
        timer = null
        super.pause()
    }

    private fun startTimer() {
        timer = Timer("heartbeat", true).apply {
            scheduleAtFixedRate(
                    object : TimerTask() {
                        override fun run() {
                            ping()
                        }
                    }, 0, interval
            )
        }
    }
}