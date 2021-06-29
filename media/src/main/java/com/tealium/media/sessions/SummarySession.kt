package com.tealium.media.sessions

import com.tealium.media.Media
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
                     mediaDispatcher: MediaDispatcher) : FullPlaybackSession(mediaContent, mediaDispatcher) {

    override fun startSession() {
        mediaContent.summary = MediaSummary()
        super.startSession()
    }

    override fun endSession() {
        mediaContent.summary?.let {
            it.sessionEndTime = System.currentTimeMillis()
            finalizeSummaryInfo()
            super.endSession()
        }
    }

    override fun endContent() {
        mediaContent.summary?.playToEnd = true
        mediaContent.summary?.playStartTime?.let {
            mediaContent.summary?.totalPlayTime =
                    Media.timeMillisToSeconds(System.currentTimeMillis() - it)
        }
        super.endContent()
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
            summary.playStartTime?.let { start ->
                val timeElapsed = Media.timeMillisToSeconds(System.currentTimeMillis() - start)
                summary.totalPlayTime?.let {
                    summary.totalPlayTime = it + timeElapsed

                }
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
                val timeElapsed = Media.timeMillisToSeconds(System.currentTimeMillis() - it)
                summary.totalAdTime = it + timeElapsed
            }
        }
    }

    override fun endAd() {
        mediaContent.summary?.let { summary ->
            summary.adEnds++
            summary.adStartTime?.let {
                val timeElapsed = Media.timeMillisToSeconds(System.currentTimeMillis() - it)
                summary.totalAdTime = it + timeElapsed
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
                val timeElapsed = Media.timeMillisToSeconds(System.currentTimeMillis() - start)
                summary.totalBufferTime?.let {
                    summary.totalBufferTime = it + timeElapsed
                }
            }
        }
    }

    override fun startSeek(position: Int) {
        mediaContent.summary?.let {
            it.seekStartTime = System.currentTimeMillis()
        }
    }

    override fun endSeek(position: Int) {
        mediaContent.summary?.let { summary ->
            summary.seekStartTime?.let { start ->
                val timeElapse = Media.timeMillisToSeconds(System.currentTimeMillis() - start)
                summary.totalSeekTime?.let {
                    summary.totalSeekTime = it + timeElapse
                }
            }
        }
    }

    override fun finalizeSummaryInfo() {
        mediaContent.summary?.let { summary ->
            reusableDate.time = summary.sessionStartTime
            summary.sessionStartTimestamp = formatIso8601.format(reusableDate)

            summary.sessionEndTime?.let {
                summary.duration = Media.timeMillisToSeconds(it.minus(summary.sessionStartTime))
            }

            summary.sessionEndTime?.let { endTime ->
                reusableDate.time = endTime
                summary.sessionEndTimestamp = formatIso8601.format(reusableDate)
            }

            if (summary.chapterStarts > 0) {
                summary.percentageChapterComplete = percentage(summary.chapterEnds, summary.chapterStarts)
            }

            if (summary.ads > 0) {
                summary.percentageAdComplete = percentage(summary.adEnds, summary.ads)
            }

            summary.totalAdTime?.let { adTime ->
                if (adTime > 0) {
                    summary.totalPlayTime?.let { totalTime ->
                        summary.percentageAdTime = percentage(adTime.toInt(), totalTime.toInt())
                    }
                }
            }
        }
    }

    private fun percentage(count: Int, total: Int): Double {
        return ((count.toDouble() / total.toDouble()) * 100)
    }

    companion object {
        const val FORMAT_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'"
        const val TIMESTAMP_INVALID = Long.MIN_VALUE
    }

    private var formatIso8601 = SimpleDateFormat(FORMAT_ISO_8601, Locale.ROOT)
    private var reusableDate: Date = Date(TIMESTAMP_INVALID)
}