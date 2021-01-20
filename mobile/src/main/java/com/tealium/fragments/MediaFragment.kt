package com.tealium.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tealium.core.Tealium
import com.tealium.media.*
import com.tealium.media.segments.Ad
import com.tealium.media.segments.AdBreak
import com.tealium.media.segments.Chapter
import com.tealium.mobile.BuildConfig
import com.tealium.mobile.R
import kotlinx.android.synthetic.main.fragment_media.*

class MediaFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_media, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startMediaButton.setOnClickListener {
            onStartMediaSession()
        }

        startAdBreakButton.setOnClickListener {
            onStartAdBreak()
        }

        startAdButton.setOnClickListener {
            onStartAd()
        }

        startChapterButton.setOnClickListener {
            onStartChapter()
        }

        completeAdButton.setOnClickListener {
            onCompleteAd()
        }

        completeAdBreakButton.setOnClickListener {
            onCompleteAdBreak()
        }

        completeChapterButton.setOnClickListener {
            onCompleteChapter()
        }

        endMediaButton.setOnClickListener {
            onEndMediaSession()
        }
    }

    private fun onStartMediaSession() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.startSession(
                MediaSession(
                        "Test Session",
                        StreamType.PODCAST,
                        MediaType.AUDIO,
                        QoE(1),
                        TrackingType.HEARTBEAT
                ))
    }

    private fun onStartAdBreak() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.startAdBreak(
                AdBreak("someId")
        )
    }

    private fun onStartAd() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.startAd(
                Ad("someId")
        )
    }

    private fun onStartChapter() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.startChapter(
                Chapter("someName")
        )
    }

    private fun onCompleteAd() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.endAd()
    }

    private fun onCompleteAdBreak() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.endAdBreak()
    }

    private fun onCompleteChapter() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.endChapter()
    }

    private fun onEndMediaSession() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.endSession()
    }
}