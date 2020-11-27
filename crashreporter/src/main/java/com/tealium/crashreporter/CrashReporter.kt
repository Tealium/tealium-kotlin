package com.tealium.crashreporter

import com.tealium.core.*
import com.tealium.crashreporter.internal.CrashHandler

/**
 * The Crash Reporter Module will register itself as the default [Thread.UncaughtExceptionHandler]
 * and log any unexpected crashes. Upon the next app launch the crash data will be sent to any
 * Dispatchers used.
 */
class CrashReporter(private val context: TealiumContext,
                    val exceptionHandler: Thread.UncaughtExceptionHandler = CrashHandler(context,
                            context.config.application.getSharedPreferences(getSharedPreferencesName(context.config), 0),
                            Thread.getDefaultUncaughtExceptionHandler()))
    : Module {

    init {
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler)
    }

    companion object : ModuleFactory {
        private const val MODULE_NAME = "CRASH_REPORTER"

        @JvmStatic
        internal fun getSharedPreferencesName(config: TealiumConfig): String {
            return "tealium.crash." + Integer.toHexString((config.accountName + config.profileName + config.environment.environment).hashCode())
        }

        override fun create(context: TealiumContext): Module {
            return CrashReporter(context)
        }
    }

    override val name: String = MODULE_NAME
    override var enabled: Boolean = true
}