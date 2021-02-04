package com.tealium.media

data class QoE(var bitrate: Int,
               var startTime: Int? = null,
               var fps: Int? = null,
               var droppedFrames: Int? = 0,
               var playbackSpeed: Double? = 1.0,
               var metadata: Map<String, Any>? = null) {
    companion object {
        fun toMap(qoe: QoE): Map<String, Any> {
            val data = mutableMapOf<String, Any>(
                    QoEKey.BITRATE to qoe.bitrate
            )

            qoe.startTime?.let {
                data[QoEKey.START_TIME] = it
            }

            qoe.fps?.let {
                data[QoEKey.FPS] = it
            }

            qoe.droppedFrames?.let {
                data[QoEKey.DROPPED_FRAMES] = it
            }

            qoe.playbackSpeed?.let {
                data[QoEKey.PLAYBACK_SPEED] = it
            }

            qoe.metadata?.let {
                data[QoEKey.METADATA] = it
            }

            return data.toMap()
        }
    }
}