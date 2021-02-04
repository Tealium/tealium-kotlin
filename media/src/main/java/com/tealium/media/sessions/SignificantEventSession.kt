package com.tealium.media.sessions

import com.tealium.core.Logger
import com.tealium.media.*
import com.tealium.media.segments.Ad
import com.tealium.media.segments.AdBreak
import com.tealium.media.segments.Chapter

/**
 * Session sends media details at significant events, like play, pause, stop, etc.
 */
open class SignificantEventsSession(private val mediaContent: MediaContent,
                                    private val mediaDispatcher: MediaDispatcher) : Session {

    override fun startSession() {
        mediaContent.startTime = System.currentTimeMillis()
        mediaDispatcher.track(MediaEvent.SESSION_START, mediaContent)
    }

    override fun endSession() {
        mediaContent.endTime = duration()
        mediaDispatcher.track(MediaEvent.SESSION_END, mediaContent)
    }

    override fun startAdBreak(adBreak: AdBreak) {
        adBreak.start()
        mediaContent.adBreakList.add(adBreak)
        mediaDispatcher.track(MediaEvent.ADBREAK_START, mediaContent, adBreak)
    }

    override fun endAdBreak() {
        if (mediaContent.adBreakList.isNotEmpty()) {
            val adBreak = mediaContent.adBreakList.last()
            adBreak.end()
            mediaDispatcher.track(MediaEvent.ADBREAK_COMPLETE, mediaContent, adBreak)
        }
    }

    override fun startAd(ad: Ad) {
        ad.start()
        mediaContent.adList.add(ad)
        mediaDispatcher.track(MediaEvent.AD_START, mediaContent, ad)
    }

    override fun clickAd() {
        if (mediaContent.adList.isNotEmpty()) {
            val ad = mediaContent.adList.last()
            mediaDispatcher.track(MediaEvent.AD_CLICK, mediaContent, ad)
        }
    }

    override fun endAd() {
        if (mediaContent.adList.isNotEmpty()) {
            val ad = mediaContent.adList.last()
            ad.end()
            mediaDispatcher.track(MediaEvent.AD_COMPLETE, mediaContent, ad)
        }
    }

    override fun skipAd() {
        if (mediaContent.adList.isNotEmpty()) {
            val ad = mediaContent.adList.last()
            mediaDispatcher.track(MediaEvent.AD_SKIP, mediaContent, ad)
        }
    }

    override fun startChapter(chapter: Chapter) {
        chapter.start()
        mediaContent.chapterList.add(chapter)
        mediaDispatcher.track(MediaEvent.CHAPTER_START, mediaContent, chapter)
    }

    override fun endChapter() {
        if (mediaContent.chapterList.isNotEmpty()) {
            val chapter = mediaContent.chapterList.last()
            chapter.end()
            mediaDispatcher.track(MediaEvent.CHAPTER_COMPLETE, mediaContent, chapter)
        }
    }

    override fun skipChapter() {
        if (mediaContent.chapterList.isNotEmpty()) {
            val chapter = mediaContent.chapterList.last()
            mediaDispatcher.track(MediaEvent.CHAPTER_SKIP, mediaContent, chapter)
        }
    }

    override fun startBuffer() {
        mediaDispatcher.track(MediaEvent.BUFFER_START, mediaContent)
    }

    override fun endBuffer() {
        mediaDispatcher.track(MediaEvent.BUFFER_COMPLETE, mediaContent)
    }

    override fun play() {
        mediaDispatcher.track(MediaEvent.PLAY, mediaContent)
    }

    override fun pause() {
        mediaDispatcher.track(MediaEvent.PAUSE, mediaContent)
    }

    override fun stop() {
        mediaDispatcher.track(MediaEvent.STOP, mediaContent)
    }

    override fun startSeek(position: Int) {
        mediaDispatcher.track(MediaEvent.SEEK_START, mediaContent)
    }

    override fun endSeek(position: Int) {
        mediaDispatcher.track(MediaEvent.SEEK_COMPLETE, mediaContent)
    }

    override fun updateBitrate(rate: Int) {
        mediaContent.qoe.bitrate = rate
        mediaDispatcher.track(MediaEvent.BITRATE_CHANGE, mediaContent)
    }

    override fun updateDroppedFrames(frames: Int) {
        mediaContent.qoe.droppedFrames = frames
    }

    override fun updatePlaybackSpeed(speed: Double) {
        mediaContent.qoe.playbackSpeed = speed
    }

    override fun updatePlayerState(state: PlayerState) {
        mediaContent.state?.let {
            mediaDispatcher.track(MediaEvent.PLAYER_STATE_STOP, mediaContent)
            mediaContent.state = state
            mediaDispatcher.track(MediaEvent.PLAYER_STATE_START, mediaContent)
        } ?: run {
            mediaContent.state = state
            mediaDispatcher.track(MediaEvent.PLAYER_STATE_START, mediaContent)
        }
    }

    override fun custom(event: String) {
        mediaDispatcher.track(event, mediaContent)
    }

    override fun sendMilestone() {
        // do nothing
    }

    override fun finalizeSummaryInfo() {
        // do nothing
    }

    private fun duration(): Long? {
        return mediaContent.startTime?.let {
            System.currentTimeMillis() - it
        }
    }
}