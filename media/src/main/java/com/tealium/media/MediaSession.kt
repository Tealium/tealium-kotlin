package com.tealium.media

import com.tealium.media.segments.*
import java.util.*

interface Session {

    val id: String
    var state: PlayerState?
    var startTime: Long
    var endTime: Long?

    fun start()
    fun end()
    fun adBreakStart(adBreak: AdBreak)
    fun adBreakComplete(): Map<String, Any>?
    fun adClick(): Map<String, Any>?
    fun adStart(ad: Ad)
    fun adComplete(): Map<String, Any>?
    fun skipAd()
    fun chapterStart(chapter: Chapter)
    fun chapterComplete(): Map<String, Any>?
    fun skipChapter()
    fun seek()
    fun seekComplete()
    fun startBuffer()
    fun bufferComplete()
//    fun play()
//    fun pause()
//    fun stop()
//    fun custom(event: String)

    fun updateBitrate(rate: Int)
    fun updateDroppedFrames(frames: Int)
    fun updatePlaybackSpeed(speed: Double)
    fun updatePlayerState(updatedStated: PlayerState)
}

class MediaSession(val name: String,
                   val streamType: StreamType,
                   val mediaType: MediaType,
                   var qoe: QoE,
                   val trackingType: TrackingType) : Session, Segment {

    override val id = UUID.randomUUID().toString()
    override var startTime: Long = -1L
    override var endTime: Long? = null
    override var state: PlayerState? = null

    private var adBreakList = mutableListOf<AdBreak>()
    private var adList = mutableListOf<Ad>()
    private var chapterList = mutableListOf<Chapter>()

    override fun start() {
        startTime = System.currentTimeMillis()
    }

    override fun end() {
        endTime = duration()
    }

    override fun adBreakStart(adBreak: AdBreak) {
        adBreakList.add(adBreak)
    }

    override fun adBreakComplete(): Map<String, Any>? {
        return if (adBreakList.isNotEmpty()) {
            val adBreakData = adBreakList.first().segmentInfo()
            adBreakList.remove(adBreakList.first())
            adBreakData
        } else null
    }

    override fun adClick(): Map<String, Any>? {
        return if (adList.isNotEmpty()) {
            adList.last().segmentInfo()
        } else null
    }

    override fun adStart(ad: Ad) {
        adList.add(ad)
    }

    override fun adComplete(): Map<String, Any>? {
        return if (adList.isNotEmpty()) {
            val adBreakData = adList.first().segmentInfo()
            adList.remove(adList.first())
            adBreakData
        } else null
    }

    override fun skipAd() {}

    override fun chapterStart(chapter: Chapter) {
        chapterList.add(chapter)
    }

    override fun chapterComplete(): Map<String, Any>? {
        return if (chapterList.isNotEmpty()) {
            val chapterData = chapterList.first().segmentInfo()
            chapterList.remove(chapterList.first())
            chapterData
        } else null
    }

    override fun skipChapter() {

    }

    override fun seek() {
        // TODO
    }

    override fun seekComplete() {
        // TODO
    }

    override fun startBuffer() {
        // TODO
    }

    override fun bufferComplete() {
        // TODO
    }

// Media Service should just send track event
//    override fun play() {
//    }
//
//    override fun pause() {
//    }
//
//    override fun stop() {
//    }
//
//    override fun custom(event: String) {
//    }

    override fun updateBitrate(rate: Int) {
        qoe.bitrate = rate
    }

    override fun updateDroppedFrames(frames: Int) {
        qoe.droppedFrames = frames
    }

    override fun updatePlaybackSpeed(speed: Double) {
        qoe.playbackSpeed = speed
    }

    override fun updatePlayerState(updatedStated: PlayerState) {
        state = updatedStated
    }

    private fun duration(): Long = System.currentTimeMillis() - startTime;

    override fun segmentInfo(): Map<String, Any> {
        // Summary data map??
        return mapOf()
    }
}

enum class StreamType(val value: String) {
    AOD("aod"),
    AUDIOBOOK("audiobook"),
    CUSTOM("custom"),
    DVOD("dvod"),
    LIVE("live"),
    LINEAR("linear"),
    PODCAST("podcast"),
    RADIO("radio"),
    SONG("song"),
    UGC("ugc"),
    VOD("vod"),
}

enum class MediaType(val value: String) {
    ALL("al"),
    AUDIO("audio"),
    VIDEO("video"),
}

enum class TrackingType(val value: String) {
    HEARTBEAT("heartbeat"),
    MILESTONE("milestone"),
    SIGNIFICANT("significant"),
    SUMMARY("summary"),

}

enum class PlayerState(val value: String) {
    CLOSEDCAPTION(""),
    FULLSCREEN(""),
    INFOCUS(""),
    MUTE(""),
    PICTUREINPICTURE(""),
}

data class QoE(var bitrate: Int,
               var startTime: Int? = null,
               var fps: Int? = null,
               var droppedFrames: Int? = 0,
               var playbackSpeed: Double? = 1.0,
               var metadata: Map<String, Any>? = null)