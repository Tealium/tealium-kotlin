package com.tealium.crashreporter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.SharedPreferences
import com.tealium.core.*
import com.tealium.dispatcher.TealiumEvent
import java.util.*

class CrashReporter (private val context: TealiumContext) : Module {

    private val sharedPrefsName: String = getSharedPreferencesName(context.config)
    private val sharedPreferences: SharedPreferences = context.config.application.getSharedPreferences(sharedPrefsName, 0)

    private var truncateCrashStackTraces: Boolean = false
    private val originalExceptionHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()

    private var crashCount: Int = 0



    private fun getBuildId(): String {
        sharedPreferences.getString(CRASH_BUILD_ID, null)?.let {
            return it
        }?: run {
            return setBuildId()
        }
    }

    private fun setBuildId(): String {
        val id: String = generateUuid()
        sharedPreferences.edit()
                .putString(CRASH_BUILD_ID, id)
                .apply()
        return id
    }

    private fun generateUuid(): String {
        return UUID.randomUUID().toString()
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
    fun uncaughtException(thread: Thread, ex: Throwable) {
        val crash: Crash = Crash(thread, ex)
        saveCrashData(crash)
        incrementCrashCount()
        originalExceptionHandler?.uncaughtException(thread, ex)
    }

    private fun sendCrashData() {
        val savedCrashData = readSavedCrashData()
        if (savedCrashData!!.isNotEmpty()) {
            // There is a crash saved.
            savedCrashData.put(CRASH_BUILD_ID, getBuildId())
            savedCrashData.put(CRASH_COUNT, crashCount)

            val eventDispatch = TealiumEvent("crash", savedCrashData)

            context.track(eventDispatch)?.let { clearSavedCrashData() }
        }
    }

    @SuppressLint("ApplySharedPref")
    fun saveCrashData(crash: Crash) {
        sharedPreferences.edit()
                .putString(CRASH_EXCEPTION_CAUSE, crash.exceptionCause)
                .putString(CRASH_EXCEPTION_NAME, crash.exceptionName)
                .putString(CRASH_UUID, crash.uUid)
                .putString(CRASH_THREADS, Crash.getThreadData(crash, truncateCrashStackTraces))
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

    private fun getSharedPreferencesName(config: TealiumConfig): String {
        return "tealium.crash." + config.accountName + config.profileName + config.environment.toString()
    }

    @SuppressLint("ApplySharedPref")
    fun clearSavedCrashData() {
        sharedPreferences.edit()
                .remove(CRASH_EXCEPTION_CAUSE)
                .remove(CRASH_EXCEPTION_NAME)
                .remove(CRASH_UUID)
                .remove(CRASH_THREADS)
                .commit()
    }

    companion object : ModuleFactory {
        const val MODULE_NAME = "CRASHREPORTER"
        const val CRASH_COUNT = "crash_count"
        const val CRASH_BUILD_ID = "crash_build_id"
        const val CRASH_EXCEPTION_CAUSE = "crash_exception_cause"
        const val CRASH_EXCEPTION_NAME = "crash_exception_name"
        const val CRASH_UUID = "crash_uuid"
        const val CRASH_THREADS = "crash_threads"

        override fun create(context: TealiumContext): Module {
            return CrashReporter(context)
        }

    }

    override val name: String = MODULE_NAME
    override var enabled: Boolean = true
}

val Modules.CrashFactory: ModuleFactory
    get() = com.tealium.crashreporter.CrashReporter

/**
 * Returns the CrashReporter module for this Tealium instance.
 */
val Tealium.crashreporter: CrashReporter?
    get() = modules.getModule(CrashReporter::class.java)