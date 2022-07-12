package com.tealium.media.v2.messaging

import com.tealium.core.messaging.Listener
import com.tealium.media.segments.Ad
import com.tealium.media.segments.AdBreak
import com.tealium.media.segments.Chapter

interface MediaListener: Listener

// TODO - consider combining listeners. e.g. `PlaybackListener` to implement all of Play/Pause/Buffer etc etc to simplify implementation

interface OnStartSessionListener: MediaListener {
    fun onStartSession()
}

interface OnResumeSessionListener: MediaListener {
    fun onResumeSession()
}

interface OnPlayListener : MediaListener {
    fun onPlay()
}

interface OnPausedListener : MediaListener {
    fun onPaused()
}

interface OnStartChapterListener : MediaListener {
    fun onStartChapter(chapter: Chapter)
}

interface OnSkipChapterListener : MediaListener {
    fun onSkipChapter()
}

interface OnEndChapterListener : MediaListener {
    fun onEndChapter()
}

interface OnStartBufferListener : MediaListener {
    fun onStartBuffer()
}

interface OnEndBufferListener : MediaListener {
    fun onEndBuffer()
}

interface OnStartSeekListener : MediaListener {
    fun onStartSeek(startSeekTime: Int?)
}

interface OnEndSeekListener : MediaListener {
    fun onEndSeek(endSeekTime: Int?)
}

interface OnStartAdBreakListener : MediaListener {
    fun onStartAdBreak(adBreak: AdBreak)
}

interface OnEndAdBreakListener : MediaListener {
    fun onEndAdBreak()
}

interface OnStartAdListener : MediaListener {
    fun onStartAd(ad: Ad)
}

interface OnClickAdListener : MediaListener {
    fun onClickAd()
}

interface OnSkipAdListener : MediaListener {
    fun onSkipAd()
}

interface OnEndAdListener : MediaListener {
    fun onEndAd()
}

interface OnEndContentListener : MediaListener {
    fun onEndContent()
}

interface OnEndSessionListener : MediaListener {
    fun onEndSession()
}

interface OnCustomEventListener : MediaListener {
    fun onCustomEvent()
}