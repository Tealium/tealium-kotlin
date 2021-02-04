package com.tealium.media.segments

import com.tealium.media.ChapterKey
import java.util.*

data class Chapter(val name: String,
                   var position: Int? = null,
                   var metadata: Map<String, Any>? = null) : Segment {

    private val uuid: String = UUID.randomUUID().toString()
    private var startTime: Long? = null
    private var duration: Long? = null
    private var skipped: Boolean = false

    override fun start() {
        startTime = System.currentTimeMillis()
    }

    override fun end() {
        startTime?.let {
            duration = System.currentTimeMillis() - it
        }
    }

    override fun skip() {
        end()
        skipped = true
    }

    override fun segmentInfo(): Map<String, Any> {
        val data = mutableMapOf(
                ChapterKey.NAME to name,
                ChapterKey.UUID to uuid,
                ChapterKey.SKIPPED to skipped
        )

        duration?.let { data[ChapterKey.DURATION] = it }
        position?.let { data[ChapterKey.POSITION] = it }
        startTime?.let { data[ChapterKey.START_TIME] = it }
        metadata?.let { data[ChapterKey.METADATA] = it }

        return data.toMap()
    }
}