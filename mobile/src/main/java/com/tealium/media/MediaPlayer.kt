package com.tealium.media

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.tealium.core.Tealium
import com.tealium.mobile.BuildConfig

class MediaPlayer(private val context: Context,
                  private val mediaContent: MediaContent) {

    private var player: SimpleExoPlayer? = null
    private var sampleUrl = "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4"

    fun start() {
        player = SimpleExoPlayer.Builder(context).build()
        player?.addListener(createListener())

        buildMediaSource()?.let {
            player?.prepare(it)
        }

    }

    fun stop() {
        player?.stop(true)
    }

    fun release() {
        if (player != null) {
            player?.release()
            player = null
        }
    }

    private fun createListener(): Player.EventListener {
        return object : Player.EventListener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                when (isPlaying) {
                    true -> Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.play()
                    false -> Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.pause()
                }
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState) {
                    ExoPlayer.STATE_BUFFERING -> {
                    }
                    ExoPlayer.STATE_ENDED -> Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.endSession()
                    ExoPlayer.STATE_READY -> {
                    }
                    ExoPlayer.STATE_IDLE -> println("Idle")
                    else -> print("unknownState$playbackState")
                }
            }
        }
    }

    private fun buildMediaSource(): MediaSource? {
        val dataSourceFactory = DefaultDataSourceFactory(context, "sample")
        return ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(sampleUrl))
    }
}