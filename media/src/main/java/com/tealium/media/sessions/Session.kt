package com.tealium.media.sessions

import com.tealium.media.PlayerState
import com.tealium.media.segments.Ad
import com.tealium.media.segments.AdBreak
import com.tealium.media.segments.Chapter

interface Session {
    fun startSession()
    fun endSession()

    // Significant Events
    fun startBuffer()
    fun endBuffer()

    fun play()
    fun pause()
    fun stop()

    fun startChapter(chapter: Chapter)
    fun skipChapter()
    fun endChapter()

    fun startSeek(position: Int)
    fun endSeek(position: Int)

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