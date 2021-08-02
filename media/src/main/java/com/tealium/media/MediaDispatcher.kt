package com.tealium.media

import com.tealium.media.segments.Segment

interface MediaDispatcher {
    fun track(event: String, mediaContent: MediaContent, segment: Segment? = null, customData: Map<String, Any>? = null)
}