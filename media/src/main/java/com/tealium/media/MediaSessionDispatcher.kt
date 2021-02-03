package com.tealium.media

import com.tealium.core.TealiumContext
import com.tealium.dispatcher.TealiumEvent
import com.tealium.media.segments.Segment

class MediaSessionDispatcher(private val context: TealiumContext) : MediaDispatcher {

    override fun track(event: String, mediaContent: MediaContent, segment: Segment?) {
        val data = mutableMapOf<String, Any>()

        data.putAll(MediaContent.toMap(mediaContent))

        segment?.let {
            // merge MediaContent metadata with Chapter metadata (chapter data overwrites mediaContent data)
            if (segment.segmentInfo().containsKey(ChapterKey.METADATA)) {
                mediaContent.metadata?.putAll(segment.segmentInfo()[ChapterKey.METADATA] as Map<String, Any>)
            }

            // TODO don't re-add chapter metadata
            data.putAll(segment.segmentInfo())


        }

        context.track(TealiumEvent(event, data))
    }
}