package com.tealium.media.segments

import com.tealium.media.ChapterKey
import java.util.*

class Chapter(var name: String,
              var position: Int? = null,
              var skipped: Boolean? = false,
              var metadata: Map<String, Any>? = null,
              private val uuid: String = UUID.randomUUID().toString()) : Segment {

    private var startTime: Long? = null
    private var duration: Long? = null

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
        val data = mutableMapOf<String, Any>()
        data[ChapterKey.NAME] = name

        duration?.let { data[ChapterKey.DURATION] = it }
        position?.let { data[ChapterKey.POSITION] = it }
        startTime?.let { data[ChapterKey.START_TIME] = it }
        metadata?.let { data.putAll(it) }

        return data.toMap()
    }
}