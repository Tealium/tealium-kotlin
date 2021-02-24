package com.tealium.media

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

open class MediaService : Service() {

    lateinit var mediaPlayer: MediaPlayer
    private val binder: IBinder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        mediaPlayer.player?.playWhenReady = true
        startPlayer()

    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }


    override fun onBind(intent: Intent?): IBinder? {
        handleIntent(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startPlayer()
        return START_STICKY
    }


    private fun startPlayer() {
        mediaPlayer = MediaPlayer(applicationContext)
        mediaPlayer.startPlayer()
    }

    private fun releasePlayer() {
        mediaPlayer.release()
    }

    fun startAdBreak() {
        mediaPlayer.onStartAdBreak()
    }

    fun endAdBreak() {
        mediaPlayer.onEndAdBreak()
    }

    fun startAd() {
        mediaPlayer.onStartAd()
    }

    fun endAd() {
        mediaPlayer.onEndAd()
    }

    fun startChapter() {
        mediaPlayer.onStartChapter()
    }

    fun endChapter() {
        mediaPlayer.onEndChapter()
    }

    fun resume() {
        mediaPlayer.resume()
    }

    fun stop() {
        mediaPlayer.stop()
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            // TODO dunno - do I need this?? Maybe
        }
    }

    inner class LocalBinder : Binder() {
        val service
            get() = this@MediaService

        val player
            get() = this@MediaService.mediaPlayer?.player
    }
}