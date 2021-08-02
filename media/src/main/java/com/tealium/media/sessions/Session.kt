package com.tealium.media.sessions

import com.tealium.media.PlayerState
import com.tealium.media.segments.Ad
import com.tealium.media.segments.AdBreak
import com.tealium.media.segments.Chapter

interface Session {
    var isBackgrounded: Boolean

    fun startSession()
    fun resumeSession()
    fun endSession()
    fun endContent()

    // Full Playback Events
    fun startBuffer()
    fun endBuffer()

    fun play(data: Map<String, Any>? = null)
    fun pause(data: Map<String, Any>? = null)

    fun startChapter(chapter: Chapter)
    fun skipChapter()
    fun endChapter()

    fun startSeek(position: Int, data: Map<String, Any>? = null)
    fun endSeek(position: Int, data: Map<String, Any>? = null)

    fun startAdBreak(adBreak: AdBreak)
    fun endAdBreak()

    fun startAd(ad: Ad)
    fun clickAd()
    fun skipAd()
    fun endAd()

    fun updateBitrate(rate: Int)
    fun updateDroppedFrames(frames: Int)
    fun updatePlaybackSpeed(speed: Double)
    fun updatePlayerState(state: PlayerState)

    fun custom(event: String)

    fun sendMilestone()
    fun finalizeSummaryInfo()

    fun ping() {}
    fun stopPing() {}
}