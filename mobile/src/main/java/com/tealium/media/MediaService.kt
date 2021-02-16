package com.tealium.media

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.tealium.core.Tealium
import com.tealium.mobile.BuildConfig

open class MediaService : Service() {

    private var sampleUrl = "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4"
    private var player: SimpleExoPlayer? = null
    private var playerNotificationManager: PlayerNotificationManager? = null
    private val binder: IBinder = LocalBinder()

    override fun onDestroy() {
        playerNotificationManager?.setPlayer(null)
//        releasePlayer()
        player = null
        super.onDestroy()
    }


    override fun onBind(intent: Intent?): IBinder? {
        handleIntent(intent)
        return LocalBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startPlayer()
        return START_STICKY
    }


    private fun startPlayer() {
        player = SimpleExoPlayer.Builder(applicationContext).build()
        player?.addListener(object : Player.EventListener {
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
        })

        player?.addListener(object : Player.EventListener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                when (isPlaying) {
                    true -> Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.play()
                    false -> Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.pause()
                }
            }
        })

        buildMediaSource()?.let {
            player?.prepare(it)
        }

        playerNotificationManager =
                PlayerNotificationManager.createWithNotificationChannel(
                        applicationContext,
                        CHANNEL_ID,
                        R.string.app_name, // TODO change this: channelName A string resource identifier for the user visible name of the channel.
                        0, // TODO change this: channelDescription A string resource identifier for the user visible description of the channel, or 0 if no description is provided.
                        NOTIFICATION_ID,
                        createMediaDescriptionManager(),
                        createNotificationListener()
                )
    }

    private fun buildMediaSource(): MediaSource? {
        val dataSourceFactory = DefaultDataSourceFactory(applicationContext, "sample")
        return ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(sampleUrl))
    }

    private fun releasePlayer() {
        if (player != null) {

        }
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            // TODO dunno - do I need this?? Maybe
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
                TODO("Not yet implemented")
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

    companion object : MediaService() {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "playback_channel"
    }

    inner class LocalBinder : Binder() {
        val service
            get() = this@MediaService

        val exoPlayer
            get() = this@MediaService.player
    }
}