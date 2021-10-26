package com.tealium.crashreporter.internal

import android.annotation.SuppressLint
import android.app.Activity
import android.content.SharedPreferences
import com.tealium.core.TealiumContext
import com.tealium.core.messaging.ActivityObserverListener
import com.tealium.crashreporter.truncateCrashReporterStackTraces
import com.tealium.dispatcher.TealiumEvent
import java.util.*

internal class CrashHandler(private val context: TealiumContext,
                            private val sharedPreferences: SharedPreferences,
                            val originalExceptionHandler: Thread.UncaughtExceptionHandler? = null,
                            val truncateStackTraces: Boolean = context.config.truncateCrashReporterStackTraces
                                    ?: false) : Thread.UncaughtExceptionHandler, ActivityObserverListener {

    private var _crashCount: Int = sharedPreferences.getInt(CRASH_COUNT, 0)
        @SuppressLint("ApplySharedPref")
        set(value) {
            field = value
            sharedPreferences.edit().putInt(CRASH_COUNT, value).commit()
        }

    val crashCount: Int
        get() = _crashCount
    val buildId: String

    init {
        context.events.subscribe(this)
        Thread.setDefaultUncaughtExceptionHandler(this)
        buildId = sharedPreferences.getString(CRASH_BUILD_ID, null)
                ?: UUID.randomUUID().toString().also {
                    sharedPreferences.edit()
                            .putString(CRASH_BUILD_ID, it)
                            .apply()
                }
    }

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        val crash = Crash(thread, ex)
        saveCrashData(crash)
        _crashCount++
        originalExceptionHandler?.uncaughtException(thread, ex)
    }

    override fun onActivityPaused(activity: Activity?) {
        // Do nothing
    }

    override fun onActivityResumed(activity: Activity?) {
        sendCrashData()
    }

    override fun onActivityStopped(activity: Activity?, isChangingConfiguration: Boolean) {
        // Do nothing
    }

    fun sendCrashData() {
        readSavedCrashData()?.let { crashData ->
            if (crashData.isNotEmpty()) {
                // There is a crash saved.
                crashData[CRASH_BUILD_ID] = buildId
                crashData[CRASH_COUNT] = crashCount

                context.track(TealiumEvent(CRASH_EVENT, crashData))
                clearSavedCrashData()
            }
        }
    }


    @SuppressLint("ApplySharedPref")
    fun saveCrashData(crash: Crash) {
        sharedPreferences.edit()
                .putString(CRASH_EXCEPTION_CAUSE, crash.exceptionCause)
                .putString(CRASH_EXCEPTION_NAME, crash.exceptionName)
                .putString(CRASH_UUID, crash.uuid)
                .putString(CRASH_THREADS, Crash.getThreadData(crash, truncateStackTraces))
                .commit()
    }

    private fun readSavedCrashData(): MutableMap<String, Any>? {
        val crashData = mutableMapOf<String, Any>()

        sharedPreferences.getString(CRASH_EXCEPTION_CAUSE, null)?.let {
            crashData[CRASH_EXCEPTION_CAUSE] = it
        }
        sharedPreferences.getString(CRASH_EXCEPTION_NAME, null)?.let {
            crashData[CRASH_EXCEPTION_NAME] = it
        }
        sharedPreferences.getString(CRASH_THREADS, null)?.let {
            crashData[CRASH_THREADS] = it
        }
        sharedPreferences.getString(CRASH_UUID, null)?.let {
            crashData[CRASH_UUID] = it
        }
        return crashData
    }

    fun clearSavedCrashData() {
        sharedPreferences.edit()
                .remove(CRASH_EXCEPTION_CAUSE)
                .remove(CRASH_EXCEPTION_NAME)
                .remove(CRASH_UUID)
                .remove(CRASH_THREADS)
                .apply()
    }

    companion object {
        internal const val CRASH_EVENT = "crash"
        internal const val CRASH_COUNT = "crash_count"
        internal const val CRASH_BUILD_ID = "crash_build_id"
        internal const val CRASH_EXCEPTION_CAUSE = "crash_cause"
        internal const val CRASH_EXCEPTION_NAME = "crash_name"
        internal const val CRASH_UUID = "crash_uuid"
        internal const val CRASH_THREADS = "crash_threads"
    }
}