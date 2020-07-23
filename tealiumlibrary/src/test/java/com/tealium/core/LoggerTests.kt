package com.tealium.core

import org.junit.Assert
import org.junit.Test

class LoggerTests {

    @Test
    fun testLogLevel_FromString() {
        Assert.assertEquals(LogLevel.DEV, LogLevel.fromString("dev"))
        Assert.assertEquals(LogLevel.QA, LogLevel.fromString("qa"))
        Assert.assertEquals(LogLevel.PROD, LogLevel.fromString("prod"))
        Assert.assertEquals(LogLevel.SILENT, LogLevel.fromString("silent"))
        // Default = PROD
        Assert.assertEquals(LogLevel.PROD, LogLevel.fromString("invalid"))
    }

    @Test
    fun testLogLevel_FromEnvironment() {
        Assert.assertEquals(LogLevel.DEV, LogLevel.fromString(Environment.DEV.environment))
        Assert.assertEquals(LogLevel.QA, LogLevel.fromString(Environment.QA.environment))
        Assert.assertEquals(LogLevel.PROD, LogLevel.fromString(Environment.PROD.environment))
    }
}