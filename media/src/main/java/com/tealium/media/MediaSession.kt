package com.tealium.media

data class MediaSession(val name: String,
                        val streamType: String,
                        val mediaType: String,
                        val qoe: String, // Quality of Experience
                        val trackingType: String) {
}

enum class StreamType(val value: String) {
    VOD("videoOnDemand"),
    LIVE("live"),
    LINEAR("linear"),
    PODCAST("podcast"),
    AUDIOBOOK("audiobook"),
    AOD("audioOnDemand"),
    SONG("song"),
    RADIO("radio"),
    UGC("userGeneratedContent"),
    DVOD("digitalVideoOnDemand"),
    CUSTOM("custom")
}

enum class MediaType(val value: String) {
    VIDEO("video"),
    AUDIO("audio"),
    ALL("all")
}

enum class TrackingType(val value: String) {
    HEARTBEAT("heartbeat"),
    SIGNIFICANT_EVENT("significant_event"),
    MILESTONE("milestone"),
    SUMMARY("summary")
}