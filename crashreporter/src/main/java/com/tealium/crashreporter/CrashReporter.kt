package com.tealium.crashreporter

import com.tealium.core.Module
import com.tealium.core.ModuleFactory
import com.tealium.core.TealiumContext
import com.tealium.crashreporter.internal.CrashReporter

interface CrashReporter: Module {
    companion object : ModuleFactory {
        override fun create(context: TealiumContext): Module {
            return CrashReporter(context)
        }
    }
}