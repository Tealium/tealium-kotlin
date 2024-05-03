package com.tealium.core

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
}