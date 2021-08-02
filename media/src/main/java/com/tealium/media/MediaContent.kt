package com.tealium.media

import com.tealium.media.segments.Ad
import com.tealium.media.segments.AdBreak
import com.tealium.media.segments.Chapter
import java.util.*

data class MediaContent(var name: String,
                        var streamType: StreamType,
                        var mediaType: MediaType,
                        var qoe: QoE,
                        var trackingType: TrackingType = TrackingType.FULL_PLAYBACK,
                        var state: PlayerState? = null,
                        var customId: String? = null,
                        var duration: Int? = null,
                        var playerName: String? = null,
                        var channelName: String? = null,
                        var metadata: MutableMap<String, Any> = mutableMapOf()) {

    private val uuid: String = UUID.randomUUID().toString()

    val adBreakList: MutableList<AdBreak> = mutableListOf()
    val adList: MutableList<Ad> = mutableListOf()
    val chapterList: MutableList<Chapter> = mutableListOf()
    var milestone: Milestone? = null
    var summary: MediaSummary? = null

    var startTime: Long? = null
    var endTime: Long? = null
    var percentContentComplete: Double? = null

    internal fun toMap(): Map<String, Any> {
        val data = mutableMapOf<String, Any>(
            SessionKey.UUID to uuid,
            SessionKey.NAME to name,
            SessionKey.STREAM_TYPE to streamType.value,
            SessionKey.MEDIA_TYPE to mediaType.value,
            SessionKey.TRACKING_TYPE to trackingType.value
        )

        data.putAll(QoE.toMap(qoe))
        data.putAll(metadata.toMap())

        percentContentComplete?.let {
            data[SessionKey.PERCENT_COMPLETE] = it
        }

        startTime?.let {
            data[SessionKey.START_TIME] = it
        }

        state?.let {
            data[SessionKey.STATE] = it.value
        }

        customId?.let {
            data[SessionKey.CUSTOM_ID] = it
        }

        duration?.let {
            data[SessionKey.DURATION] = it
        }

        playerName?.let {
            data[SessionKey.PLAYER_NAME] = it
        }

        channelName?.let {
            data[SessionKey.CHANNEL_NAME] = it
        }

        milestone?.let {
            data[SessionKey.MILESTONE] = it.value
        }

        summary?.let {
            data.putAll(MediaSummary.toMap(it))
        }

        return data
    }

    companion object {
        fun toMap(mediaContent: MediaContent): Map<String, Any> {
            val data = mutableMapOf<String, Any>(
                    SessionKey.UUID to mediaContent.uuid,
                    SessionKey.NAME to mediaContent.name,
                    SessionKey.STREAM_TYPE to mediaContent.streamType.value,
                    SessionKey.MEDIA_TYPE to mediaContent.mediaType.value,
                    SessionKey.TRACKING_TYPE to mediaContent.trackingType.value
            )

            data.putAll(QoE.toMap(mediaContent.qoe))
            data.putAll(mediaContent.metadata)

            mediaContent.percentContentComplete?.let {
                data[SessionKey.PERCENT_COMPLETE] = it
            }

            mediaContent.startTime?.let {
                data[SessionKey.START_TIME] = it
            }

            mediaContent.state?.let {
                data[SessionKey.STATE] = it.value
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

            mediaContent.milestone?.let {
                data[SessionKey.MILESTONE] = it.value
            }

            mediaContent.summary?.let {
                data.putAll(MediaSummary.toMap(it))
            }

            return data
        }
    }
}