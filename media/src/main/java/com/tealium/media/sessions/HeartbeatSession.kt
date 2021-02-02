package com.tealium.media.sessions

import com.tealium.media.*
import java.util.*

/**
 * Session sends recorded standard events, as well as a "ping" every 10 seconds to record media
 * is playing
 */
class HeartbeatSession(private val mediaContent: MediaContent,
                       private val mediaDispatcher: MediaDispatcher,
                       private val interval: Long = Media.DEFAULT_HEARTBEAT_INTERVAL) : SignificantEventsSession(mediaContent, mediaDispatcher) {

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
        // TODO isDaemon = true? per docs, Timer schedules tasks on a background thread
        timer = Timer("heartbeat").apply {
            scheduleAtFixedRate(
                    // check timeElapsed every 1 second
                    object : TimerTask() {
                        override fun run() {
                            ping()
                        }
                    }, 0, interval
            )
        }
    }
}