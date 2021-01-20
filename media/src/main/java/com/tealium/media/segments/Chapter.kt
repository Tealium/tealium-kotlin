package com.tealium.media.segments

import com.tealium.media.ChapterKey

class Chapter(var name: String,
              var duration: Long? = null,
              var position: Int? = null,
              var startTime: Long? = null,
              var metadata: Any? = null, // what is this????
              private var numberOfChapters: Int = 0) : Segment {

    override fun segmentInfo(): Map<String, Any>? {
        val data = mutableMapOf<String, Any>()
        data[ChapterKey.NAME] = name

        duration?.let {
            data[ChapterKey.DURATION] = it
        }
        position?.let {
            data[ChapterKey.POSITION] = it
        }
        startTime?.let {
            data[ChapterKey.START_TIME] = it
        }
        metadata?.let {
            data[ChapterKey.METADATA] = it
        }

        return data.toMap()
    }
}