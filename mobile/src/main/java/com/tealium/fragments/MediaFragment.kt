package com.tealium.fragments

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.MediaController
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.tealium.core.Tealium
import com.tealium.media.*
import com.tealium.media.segments.Ad
import com.tealium.media.segments.AdBreak
import com.tealium.media.segments.Chapter
import com.tealium.mobile.BuildConfig
import com.tealium.mobile.R
import com.tealium.mobile.TealiumHelper
import kotlinx.android.synthetic.main.fragment_media.*

class MediaFragment : Fragment() {

    private var videoPlayer: SimpleExoPlayer? = null
    private var sampleUrl = "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_media, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onStartMediaSession()
        initializePlayer()

        startAdBreakButton.setOnClickListener {
            onStartAdBreak()
        }

        endAdBreakButton.setOnClickListener {
            onEndAdBreak()
        }

        startAdButton.setOnClickListener {
            onStartAd()
        }

        endAdButton.setOnClickListener {
            onEndAd()
        }

        startChapterButton.setOnClickListener {
            onStartChapter()
        }

        endChapterButton.setOnClickListener {
            onEndChapter()
        }
    }

    private fun buildMediaSource(): MediaSource? {
        val dataSourceFactory = DefaultDataSourceFactory(activity, "sample")
        return ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(sampleUrl))
    }

    private fun initializePlayer() {
        activity?.applicationContext?.let { app ->
            videoPlayer = SimpleExoPlayer.Builder(app).build()
            videoPlayer?.addListener(object : Player.EventListener {
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    when (playbackState) {
                        ExoPlayer.STATE_BUFFERING -> {}
                        ExoPlayer.STATE_ENDED -> Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.endSession()
                        ExoPlayer.STATE_READY -> {}
                        ExoPlayer.STATE_IDLE -> println("Idle")
                        else -> print("unknownState$playbackState")
                    }
                }
            })

            videoPlayer?.addListener(object : Player.EventListener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    when (isPlaying) {
                        true -> Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.play()
                        false -> Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.pause()
                    }
                }
            })
            video_player_view?.player = videoPlayer
            buildMediaSource()?.let {
                videoPlayer?.prepare(it)
            }
        }
    }

    private fun onStartMediaSession() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.startSession(
                MediaContent(
                        "Test Session",
                        StreamType.VOD,
                        MediaType.AUDIO,
                        QoE(1),
                        trackingType = TrackingType.MILESTONE,
                        duration = 126
                ))
    }

    private fun onStartAdBreak() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.startAdBreak(AdBreak("Ad Break 1"))
    }

    private fun onEndAdBreak() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.endAdBreak()
    }

    private fun onStartAd() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.startAd(Ad("Ad  1"))
    }

    private fun onEndAd() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.endAd()
    }

    private fun onStartChapter() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.startChapter(Chapter("Chapter 1"))
    }

    private fun onEndChapter() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.endChapter()
    }

    override fun onResume() {
        super.onResume()
        videoPlayer?.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        videoPlayer?.playWhenReady = false
        releasePlayer()
    }

    private fun releasePlayer() {
        videoPlayer?.release()
    }
}