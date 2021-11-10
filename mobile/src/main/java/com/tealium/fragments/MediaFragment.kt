package com.tealium.fragments

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.*
import androidx.fragment.app.Fragment
import com.tealium.media.*
import com.tealium.mobile.databinding.FragmentMediaBinding

class MediaFragment : Fragment() {

    private lateinit var binding: FragmentMediaBinding
    private lateinit var mediaService: MediaService
    private var isBound: Boolean = false

    private var connection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MediaService.LocalBinder) {
                mediaService = service.service
                binding.videoPlayerView.player = service.player
                isBound = true
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMediaBinding.inflate(inflater, container, false)
        activity?.applicationContext?.let { app ->
            Intent(app, MediaService::class.java).also { intent ->
                activity?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.startAdBreakButton.setOnClickListener {
            if (isBound) {
                mediaService.startAdBreak()
            }
        }

        binding.endAdBreakButton.setOnClickListener {
            if (isBound) {
                mediaService.endAdBreak()
            }
        }

        binding.startAdButton.setOnClickListener {
            if (isBound) {
                mediaService.startAd()
            }
        }

        binding.endAdButton.setOnClickListener {
            if (isBound) {
                mediaService.endAd()
            }
        }

        binding.startChapterButton.setOnClickListener {
            if (isBound) {
                mediaService.startChapter()
            }
        }

        binding.endChapterButton.setOnClickListener {
            if (isBound) {
                mediaService.endChapter()
            }
        }
    }

    override fun onDestroy() {
        println("Media Fragment destroyed")
        super.onDestroy()
    }
}