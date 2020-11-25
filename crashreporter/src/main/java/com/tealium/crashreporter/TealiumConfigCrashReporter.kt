package com.tealium.crashreporter

import com.tealium.core.ModuleFactory
import com.tealium.core.Modules
import com.tealium.core.Tealium
import com.tealium.core.TealiumConfig
import com.tealium.crashreporter.CrashReporter

val Modules.CrashReporter: ModuleFactory
    get() = com.tealium.crashreporter.CrashReporter

/**
 * Returns the CrashReporter module for this Tealium instance.
 */
val Tealium.crashReporter: CrashReporter?
    get() = modules.getModule(CrashReporter::class.java)

const val CRASH_REPORTER_TRUNCATE_STACK_TRACES = "crash_reporter_truncate_stack_traces"

/**
 * Determines whether or not to truncate Stack Trace data. When set to true, only a single line
 * of stack trace data will be available.
 *
 * Default: false
 */
var TealiumConfig.truncateCrashReporterStackTraces: Boolean?
    get() = options[CRASH_REPORTER_TRUNCATE_STACK_TRACES] as? Boolean
    set(value) {
        value?.let {
            options[CRASH_REPORTER_TRUNCATE_STACK_TRACES] = it
        } ?: options.remove(CRASH_REPORTER_TRUNCATE_STACK_TRACES)
    }