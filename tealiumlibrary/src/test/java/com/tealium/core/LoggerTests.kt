package com.tealium.core

import com.tealium.core.settings.LibrarySettings
import org.junit.Assert.assertEquals
import org.junit.Test

class LoggerTests {

    @Test
    fun testLogLevel_FromString() {
        assertEquals(LogLevel.DEV, LogLevel.fromString("dev"))
        assertEquals(LogLevel.QA, LogLevel.fromString("qa"))
        assertEquals(LogLevel.PROD, LogLevel.fromString("prod"))
        assertEquals(LogLevel.SILENT, LogLevel.fromString("silent"))
        // Default = PROD
        assertEquals(LogLevel.PROD, LogLevel.fromString("invalid"))
    }

    @Test
    fun testLogLevel_FromEnvironment() {
        assertEquals(LogLevel.DEV, LogLevel.fromString(Environment.DEV.environment))
        assertEquals(LogLevel.QA, LogLevel.fromString(Environment.QA.environment))
        assertEquals(LogLevel.PROD, LogLevel.fromString(Environment.PROD.environment))
    }

    @Test
    fun onLibrarySettingsUpdated_Sets_NewLogLevel() {
        Logger.logLevel = LogLevel.SILENT

        Logger.onLibrarySettingsUpdated(LibrarySettings(logLevel = LogLevel.DEV))
        assertEquals(LogLevel.DEV, Logger.logLevel)
        Logger.onLibrarySettingsUpdated(LibrarySettings(logLevel = LogLevel.QA))
        assertEquals(LogLevel.QA, Logger.logLevel)
        Logger.onLibrarySettingsUpdated(LibrarySettings(logLevel = LogLevel.PROD))
        assertEquals(LogLevel.PROD, Logger.logLevel)
        Logger.onLibrarySettingsUpdated(LibrarySettings(logLevel = LogLevel.SILENT))
        assertEquals(LogLevel.SILENT, Logger.logLevel)
    }
}