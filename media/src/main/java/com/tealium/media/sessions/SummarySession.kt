package com.tealium.media.sessions

import com.tealium.media.MediaContent
import com.tealium.media.MediaSummary
import com.tealium.media.MediaDispatcher
import com.tealium.media.segments.Ad
import com.tealium.media.segments.Chapter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Records session summary from startSession() to endSession(). Keeps track of recorded plays, pauses,
 * and stops of media. Details captured are sent after endSession() is called.
 */
class SummarySession(private val mediaContent: MediaContent,
                     private val mediaDispatcher: MediaDispatcher) : SignificantEventsSession(mediaContent, mediaDispatcher) {

    override fun startSession() {
        mediaContent.summary = MediaSummary()
        super.startSession()
    }

    override fun endSession() {
        mediaContent.summary?.let {
            it.sessionEnd = System.currentTimeMillis()
            it.playToEnd = true
            finalizeSummaryInfo()
            super.endSession()
        }
    }

    override fun play() {
        mediaContent.summary?.let {
            it.playStartTime = System.currentTimeMillis()
            it.plays++
        }
    }

    override fun pause() {
        mediaContent.summary?.let { summary ->
            summary.pauses++
            summary.totalPlayTime?.let {
                val timeElapsed = System.currentTimeMillis() - it
                summary.totalPlayTime = it + timeElapsed.toInt()
            }
        }
    }

    override fun stop() {
        mediaContent.summary?.let { summary ->
            summary.stops++
            summary.totalPlayTime?.let {
                val timeElapsed = System.currentTimeMillis() - it
                summary.totalPlayTime = it + timeElapsed.toInt()
            }
        }
    }

    override fun startChapter(chapter: Chapter) {
        mediaContent.summary?.let {
            it.chapterStarts++
        }
    }

    override fun skipChapter() {
        mediaContent.summary?.let {
            it.chapterSkips++
            it.chapterEnds++
        }
    }

    override fun endChapter() {
        mediaContent.summary?.let {
            it.chapterEnds++
        }
    }

    override fun startAd(ad: Ad) {
        mediaContent.summary?.let {
            it.adStartTime = System.currentTimeMillis()
            it.adUuids.add(ad.uuid)
            it.ads++
        }
    }

    override fun skipAd() {
        mediaContent.summary?.let { summary ->
            summary.adSkips++
            summary.adEnds++
            summary.adStartTime?.let {
                val timeElapsed = System.currentTimeMillis() - it
                summary.adStartTime = it + timeElapsed
            }
        }
    }

    override fun endAd() {
        mediaContent.summary?.let { summary ->
            summary.adEnds++
            summary.adStartTime?.let {
                val timeElapsed = System.currentTimeMillis() - it
                summary.adStartTime = it +timeElapsed
            }
        }
    }

    override fun startBuffer() {
        mediaContent.summary?.let {
            it.bufferStartTime = System.currentTimeMillis()
        }
    }

    override fun endBuffer() {
        mediaContent.summary?.let { summary ->
            summary.bufferStartTime?.let { start ->
                val timeElapse = System.currentTimeMillis() - start
                summary.totalBufferTime?.let {
                    summary.totalBufferTime = it + timeElapse.toInt()
                }
            }
        }
    }

    override fun startSeek(playhead: Int?) {
        mediaContent.summary?.let {
            it.seekStartTime = System.currentTimeMillis()
        }
    }

    override fun endSeek(playhead: Int?) {
        mediaContent.summary?.let { summary ->
            summary.seekStartTime?.let { start ->
                val timeElapse = System.currentTimeMillis() - start
                summary.totalSeekTime?.let {
                    summary.totalSeekTime = it + timeElapse.toInt()
                }
            }
        }
    }

    override fun finalizeSummaryInfo() {
        mediaContent.summary?.let { summary ->
            reusableDate.time = summary.sessionStart
            summary.sessionStartTime = formatIso8601.format(reusableDate)

            summary.duration = summary.sessionEnd?.minus(summary.sessionStart)

            summary.sessionEnd?.let { endTime ->
                reusableDate.time = endTime
                summary.sessionEndTime = formatIso8601.format(reusableDate)
            }

            if (summary.chapterStarts > 0) {
                summary.percentageChapterComplete = ((summary.chapterEnds / summary.chapterStarts) * 100).toDouble()
            }

            if (summary.ads > 0) {
                summary.percentageAdComplete = ((summary.adEnds / summary.ads) * 100).toDouble()
            }

            summary.totalAdTime?.let { adTime ->
                if (adTime > 0) {
                    summary.totalPlayTime?.let { totalTime ->
                        summary.percentageAdTime = ((adTime / totalTime) * 100).toDouble()
                    }
                }
            }
        }
    }

    companion object {
        const val FORMAT_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'"
        const val TIMESTAMP_INVALID = Long.MIN_VALUE
    }

    private var formatIso8601 = SimpleDateFormat(FORMAT_ISO_8601, Locale.ROOT)
    private var reusableDate: Date = Date(TIMESTAMP_INVALID)
}