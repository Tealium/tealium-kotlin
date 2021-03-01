package com.tealium.media.segments

import com.tealium.media.AdBreakKey
import com.tealium.media.Media
import java.util.*

data class AdBreak(var id: String,
                   val name: String? = null,
                   var index: Int? = null,
                   var position: Int? = null) : Segment {

    private val uuid: String = UUID.randomUUID().toString()
    private val adBreakName: String = name ?: uuid
    private var startTime: Long? = null
    private var duration: Double? = null

    override fun start() {
        startTime = System.currentTimeMillis()
    }

    override fun end() {
        startTime?.let {
            duration = Media.timeMillisToSeconds(System.currentTimeMillis() - it)
        }
    }

    override fun skip() {
        // do nothing
    }

    override fun segmentInfo(): Map<String, Any> {
        val data = mutableMapOf<String, Any>(
                AdBreakKey.ID to id,
                AdBreakKey.UUID to uuid,
                AdBreakKey.NAME to adBreakName
        )

        index?.let { data[AdBreakKey.INDEX] = it }
        position?.let { data[AdBreakKey.POSITION] = it }
        duration?.let { data[AdBreakKey.DURATION] = it }

        return data.toMap()
    }
}