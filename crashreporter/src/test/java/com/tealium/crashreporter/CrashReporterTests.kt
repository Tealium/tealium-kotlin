package com.tealium.crashreporter

import android.app.Application
import android.content.SharedPreferences
import com.tealium.core.Environment
import com.tealium.core.Tealium
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File

class CrashReporterTests {

    @MockK
    lateinit var mockContext: Application

    @MockK
    lateinit var mockFile: File

    @MockK
    lateinit var mockSharedPreferences: SharedPreferences

    @MockK
    lateinit var mockJSONObject: JSONObject

    @MockK
    lateinit var mockEditor: SharedPreferences.Editor

    lateinit var tealiumContext: TealiumContext
    lateinit var config: TealiumConfig
    lateinit var tealium: Tealium
    lateinit var reporter: CrashReporter

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        tealium = mockk()
        mockJSONObject = mockk()
        every { mockContext.filesDir } returns mockFile

        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.putString("", "") } returns mockEditor
        every { mockEditor.putInt(CrashReporter.CRASH_COUNT, 1) } returns mockEditor
        every { mockEditor.putString(CrashReporter.CRASH_EXCEPTION_CAUSE, "name") } returns mockEditor
        every { mockEditor.apply() } just Runs

        every { mockJSONObject.put("", "") } returns mockJSONObject

        every { mockSharedPreferences.contains(CrashReporter.CRASH_EXCEPTION_CAUSE) } returns true
        every { mockSharedPreferences.contains(CrashReporter.CRASH_COUNT) } returns true

        every { mockSharedPreferences.getString(CrashReporter.CRASH_EXCEPTION_NAME, null) } returns null
        every { mockSharedPreferences.getString(CrashReporter.CRASH_EXCEPTION_CAUSE, null) } returns null
        every { mockSharedPreferences.getString(CrashReporter.CRASH_UUID, null) } returns null
        every { mockSharedPreferences.getString(CrashReporter.CRASH_THREADS, null) } returns null

        config = TealiumConfig(mockContext, "test_account", "test_profile", Environment.QA)
        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)
        reporter = spyk(CrashReporter(tealiumContext))
    }

    @Test
    fun testSaveCrashDataOnCrash() {
        val thread = Thread()
        val exception = RuntimeException("crash")
        reporter.uncaughtException(thread, exception)

        every { reporter.saveCrashData(any()) } just Runs
    }

    // verify crash data is saved
    @Test
    fun testCrashDataOnException() {
        val thread = Thread()
        val exception = RuntimeException("crash")
        reporter.uncaughtException(thread, exception)

        Assert.assertTrue(mockSharedPreferences.contains(CrashReporter.CRASH_EXCEPTION_CAUSE))
    }

    @Test
    fun verifyCrashDataOnActivityResumed() {
        reporter.onActivityResumed()

        Assert.assertTrue(mockSharedPreferences.contains(CrashReporter.CRASH_COUNT))
    }
}