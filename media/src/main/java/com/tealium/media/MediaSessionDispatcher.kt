package com.tealium.media

import com.tealium.core.TealiumContext
import com.tealium.dispatcher.TealiumEvent
import com.tealium.media.segments.Segment

class MediaSessionDispatcher(private val context: TealiumContext) : MediaDispatcher {

    override fun track(event: String, mediaContent: MediaContent, segment: Segment?) {
        val data = mutableMapOf<String, Any>()

        data.putAll(MediaContent.toMap(mediaContent))

        // TODO check segment data overrides Media Content data (should be granular)
        segment?.let {
            data.putAll(segment.segmentInfo())
        }

        context.track(TealiumEvent(event, data))
    }
}