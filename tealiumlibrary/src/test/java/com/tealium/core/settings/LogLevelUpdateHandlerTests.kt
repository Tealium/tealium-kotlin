package com.tealium.core.settings

import com.tealium.core.LogLevel
import com.tealium.core.Logger
import org.junit.Assert
import org.junit.Test

class LogLevelUpdateHandlerTests {

    @Test
    fun onLibrarySettingsUpdated_Sets_NewLogLevel() {
        Logger.logLevel = LogLevel.SILENT
        val handler = LogLevelUpdateHandler(null)

        handler.onLibrarySettingsUpdated(LibrarySettings(logLevel = LogLevel.DEV))
        Assert.assertEquals(LogLevel.DEV, Logger.logLevel)
        handler.onLibrarySettingsUpdated(LibrarySettings(logLevel = LogLevel.QA))
        Assert.assertEquals(LogLevel.QA, Logger.logLevel)
        handler.onLibrarySettingsUpdated(LibrarySettings(logLevel = LogLevel.PROD))
        Assert.assertEquals(LogLevel.PROD, Logger.logLevel)
        handler.onLibrarySettingsUpdated(LibrarySettings(logLevel = LogLevel.SILENT))
        Assert.assertEquals(LogLevel.SILENT, Logger.logLevel)
    }

    @Test
    fun onLibrarySettingsUpdated_DoesNotSet_NewLogLevel_When_ConfigLogLevel_IsSet() {
        Logger.logLevel = LogLevel.SILENT
        val handler = LogLevelUpdateHandler(LogLevel.SILENT)

        handler.onLibrarySettingsUpdated(LibrarySettings(logLevel = LogLevel.DEV))
        Assert.assertEquals(LogLevel.SILENT, Logger.logLevel)
        handler.onLibrarySettingsUpdated(LibrarySettings(logLevel = LogLevel.QA))
        Assert.assertEquals(LogLevel.SILENT, Logger.logLevel)
        handler.onLibrarySettingsUpdated(LibrarySettings(logLevel = LogLevel.PROD))
        Assert.assertEquals(LogLevel.SILENT, Logger.logLevel)
        handler.onLibrarySettingsUpdated(LibrarySettings(logLevel = LogLevel.SILENT))
        Assert.assertEquals(LogLevel.SILENT, Logger.logLevel)
    }
}