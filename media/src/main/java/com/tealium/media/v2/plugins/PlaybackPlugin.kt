package com.tealium.media.v2.plugins

import com.tealium.media.MediaDispatcher
import com.tealium.media.MediaSummary
import com.tealium.media.segments.Ad
import com.tealium.media.segments.Chapter
import com.tealium.media.v2.*
import com.tealium.media.v2.messaging.*

class PlaybackPlugin(
    private val mediaSessionDataProvider: MediaSessionDataProvider,
    private val events: MediaSessionEvents,
    private val tracker: MediaDispatcher
): MediaSessionPlugin,
    OnStartSessionListener,
    OnResumeSessionListener,
    OnEndSessionListener,
    OnPlayListener,
    OnPausedListener,
    OnStartAdListener,
    OnSkipAdListener,
    OnClickAdListener,
    OnEndAdListener,
//    OnStartAdBreakListener,
//    OnEndAdBreakListener,
    OnStartChapterListener,
    OnEndChapterListener,
    OnSkipChapterListener,
    OnStartSeekListener,
    OnEndSeekListener,
    OnStartBufferListener,
    OnEndBufferListener,
    OnEndContentListener
{
    override fun onStartSession() {
        TODO("Not yet implemented")
    }

    override fun onResumeSession() {
        TODO("Not yet implemented")
    }

    override fun onPlay() {
        TODO("Not yet implemented")
    }

    override fun onPaused() {
        TODO("Not yet implemented")
    }

    override fun onStartChapter(chapter: Chapter) {
        TODO("Not yet implemented")
    }

    override fun onSkipChapter() {
        TODO("Not yet implemented")
    }

    override fun onEndChapter() {
        TODO("Not yet implemented")
    }

    override fun onStartBuffer() {
        TODO("Not yet implemented")
    }

    override fun onEndBuffer() {
        TODO("Not yet implemented")
    }

    override fun onStartSeek(startSeekTime: Int?) {
        TODO("Not yet implemented")
    }

    override fun onEndSeek(endSeekTime: Int?) {
        TODO("Not yet implemented")
    }

    override fun onStartAd(ad: Ad) {
        TODO("Not yet implemented")
    }

    override fun onClickAd() {
        TODO("Not yet implemented")
    }

    override fun onSkipAd() {
        TODO("Not yet implemented")
    }

    override fun onEndAd() {
        TODO("Not yet implemented")
    }

    override fun onEndContent() {
        TODO("Not yet implemented")
    }

    override fun onEndSession() {
        TODO("Not yet implemented")
    }

    companion object Factory: MediaPluginFactory {
        override fun create(
            mediaSessionDataProvider: MediaSessionDataProvider,
            events: MediaSessionEvents,
            tracker: MediaDispatcher
        ): MediaSessionPlugin {
            return PlaybackPlugin(mediaSessionDataProvider, events, tracker)
        }
    }
}