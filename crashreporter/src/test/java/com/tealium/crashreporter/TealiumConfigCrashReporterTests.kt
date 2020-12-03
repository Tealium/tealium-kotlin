package com.tealium.crashreporter

import android.app.Application
import com.tealium.core.Environment
import com.tealium.core.Modules
import com.tealium.core.TealiumConfig
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class TealiumConfigCrashReporterTests {

    @MockK
    lateinit var mockApplication: Application

    @MockK
    lateinit var mockFile: File

    private val accountName = "account"
    private val profileName = "profile"
    private val environment = Environment.DEV

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { mockApplication.filesDir } returns mockFile
    }

    @Test
    fun overrides_TruncateStackTraces_SetsCorrectly() {
        val config = TealiumConfig(mockApplication, accountName, profileName, environment)
        val override = true
        assertNull(config.truncateCrashReporterStackTraces)
        config.truncateCrashReporterStackTraces = override
        assertEquals(override, config.truncateCrashReporterStackTraces)
        assertEquals(override, config.options[CRASH_REPORTER_TRUNCATE_STACK_TRACES])

        config.truncateCrashReporterStackTraces = null
        assertNull(config.truncateCrashReporterStackTraces)
        assertNull(config.options[CRASH_REPORTER_TRUNCATE_STACK_TRACES])
    }

    @Test
    fun modules_PointsToFactory() {
        assertSame(CrashReporter, Modules.CrashReporter)
    }
}
