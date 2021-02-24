package com.tealium.media

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.tealium.core.Tealium
import com.tealium.media.segments.Ad
import com.tealium.media.segments.AdBreak
import com.tealium.media.segments.Chapter
import com.tealium.mobile.BuildConfig

class MediaPlayer(private val context: Context) {

    var player: SimpleExoPlayer? = null
    private var sampleUrl = "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4"
    private var playerNotificationManager: PlayerNotificationManager? = null
    private val mediaContent: MediaContent = MediaContent(
            "Test Session",
            StreamType.VOD,
            MediaType.VIDEO,
            QoE(1),
            trackingType = TrackingType.HEARTBEAT_MILESTONE,
            duration = 126
    )

//    init {
//        startPlayer()
//    }

    // should this be done on init{} instead?
    fun startPlayer() {
        if (player == null) {
            player = SimpleExoPlayer.Builder(context).build()
            player?.addListener(createListener())

            buildMediaSource()?.let {
                player?.prepare(it)
            }

            playerNotificationManager =
                    PlayerNotificationManager.createWithNotificationChannel(
                            context,
                            CHANNEL_ID,
                            R.string.app_name, // TODO change this: channelName A string resource identifier for the user visible name of the channel.
                            0, // TODO change this: channelDescription A string resource identifier for the user visible description of the channel, or 0 if no description is provided.
                            NOTIFICATION_ID,
                            createMediaDescriptionManager(),
                            createNotificationListener()
                    )
            playerNotificationManager?.setPlayer(player)
            startMediaSession()
        }
    }

    fun resume() {
        player?.playWhenReady = true
    }

    fun stop() {
        player?.playWhenReady = false
        player?.stop(true)
        release()
    }

    fun release() {
        player?.let {
            it.release()
            player = null
            playerNotificationManager?.setPlayer(null)
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

    private fun createMediaDescriptionManager(): PlayerNotificationManager.MediaDescriptionAdapter {
        return object : PlayerNotificationManager.MediaDescriptionAdapter {
            override fun createCurrentContentIntent(player: Player): PendingIntent? {
                TODO("Not yet implemented")
            }

            override fun getCurrentContentText(player: Player): CharSequence? {
                TODO("Not yet implemented")
            }

            override fun getCurrentContentTitle(player: Player): CharSequence {
                TODO("Not yet implemented")
            }

            override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): Bitmap? {
                return null
            }

        }
    }

    private fun createNotificationListener(): PlayerNotificationManager.NotificationListener {
        return object : PlayerNotificationManager.NotificationListener {
            override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                stopSelf()
            }

            override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
                startForeground(notificationId, notification)
            }
        }
    }

    private fun buildMediaSource(): MediaSource? {
        val dataSourceFactory = DefaultDataSourceFactory(context, "sample")
        return ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(sampleUrl))
    }

    fun startMediaSession() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.startSession(mediaContent)
    }

    fun onStartAdBreak() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.startAdBreak(AdBreak("Ad Break 1"))
    }

    fun onEndAdBreak() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.endAdBreak()
    }

    fun onStartAd() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.startAd(Ad("Ad  1"))
    }

    fun onEndAd() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.endAd()
    }

    fun onStartChapter() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.startChapter(Chapter("Chapter 1"))
    }

    fun onEndChapter() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.endChapter()
    }

    companion object : MediaService() {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "playback_channel"
    }
}