package com.tealium.fragments

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.tealium.media.*
import com.tealium.mobile.R
import kotlinx.android.synthetic.main.fragment_media.*

class MediaFragment : Fragment() {

    private lateinit var mediaService: MediaService
    private var isBound: Boolean = false

    private var connection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MediaService.LocalBinder) {
                mediaService = service.service
                video_player_view.player = service.player
                isBound = true
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity?.applicationContext?.let { app ->
            Intent(app, MediaService::class.java).also { intent ->
                activity?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        }
        return inflater.inflate(R.layout.fragment_media, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startAdBreakButton.setOnClickListener {
            mediaService.startAdBreak()
        }

        endAdBreakButton.setOnClickListener {
            mediaService.endAdBreak()
        }

        startAdButton.setOnClickListener {
            mediaService.startAd()
        }

        endAdButton.setOnClickListener {
            mediaService.endAd()
        }

        startChapterButton.setOnClickListener {
            mediaService.startChapter()
        }

        endChapterButton.setOnClickListener {
            mediaService.endChapter()
        }
    }

    override fun onResume() {
        super.onResume()
//        mediaService.resume()
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
//            activity?.unbindService(connection)
//            mediaService.stop()
        }
    }
}