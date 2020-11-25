package com.tealium.crashreporter.internal

import android.app.Activity
import android.content.SharedPreferences
import com.tealium.core.*
import com.tealium.core.messaging.ActivityObserverListener
import com.tealium.crashreporter.truncateCrashReporterStackTraces
import com.tealium.dispatcher.TealiumEvent
import java.util.*

internal class CrashReporter(private val context: TealiumContext,
                    private val sharedPreferences: SharedPreferences = context.config.application.getSharedPreferences(getSharedPreferencesName(context.config), 0))
    : Module, Thread.UncaughtExceptionHandler, ActivityObserverListener {

    private val originalExceptionHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()
    private var truncateCrashStackTraces: Boolean = context.config.truncateCrashReporterStackTraces
            ?: false
    private var crashCount: Int = sharedPreferences.getInt(CRASH_COUNT, 0)

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    private fun getBuildId(): String {
        return sharedPreferences.getString(CRASH_BUILD_ID, null) ?: setBuildId()
    }

    private fun setBuildId(): String {
        val id: String = UUID.randomUUID().toString()
        sharedPreferences.edit()
                .putString(CRASH_BUILD_ID, id)
                .apply()
        return id
    }

    private fun incrementCrashCount() {
        sharedPreferences.edit()
                .putInt(CRASH_COUNT, ++crashCount)
                .apply()
    }

    /**
     * Handler invoked when a Thread abruptly terminates due to an uncaught exception.
     *
     * @param thread
     * @param ex
     */
    override fun uncaughtException(thread: Thread, ex: Throwable) {
        val crash = Crash(thread, ex)
        saveCrashData(crash)
        incrementCrashCount()
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
                crashData.put(CRASH_BUILD_ID, getBuildId())
                crashData.put(CRASH_COUNT, crashCount)

                context.track(TealiumEvent("crash", crashData))
                clearSavedCrashData()
            }
        }
    }

    fun saveCrashData(crash: Crash) {
        sharedPreferences.edit()
                .putString(CRASH_EXCEPTION_CAUSE, crash.exceptionCause)
                .putString(CRASH_EXCEPTION_NAME, crash.exceptionName)
                .putString(CRASH_UUID, crash.uuid)
                .putString(CRASH_THREADS, Crash.getThreadData(crash, truncateCrashStackTraces))
                .apply()
    }

    fun readSavedCrashData(): MutableMap<String, Any>? {
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
        private const val MODULE_NAME = "CRASH_REPORTER"
        private const val CRASH_COUNT = "crash_count"
        private const val CRASH_BUILD_ID = "crash_build_id"
        private const val CRASH_EXCEPTION_CAUSE = "crash_exception_cause"
        private const val CRASH_EXCEPTION_NAME = "crash_exception_name"
        private const val CRASH_UUID = "crash_uuid"
        private const val CRASH_THREADS = "crash_threads"

        @JvmStatic private fun getSharedPreferencesName(config: TealiumConfig): String {
            return "tealium.crash." + config.accountName + config.profileName + config.environment.toString()
        }
    }

    override val name: String = MODULE_NAME
    override var enabled: Boolean = true
}