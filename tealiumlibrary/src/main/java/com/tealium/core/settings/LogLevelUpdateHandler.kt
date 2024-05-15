package com.tealium.core.settings

import com.tealium.core.LogLevel
import com.tealium.core.Logger
import com.tealium.core.messaging.LibrarySettingsUpdatedListener

class LogLevelUpdateHandler(private val configLogLevel: LogLevel?): LibrarySettingsUpdatedListener {
    override fun onLibrarySettingsUpdated(settings: LibrarySettings) {
        // don't override if log level was set by code.
        if (configLogLevel == null) {
            Logger.logLevel = settings.logLevel
        }
    }
}