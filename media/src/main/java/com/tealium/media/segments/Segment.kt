package com.tealium.media.segments

interface Segment {
    fun start()
    fun end()
    fun skip()

    fun segmentInfo(): Map<String, Any>
}

