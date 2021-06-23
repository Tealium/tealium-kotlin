package com.tealium.media.sessions

import com.tealium.media.*
import java.util.*

/**
 * Session sends recorded standard events, as well as a "ping" every 10 seconds to record media
 * is playing
 */
class IntervalSession(private val mediaContent: MediaContent,
                      private val mediaDispatcher: MediaDispatcher) : FullPlaybackSession(mediaContent, mediaDispatcher) {

    private val interval: Long = Media.DEFAULT_SESSION_INTERVAL
    private var timer: Timer? = null

    override fun ping() {
        mediaDispatcher.track(MediaEvent.INTERVAL, mediaContent)
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
        timer = Timer("interval", true).apply {
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