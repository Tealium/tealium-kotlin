package com.tealium.media.v2.plugins

import android.util.Log
import com.tealium.media.*
import com.tealium.media.segments.Ad
import com.tealium.media.segments.Chapter
import com.tealium.media.v2.*
import com.tealium.media.v2.messaging.*
import java.text.SimpleDateFormat
import java.util.*

class SummaryPlugin(
    private val mediaSessionDataProvider: MediaSessionDataProvider,
    private val events: MediaSessionEvents,
    private val tracker: MediaDispatcher
) : MediaSessionPlugin,
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
    OnEndContentListener {
    private val TAG = "${BuildConfig.TAG}-Summary"

    private var _summary = MediaSummary()

    override fun onStartSession() {
        Log.d(TAG, ":: Start Session")
        _summary.sessionStartTime = System.currentTimeMillis()
    }

    override fun onResumeSession() {
        //TODO("Not yet implemented")
        Log.d(TAG, ":: Session Resumed")
    }

    override fun onPlay() {
        Log.d(TAG, ":: Play")
        _summary.playStartTime = System.currentTimeMillis()
        _summary.plays++
    }

    override fun onPaused() {
        Log.d(TAG, ":: Paused")
        _summary.pauses++
        _summary.playStartTime?.let { start ->
            val timeElapsed = Media.timeMillisToSeconds(System.currentTimeMillis() - start)
            _summary.totalPlayTime?.let {
                _summary.totalPlayTime = it + timeElapsed
            }
        }
    }

    override fun onStartChapter(chapter: Chapter) {
        Log.d(TAG, ":: Start Chapter")
        _summary.chapterStarts++
    }

    override fun onSkipChapter() {
        Log.d(TAG, ":: Skip Chapter")
        _summary.chapterSkips++
        _summary.chapterEnds++
    }

    override fun onEndChapter() {
        Log.d(TAG, ":: End Chapter")
        _summary.chapterEnds++
    }

    override fun onStartBuffer() {
        Log.d(TAG, ":: Start Buffer")
        _summary.bufferStartTime = System.currentTimeMillis()
    }

    override fun onEndBuffer() {
        Log.d(TAG, ":: End Buffer")
        _summary.bufferStartTime?.let { start ->
            val timeElapsed = Media.timeMillisToSeconds(System.currentTimeMillis() - start)
            _summary.totalBufferTime?.let {
                _summary.totalBufferTime = it + timeElapsed
            }
        }
    }

    override fun onStartSeek(startSeekTime: Int?) {
        Log.d(TAG, ":: Start Seek")
        _summary.seekStartTime = System.currentTimeMillis()
    }

    override fun onEndSeek(endSeekTime: Int?) {
        Log.d(TAG, ":: End Seek")
        _summary.seekStartTime?.let { start ->
            val timeElapse = Media.timeMillisToSeconds(System.currentTimeMillis() - start)
            _summary.totalSeekTime?.let {
                _summary.totalSeekTime = it + timeElapse
            }
        }
    }

//    override fun onStartAdBreak(adBreak: AdBreak) {
//        // no-op
//    }
//
//    override fun onEndAdBreak() {
//        // no-op
//    }

    override fun onStartAd(ad: Ad) {
        Log.d(TAG, ":: Start Ad")
        _summary.adStartTime = System.currentTimeMillis()
        _summary.adUuids.add(ad.uuid)
        _summary.ads++
    }

    override fun onClickAd() {
        Log.d(TAG, ":: Click Ad")
        // TODO( no ad clicks in MediaSummary)
    }

    override fun onSkipAd() {
        Log.d(TAG, ":: Skip Ad")
        _summary.adSkips++
        _summary.adEnds++
        _summary.adStartTime?.let {
            val timeElapsed = Media.timeMillisToSeconds(System.currentTimeMillis() - it)
            _summary.totalAdTime = it + timeElapsed
        }
    }

    override fun onEndAd() {
        Log.d(TAG, ":: End Ad")
        _summary.adEnds++
        _summary.adStartTime?.let {
            val timeElapsed = Media.timeMillisToSeconds(System.currentTimeMillis() - it)
            _summary.totalAdTime = it + timeElapsed
        }
    }

    override fun onEndContent() {
        Log.d(TAG, ":: End Content")
        _summary.playToEnd = true
        _summary.playStartTime?.let {
            _summary.totalPlayTime =
                Media.timeMillisToSeconds(System.currentTimeMillis() - it)
        }
    }

    override fun onEndSession() {
        Log.d(TAG, ":: End Session")
        _summary.sessionEndTime = System.currentTimeMillis()

        reusableDate.time = _summary.sessionStartTime
        _summary.sessionStartTimestamp = formatIso8601.format(reusableDate)

        _summary.sessionEndTime?.let {
            _summary.duration = Media.timeMillisToSeconds(it.minus(_summary.sessionStartTime))
        }

        _summary.sessionEndTime?.let { endTime ->
            reusableDate.time = endTime
            _summary.sessionEndTimestamp = formatIso8601.format(reusableDate)
        }

        if (_summary.chapterStarts > 0) {
            _summary.percentageChapterComplete = percentage(_summary.chapterEnds, _summary.chapterStarts)
        }

        if (_summary.ads > 0) {
            _summary.percentageAdComplete = percentage(_summary.adEnds, _summary.ads)
        }

        _summary.totalAdTime?.let { adTime ->
            if (adTime > 0) {
                _summary.totalPlayTime?.let { totalTime ->
                    _summary.percentageAdTime = percentage(adTime.toInt(), totalTime.toInt())
                }
            }
        }

        tracker.track(MediaEvent.SESSION_END, mutableMapOf<String, Any>().apply {
            putAll(mediaSessionDataProvider.getTrackingData())
            putAll(_summary.toMap())
        })
    }

    companion object Factory : MediaPluginFactory {
        const val FORMAT_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'"
        const val TIMESTAMP_INVALID = Long.MIN_VALUE

        override fun create(
            mediaSessionDataProvider: MediaSessionDataProvider,
            events: MediaSessionEvents,
            tracker: MediaDispatcher
        ): MediaSessionPlugin {
            return SummaryPlugin(mediaSessionDataProvider, events, tracker)
        }
    }

    private fun percentage(count: Int, total: Int): Double {
        return ((count.toDouble() / total.toDouble()) * 100)
    }

    private var formatIso8601 = SimpleDateFormat(FORMAT_ISO_8601, Locale.ROOT)
    private var reusableDate: Date = Date(TIMESTAMP_INVALID)
}