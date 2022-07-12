package com.tealium.media.v2.plugins

import android.util.Log
import com.tealium.core.Logger
import com.tealium.media.*
import com.tealium.media.segments.AdBreak
import com.tealium.media.v2.*
import com.tealium.media.v2.messaging.*
import java.util.*

class MilestonePlugin(
    private val mediaSessionDataProvider: MediaSessionDataProvider,
    private val tracker: MediaDispatcher,
    private val interval: Long = DEFAULT_MILESTONE_INTERVAL
) : MediaSessionPlugin,
    OnEndSessionListener,
    OnPlayListener,
    OnPausedListener,
    OnStartAdBreakListener,
    OnEndAdBreakListener,
    OnStartSeekListener,
    OnEndSeekListener,
    OnEndContentListener
{
    private val TAG = "${BuildConfig.TAG}-Milestone"

    private val contentDuration: Long? = mediaSessionDataProvider.metadata.duration?.toLong()
    private var timer: Timer? = null
    val totalContentPlayed: Double
        get() {
            return lastPlayTimestamp?.let {
                totalPlaybackTime = Media.timeMillisToSeconds(System.currentTimeMillis() - it)
                totalPlaybackTime
            } ?: totalPlaybackTime
        }
    private var totalPlaybackTime: Double = 0.0
    private var lastPlayTimestamp: Long? = null
    private var startSeekPosition: Int? = null

    private val milestonesAchieved = mutableSetOf<Milestone>()

    private fun ping() {
        checkMilestone()?.let {
            sendMilestone(it)

            // TODO - onEndContent called by user?
//            if (it == Milestone.ONE_HUNDRED) {
//                onEndContent()
//            }
        }
    }

    // TODO -> not required?
//    fun stopPing() {
//        timer?.cancel()
//    }

    override fun onEndSession() {
        cancelTimer()
    }

    override fun onEndContent() {
        cancelTimer()
    }

    override fun onPlay() {
        if (lastPlayTimestamp == null) {
            lastPlayTimestamp = System.currentTimeMillis()
        }
        startTimer()
    }

    override fun onPaused() {
        processPause()
        cancelTimer()
    }

    override fun onStartAdBreak(adBreak: AdBreak) {
        processPause()
        cancelTimer()
    }

    override fun onEndAdBreak() {
        if (lastPlayTimestamp == null) {
            lastPlayTimestamp = System.currentTimeMillis()
        }
    }

    override fun onStartSeek(startSeekTime: Int?) {
        startSeekPosition = startSeekTime
        cancelTimer()
    }

    override fun onEndSeek(endSeekTime: Int?) {
        endSeekTime?.let { endPos ->
            startSeekPosition?.let { startPos ->
                totalPlaybackTime += endPos - startPos
            }
        }

        milestonesAchieved.clear()
        startTimer()
    }

    private fun sendMilestone(milestone: Milestone) {
        Log.d(TAG, "Milestone reached: ${milestone.value}")

        mediaSessionDataProvider.dataLayer.put(SessionKey.MILESTONE, milestone.value)
        tracker.track(MediaEvent.MILESTONE, mediaSessionDataProvider.getTrackingData())
    }

    private fun startTimer() {
        timer = Timer("Milestone", true).apply {
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

    private fun checkMilestone(): Milestone? {
        val percentagePlayed = percentageContentPlayed()
        val milestone = when {
            percentagePlayed >= 100.0 -> Milestone.ONE_HUNDRED
            percentagePlayed > 90.0 -> Milestone.NINETY
            percentagePlayed > 75.0 -> Milestone.SEVENTY_FIVE
            percentagePlayed > 50.0 -> Milestone.FIFTY
            percentagePlayed > 25.0 -> Milestone.TWENTY_FIVE
            percentagePlayed > 10.0 -> Milestone.TEN
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

    companion object Factory : ConfigurableMediaPluginFactory<Long?> {
        const val DEFAULT_MILESTONE_INTERVAL = 1000L

        override fun configure(config: Long?): MediaPluginFactory {
            return object : MediaPluginFactory {
                override fun create(
                    mediaSessionDataProvider: MediaSessionDataProvider,
                    events: MediaSessionEvents,
                    tracker: MediaDispatcher
                ): MediaSessionPlugin {
                    return MilestonePlugin(mediaSessionDataProvider, tracker, config ?: DEFAULT_MILESTONE_INTERVAL)
                }
            }
        }
    }
}