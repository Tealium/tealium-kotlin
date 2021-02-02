package com.tealium.media

import com.tealium.media.segments.Ad
import com.tealium.media.segments.AdBreak
import com.tealium.media.segments.Chapter
import java.util.*

data class MediaContent(var name: String,
                        var streamType: StreamType,
                        var mediaType: MediaType,
                        var qoe: QoE,
                        var trackingType: TrackingType = TrackingType.SIGNIFICANT,
                        var state: PlayerState? = null,
                        var customId: String? = null,
                        var duration: Int? = null,
                        var playerName: String? = null,
                        var channelName: String? = null,
                        var metadata: Map<String, Any>? = null,

                        val adBreakList: MutableList<AdBreak> = mutableListOf(),
                        var adBreakCount: Int = 0,
                        val adList: MutableList<Ad> = mutableListOf(),
                        var adCount: Int = 0,
                        val chapterList: MutableList<Chapter> = mutableListOf(),
                        var chapterCount: Int = 0,
                        var milestone: Milestone? = null,
                        var summary: MediaSummary? = null,
                        var startTime: Long? = null,
                        var endTime: Long? = null,
                        private val uuid: String = UUID.randomUUID().toString()) {

    companion object {
        fun toMap(mediaContent: MediaContent): Map<String, Any> {
            val data = mutableMapOf(
                    SessionKey.UUID to mediaContent.uuid,
                    SessionKey.NAME to mediaContent.name,
                    SessionKey.STREAM_TYPE to mediaContent.streamType,
                    SessionKey.MEDIA_TYPE to mediaContent.mediaType,
                    SessionKey.QOE to QoE.toMap(mediaContent.qoe),
                    SessionKey.TRACKING_TYPE to mediaContent.trackingType
            )

            mediaContent.startTime?.let {
                data[SessionKey.START_TIME] = it
            }

            mediaContent.state?.let {
                data[SessionKey.STATE] = it
            }

            mediaContent.customId?.let {
                data[SessionKey.CUSTOM_ID] = it
            }

            mediaContent.duration?.let {
                data[SessionKey.DURATION] = it
            }

            mediaContent.playerName?.let {
                data[SessionKey.PLAYER_NAME] = it
            }

            mediaContent.channelName?.let {
                data[SessionKey.CHANNEL_NAME] = it
            }

            mediaContent.metadata?.let {
                data[SessionKey.METADATA] = it
            }

            mediaContent.milestone?.let {
                data[SessionKey.MILESTONE] = it
            }

            mediaContent.summary?.let {
                data[SessionKey.SUMMARY] = MediaSummary.toMap(it)
            }

            return data
        }
    }
}