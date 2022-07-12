package com.tealium.media.v2

import com.tealium.media.MediaType
import com.tealium.media.SessionKey
import com.tealium.media.StreamType

data class MediaMetadata(
    val id: String? = null,
    val name: String? = null,
    val duration: Int? = null,
    val streamType: StreamType? = null,
    val mediaType: MediaType? = null,
    val startTime: Long? = null,
    val playerName: String? = null,
    val channelName: String? = null
)

fun MediaMetadata.toMap() : Map<String, Any> {
    val data = mutableMapOf<String, Any>()

    id?.let { data[SessionKey.UUID] = it }
    name?.let { data[SessionKey.NAME] = it }
    streamType?.value?.let { data[SessionKey.STREAM_TYPE] = it }
    mediaType?.value?.let { data[SessionKey.MEDIA_TYPE] = it }
    startTime?.let { data[SessionKey.START_TIME] = it }
    playerName?.let { data[SessionKey.PLAYER_NAME] = it }
    channelName?.let { data[SessionKey.CHANNEL_NAME] = it }

    return data.toMap()
}
