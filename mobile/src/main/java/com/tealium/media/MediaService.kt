package com.tealium.media

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
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

class MediaService : Service() {

    private var player: SimpleExoPlayer? = null
    private var playerNotificationManager: PlayerNotificationManager? = null
    private var mediaSession: MediaSessionCompat? = null

    private var mediaUrl = "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4"
    private var binder = LocalBinder()

    private val mediaContent: MediaContent = MediaContent(
            "Tealium Sample Media Content",
            StreamType.VOD,
            MediaType.VIDEO,
            QoE(1),
            trackingType = TrackingType.SIGNIFICANT,
            duration = 120
    )

    override fun onCreate() {
        super.onCreate()
        startPlayer()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onDestroy() {
        mediaSession?.release()
        playerNotificationManager?.setPlayer(null)
        player?.release()
        player = null

        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startPlayer() {
        player = SimpleExoPlayer.Builder(this).build()
        player?.addListener(createListener())

        buildMediaSource()?.let {
            player?.prepare(it)
        }

        playerNotificationManager =
                PlayerNotificationManager.createWithNotificationChannel(
                        this,
                        CHANNEL_ID,
                        R.string.app_name, // TODO change this: channelName A string resource identifier for the user visible name of the channel.
                        0, // TODO change this: channelDescription A string resource identifier for the user visible description of the channel, or 0 if no description is provided.
                        NOTIFICATION_ID,
                        createMediaDescriptionManager(),
                        createNotificationListener()
                )
        playerNotificationManager?.setPlayer(player)

        val playbackState = PlaybackStateCompat.Builder()
        playbackState.setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE)

        mediaSession = MediaSessionCompat(this, "Tealium Sample Media").apply {
            isActive = true
            setPlaybackState(playbackState.build())
            playerNotificationManager?.setMediaSessionToken(sessionToken)
        }
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.startSession(mediaContent)
    }

    private fun buildMediaSource(): MediaSource? {
        val dataSourceFactory = DefaultDataSourceFactory(this, "sample")
        return ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(mediaUrl))
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
                return null
            }

            override fun getCurrentContentText(player: Player): CharSequence? {
                return "Tealium Sample Media"
            }

            override fun getCurrentContentTitle(player: Player): CharSequence {
                return "Tealium Sample Media"
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

    fun startAdBreak() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.startAdBreak(AdBreak("Ad Break 1"))
    }

    fun endAdBreak() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.endAdBreak()
    }

    fun startAd() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.startAd(Ad("Ad  1"))
    }

    fun endAd() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.endAd()
    }

    fun startChapter() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.startChapter(Chapter("Chapter 1"))
    }

    fun endChapter() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.endChapter()
    }

    fun resume() {
    }

    fun stop() {
    }

    inner class LocalBinder : Binder() {
        val service
            get() = this@MediaService

        val player
            get() = this@MediaService.player
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "playback_channel"
    }
}