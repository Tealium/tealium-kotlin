package com.tealium.crashreporter

import android.app.Application
import android.content.SharedPreferences
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.crashreporter.internal.CrashHandler
import io.mockk.*
import io.mockk.impl.annotations.MockK
import junit.framework.Assert.assertNotNull
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class CrashReporterTests {

    @MockK
    lateinit var mockContext: Application

    @MockK
    lateinit var mockFile: File

    @MockK
    lateinit var mockSharedPreferences: SharedPreferences

    @MockK
    lateinit var mockEditor: SharedPreferences.Editor

    @MockK
    lateinit var mockTealiumContext: TealiumContext

    @MockK
    lateinit var mockConfig: TealiumConfig

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { mockTealiumContext.config } returns mockConfig
        every { mockConfig.application } returns mockContext
        every { mockConfig.accountName } returns "account_1"
        every { mockConfig.profileName } returns "profile_1"
        every { mockConfig.environment } returns Environment.DEV
        every { mockConfig.options } returns mutableMapOf()
        every { mockContext.filesDir } returns mockFile

        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor
        every { mockEditor.apply() } just Runs
        every { mockSharedPreferences.getInt(CrashHandler.CRASH_COUNT, 0) } returns 0
        every { mockSharedPreferences.getString(CrashHandler.CRASH_BUILD_ID, any()) } returns "buildId"
        every { mockTealiumContext.track(any()) } just Runs
    }

    @Test
    fun sharedPrefs_NameIsUnique() {
        val config1: TealiumConfig = mockk()
        every { config1.accountName } returns "account_1"
        every { config1.profileName } returns "profile_1"
        every { config1.environment } returns Environment.DEV
        val config2: TealiumConfig = mockk()
        every { config2.accountName } returns "account_2"
        every { config2.profileName } returns "profile_2"
        every { config2.environment } returns Environment.DEV

        val sharedPrefsName1 = CrashReporter.getSharedPreferencesName(config1)
        val sharedPrefsName2 = CrashReporter.getSharedPreferencesName(config2)
        assertNotEquals(sharedPrefsName1, sharedPrefsName2)
    }

    @Test
    fun factory_CreatesNewInstance() {
        val crashReporter = CrashReporter.create(mockTealiumContext)
        assertNotNull(crashReporter)
    }

    @Test
    fun factory_CreatesUniqueInstances() {
        val crashReporter1 = CrashReporter.create(mockTealiumContext)
        val crashReporter2 = CrashReporter.create(mockTealiumContext)
        assertNotSame(crashReporter1, crashReporter2)
    }

    @Test
    fun factory_MultipleInstances_ChainsHandlers() {
        val mockExceptionHandler: Thread.UncaughtExceptionHandler = mockk(relaxed = true)
        Thread.setDefaultUncaughtExceptionHandler(mockExceptionHandler)
        val crashReporter1 = CrashReporter(mockTealiumContext)
        val crashReporter2 = CrashReporter(mockTealiumContext)

        assertSame(crashReporter2.exceptionHandler, Thread.getDefaultUncaughtExceptionHandler())
        assertSame(crashReporter1.exceptionHandler, (crashReporter2.exceptionHandler as CrashHandler).originalExceptionHandler)
        assertSame(mockExceptionHandler, (crashReporter1.exceptionHandler as CrashHandler).originalExceptionHandler)
    }
}