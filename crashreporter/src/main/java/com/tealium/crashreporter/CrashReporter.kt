package com.tealium.crashreporter

import android.content.SharedPreferences
import com.tealium.core.*

/**
 * Crash Reporter module for tracking crash data. When used, this module populates
 * <i>crash_*</i> attributes defined in {@link com.tealium.library.DataSources.Key}.
 */
class CrashReporter (private val context: TealiumContext) : Module {
    private val CRASH_COUNT = "crash_count"

    private var sharedPreferences: SharedPreferences
    // ? in constructor?
    private lateinit var config: TealiumConfig
    private var truncateCrashStackTraces: Boolean = false
    private lateinit var originalExceptionHandler: Thread.UncaughtExceptionHandler

    private var crashCount: Int = 0

    init {
        val sharedPrefsName: String = getSharedPreferencesName(config)

        sharedPreferences = config.application
                .getSharedPreferences(sharedPrefsName, 0)

        originalExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

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
        if (originalExceptionHandler != null) {
            originalExceptionHandler.uncaughtException(thread, ex)
        }
    }

    fun incrementCrashCount() {
        sharedPreferences.edit()
                .putInt(CRASH_COUNT, ++crashCount)
                .commit()
    }

    fun saveCrashData(crash: Crash) {
        sharedPreferences.edit()
                .putString("cause", crash.exceptionCause)
                .putString("name", crash.exceptionName)
                .putString("id", crash.uUid)
                .putString("thread", Crash.getThreadData(crash, truncateCrashStackTraces))
                .commit()
    }

    private fun getSharedPreferencesName(config: TealiumConfig): String {
        return "tealium.crash." + config.accountName + config.profileName + config.environment.toString()
    }



    companion object : ModuleFactory {
        const val MODULE_NAME = "CRASHREPORTER"

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
 * Returns the Lifecycle module for this Tealium instance.
 */
val Tealium.crashreporter: CrashReporter?
    get() = modules.getModule(CrashReporter::class.java)