package com.tealium.media

data class MediaSummary(var sessionStartTime: Long = System.currentTimeMillis()) {

    var sessionStartTimestamp: String? = null
    var plays: Int = 0
    var pauses: Int = 0
    var ads: Int = 0
    var adUuids: MutableSet<String> = mutableSetOf()
    var adSkips: Int = 0
    var chapterSkips: Int = 0
    var playToEnd: Boolean = false

    var duration: Double? = 0.0
    var totalPlayTime: Double? = 0.0
    var totalAdTime: Double? = 0.0
    var totalBufferTime: Double? = 0.0
    var totalSeekTime: Double? = 0.0
    var percentageAdTime: Double? = 0.0
    var percentageAdComplete: Double? = 0.0
    var percentageChapterComplete: Double? = 0.0

    var playStartTime: Long? = 0
    var bufferStartTime: Long? = 0
    var seekStartTime: Long? = 0
    var adStartTime: Long? = 0
    var adEnds: Int = 0
    var chapterStarts: Int = 0
    var chapterEnds: Int = 0

    var sessionEndTime: Long? = 0
    var sessionEndTimestamp: String? = null

    companion object {
        fun toMap(mediaSummary: MediaSummary): Map<String, Any> {
            val data = mutableMapOf<String, Any>()
            mediaSummary.sessionStartTimestamp?.let {
                data[SummaryKey.SESSION_START_TIME] = it
            }

            data[SummaryKey.PLAYS] = mediaSummary.plays
            data[SummaryKey.PAUSES] = mediaSummary.pauses
            data[SummaryKey.AD_SKIPS] = mediaSummary.adSkips
            data[SummaryKey.CHAPTER_SKIPS] = mediaSummary.chapterSkips
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

            mediaSummary.sessionEndTimestamp?.let {
                data[SummaryKey.SESSION_END_TIME] = it
            }
            return data
        }
    }
}

fun MediaSummary.toMap() : Map<String, Any> {
    return MediaSummary.toMap(this)
}