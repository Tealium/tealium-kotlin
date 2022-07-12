package com.tealium.media.v2

import android.util.Log
import com.tealium.media.MediaDispatcher
import com.tealium.media.MediaSessionDispatcher
import com.tealium.media.PlayerState
import com.tealium.media.segments.Ad
import com.tealium.media.segments.AdBreak
import com.tealium.media.segments.Chapter
import com.tealium.media.sessions.Session
import com.tealium.media.v2.messaging.*

class MediaSession(
    private val plugins: Set<MediaSessionPlugin>,
    private val messageRouter: MediaMessageRouter = MediaMessageDispatcher(),
) : Session {

    init {
        Log.d("Tealium-MediaSession", "Media Session initialized with plugin : ${
            plugins.joinToString(
                ", "
            ) { it.javaClass.simpleName }
        }")

        plugins.filterIsInstance(
            MediaListener::class.java
        ).toSet().forEach {
            messageRouter.subscribe(it)
        }
    }

    override var isBackgrounded: Boolean = false // todo: delegate

    override fun startSession() {
        messageRouter.send(OnStartSessionMessenger())
    }

    override fun resumeSession() {
        messageRouter.send(OnResumeSessionMessenger())
    }

    override fun endSession() {
        messageRouter.send(OnEndSessionMessenger())
    }

    override fun endContent() {
        messageRouter.send(OnEndContentMessenger())
    }

    override fun startBuffer() {
        messageRouter.send(OnStartBufferMessenger())
    }

    override fun endBuffer() {
        messageRouter.send(OnEndBufferMessenger())
    }

    override fun play() {
        messageRouter.send(OnPlayMessenger())
    }

    override fun pause() {
        messageRouter.send(OnPausedMessenger())
    }

    override fun startChapter(chapter: Chapter) {
        messageRouter.send(OnStartChapterMessenger(chapter))
    }

    override fun skipChapter() {
        messageRouter.send(OnSkipChapterMessenger())
    }

    override fun endChapter() {
        messageRouter.send(OnEndChapterMessenger())
    }

    override fun startSeek(position: Int) {
        messageRouter.send(OnStartSeekMessenger(position))
    }

    override fun endSeek(position: Int) {
        messageRouter.send(OnEndSeekMessenger(position))
    }

    override fun startAdBreak(adBreak: AdBreak) {
        messageRouter.send(OnStartAdBreakMessenger(adBreak))
    }

    override fun endAdBreak() {
        messageRouter.send(OnEndAdBreakMessenger())
    }

    override fun startAd(ad: Ad) {
        messageRouter.send(OnStartAdMessenger(ad))
    }

    override fun clickAd() {
        messageRouter.send(OnClickAdMessenger())
    }

    override fun skipAd() {
        messageRouter.send(OnSkipAdMessenger())
    }

    override fun endAd() {
        messageRouter.send(OnEndAdMessenger())
    }

    override fun updateBitrate(rate: Int) {
        // no-op
    }

    override fun updateDroppedFrames(frames: Int) {
        // no-op
    }

    override fun updatePlaybackSpeed(speed: Double) {
        // no-op
    }

    override fun updatePlayerState(state: PlayerState) {
        // no-op
    }

    override fun custom(event: String) {
        messageRouter.send(OnCustomEventMessenger(event))
    }

    override fun sendMilestone() {
        // no-op
    }

    override fun finalizeSummaryInfo() {
        // no-op
    }
}