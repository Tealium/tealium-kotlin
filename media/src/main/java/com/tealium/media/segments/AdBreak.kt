package com.tealium.media.segments

import com.tealium.media.AdBreakKey
import java.util.*

class AdBreak(var id: String,
              var title: String? = null,
              var index: Int? = null,
              var position: Int? = null,
              var startTime: Long? = null,
              var duration: Long? = null,
              private val uuid: String = UUID.randomUUID().toString()) : Segment {

    override fun start() {
        startTime = System.currentTimeMillis()
    }

    override fun end() {
        startTime?.let {
            duration = System.currentTimeMillis() - it
        }
    }

    override fun skip() {
        // nothing?
    }

    override fun segmentInfo(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()
        data[AdBreakKey.ID] = id
        data[AdBreakKey.UUID] = uuid

        title?.let { data[AdBreakKey.TITLE] = it }
        index?.let { data[AdBreakKey.INDEX] = it }
        position?.let { data[AdBreakKey.POSITION] = it }
        duration?.let { data[AdBreakKey.DURATION] = it }

        return data.toMap()
    }
}