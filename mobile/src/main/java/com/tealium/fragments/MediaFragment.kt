package com.tealium.fragments

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
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

//    private lateinit var mediaService: MediaService
    private var isBound: Boolean = false

    private var connection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MediaService.LocalBinder) {
                video_player_view.player = service.player
                isBound = true
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_media, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.applicationContext?.let { app ->
            Intent(app, MediaService::class.java).also { intent ->
                activity?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }

        }

//        onStartMediaSession()
//        initializePlayer()

        startAdBreakButton.setOnClickListener {
//            onStartAdBreak()
        }

        endAdBreakButton.setOnClickListener {
//            onEndAdBreak()
        }

        startAdButton.setOnClickListener {
//            onStartAd()
        }

        endAdButton.setOnClickListener {
//            onEndAd()
        }

        startChapterButton.setOnClickListener {
//            onStartChapter()
        }

        endChapterButton.setOnClickListener {
//            onEndChapter()
        }
    }

    override fun onResume() {
        super.onResume()
//        videoPlayer?.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
//        videoPlayer?.playWhenReady = false
//        releasePlayer()
    }
}