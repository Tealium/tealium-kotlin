package com.tealium.media.segments

import com.tealium.media.AdBreakKeys
import java.util.*

class AdBreak(var id: String,
              var name: String? = null,
              var index: Int? = null,
              var position: Int? = null,
              var startTime: Long? = System.currentTimeMillis(),
              var duration: Long? = null,
              var uuid: String? = UUID.randomUUID().toString(),
              private var numberOfAdBreaks: Int = 0) : Segment {

    override fun segmentInfo(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()
        data[AdBreakKeys.ID] = id

//        name?.let { data[AdBreakKeys.NAME] = it }
        index?.let { data[AdBreakKeys.INDEX] = it }
        position?.let { data[AdBreakKeys.POSITION] = it }
//        startTime?.let { data[AdBreakKeys.START_TIME] = it }
        duration?.let { data[AdBreakKeys.DURATION] = it }
        uuid?.let { data[AdBreakKeys.UUID] = it }
//        numberOfAdBreaks?.let { data[AdBreakKeys.NUMBER_OF_ADBREAKS] = it }

        return data.toMap()
    }
}