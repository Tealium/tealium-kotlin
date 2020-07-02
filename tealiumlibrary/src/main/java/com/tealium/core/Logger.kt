package com.tealium.core

import android.content.Context
import android.util.Log
import com.tealium.core.messaging.*
import com.tealium.core.model.LibrarySettings
import com.tealium.dispatcher.Dispatch
import com.tealium.tealiumlibrary.BuildConfig
import kotlinx.coroutines.CoroutineExceptionHandler

enum class LogLevel(val level: Int) {
    DEV(Log.VERBOSE),
    QA(Log.INFO),
    PROD(Log.ASSERT),
    SILENT(Integer.MAX_VALUE)
}

interface Logging {

    fun dev(tag: String, msg: String)

    fun qa(tag: String, msg: String)

    fun prod(tag: String, msg: String)
}

class Logger(val config: TealiumConfig) {

    val context: Context = config.application

    companion object : Logging,
            DispatchQueuedListener,
            DispatchReadyListener,
            DispatchSendListener,
            BatchDispatchSendListener,
            LibrarySettingsUpdatedListener {

        var logLevel: LogLevel = LogLevel.DEV
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            prod(BuildConfig.TAG, "Caught $exception")
            exception.stackTrace?.let { stackStrace ->
                prod(BuildConfig.TAG, stackStrace.joinToString { element ->
                    element.toString() + "\n"
                })
            }
        }

        override fun dev(tag: String, msg: String) {
            if (logLevel.level <= LogLevel.DEV.level) {
                Log.d(tag, msg)
            }
        }

        override fun qa(tag: String, msg: String) {
            if (logLevel.level <= LogLevel.QA.level) {
                Log.i(tag, msg)
            }
        }

        override fun prod(tag: String, msg: String) {
            if (logLevel.level <= LogLevel.PROD.level) {
                Log.e(tag, msg)
            }
        }

        override fun onDispatchReady(dispatch: Dispatch) {
            dev(BuildConfig.TAG, "Dispatch(${dispatch.id.substring(0, 5)}) - Ready")
        }

        override suspend fun onDispatchSend(dispatch: Dispatch) {
            dev(BuildConfig.TAG, "Dispatch(${dispatch.id.substring(0, 5)}) - Sending")
        }

        override suspend fun onBatchDispatchSend(dispatches: List<Dispatch>) {
            dev(BuildConfig.TAG, "Dispatch(${dispatches.joinToString(prefix = "[", postfix = "]") {
                it.id.substring(0, 5)
            }
            }) - Sending")
        }

        override fun onDispatchQueued(dispatch: Dispatch) {
            dev(BuildConfig.TAG, "Dispatch(${dispatch.id.substring(0, 5)}) - Queueing")
        }

        override fun onLibrarySettingsUpdated(settings: LibrarySettings) {
            dev(BuildConfig.TAG, "LibrarySettings updated: $settings")
        }
    }
}
