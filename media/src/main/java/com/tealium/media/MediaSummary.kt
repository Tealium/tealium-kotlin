package com.tealium.media

data class MediaSummary(var sessionStartTime: String? = null,
                        var plays: Int = 0,
                        var pauses: Int = 0,
                        var adSkips: Int = 0,
                        var chapterSkips: Int = 0,
                        var stops: Int = 0,
                        var ads: Int = 0,
                        var adUuids: MutableSet<String> = mutableSetOf(),
                        var playToEnd: Boolean = false,
                        var duration: Long? = 0,
                        var totalPlayTime: Int? = 0,
                        var totalAdTime: Int? = 0,
                        var percentageAdTime: Double? = 0.0,
                        var percentageAdComplete: Double? = 0.0,
                        var percentageChapterComplete: Double? = 0.0,
                        var totalBufferTime: Int? = 0,
                        var totalSeekTime: Int? = 0,
                        var sessionEndTime: String? = null,

                        var sessionStart: Long = System.currentTimeMillis(),
                        var sessionEnd: Long? = 0,
                        var playStartTime: Long? = 0,
                        var bufferStartTime: Long? = 0,
                        var seekStartTime: Long? = 0,
                        var adStartTime: Long? = 0,
                        var chapterStarts: Int = 0,
                        var chapterEnds: Int = 0,
                        var adEnds: Int = 0) {
    companion object {
        fun toMap(mediaSummary: MediaSummary) {
            val data = mutableMapOf<String, Any>()
            mediaSummary.sessionStartTime?.let {
                data[SummaryKey.SESSION_START_TIME] = it
            }

            data[SummaryKey.PLAYS] = mediaSummary.plays
            data[SummaryKey.PAUSES] = mediaSummary.pauses
            data[SummaryKey.ADSKIPS] = mediaSummary.adSkips
            data[SummaryKey.CHAPTER_SKIPS] = mediaSummary.chapterSkips
            data[SummaryKey.STOPS] = mediaSummary.stops
            data[SummaryKey.ADS] = mediaSummary.ads
            data[SummaryKey.AD_UUIDS] = mediaSummary.adUuids
            data[SummaryKey.PLAY_TO_END] = mediaSummary.playToEnd
            mediaSummary.duration?.let {
                data[SummaryKey.DURATION] = it
            }

            mediaSummary.totalPlayTime?.let {
                data[SummaryKey.TOTAL_PLAY_TIME] = it
            }

            mediaSummary.totalAdTime?.let {
                data[SummaryKey.TOTAL_AD_TIME] = it
            }

            mediaSummary.percentageAdTime?.let {
                data[SummaryKey.PERCENTAGE_AD_TIME] = it
            }

            mediaSummary.percentageAdComplete?.let {
                data[SummaryKey.PERCENTAGE_AD_COMPLETE] = it
            }

            mediaSummary.percentageChapterComplete?.let {
                data[SummaryKey.PERCENTAGE_CHAPTER_COMPLETE] = it
            }

            mediaSummary.totalBufferTime?.let {
                data[SummaryKey.TOTAL_BUFFER_TIME] = it
            }

            mediaSummary.totalSeekTime?.let {
                data[SummaryKey.TOTAL_SEEK_TIME] = it
            }

            mediaSummary.sessionEnd?.let {
                data[SummaryKey.SESSION_END_TIME] = it
            }
        }
    }
}