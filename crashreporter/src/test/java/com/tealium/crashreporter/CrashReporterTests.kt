package com.tealium.crashreporter

import android.app.Application
import android.content.SharedPreferences
import com.tealium.core.Environment
import com.tealium.core.Tealium
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.TealiumEvent
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.json.JSONObject
import org.junit.Assert
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
    lateinit var mockJSONObject: JSONObject

    @MockK
    lateinit var mockEditor: SharedPreferences.Editor

    lateinit var tealiumContext: TealiumContext
    lateinit var config: TealiumConfig
    lateinit var tealium: Tealium
    lateinit var reporter: CrashReporter
    lateinit var eventDispatch : Dispatch

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        tealium = mockk()
        mockJSONObject = mockk()

        val thread = Thread()
        val exception = RuntimeException("crash")
        val crash = Crash(thread, exception)

        eventDispatch =  TealiumEvent("crash")

        every { mockContext.filesDir } returns mockFile

        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.putInt(CrashReporter.CRASH_COUNT, 1) } returns mockEditor
        every { mockEditor.apply() } just Runs

        config = TealiumConfig(mockContext, "test_account", "test_profile", Environment.QA)
        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)
        reporter = spyk(CrashReporter(tealiumContext))

        every { mockSharedPreferences.contains(CrashReporter.CRASH_COUNT) } returns true
        every { mockSharedPreferences.getString(CrashReporter.CRASH_EXCEPTION_CAUSE, null) } returns "java.lang.RuntimeException"
        every { mockSharedPreferences.getString(CrashReporter.CRASH_EXCEPTION_NAME, null) } returns "crash"
        every { mockSharedPreferences.getString(CrashReporter.CRASH_BUILD_ID, null) } returns null
        every { mockSharedPreferences.getString(CrashReporter.CRASH_UUID, null) } returns crash.uUid
        every { mockSharedPreferences.getString(CrashReporter.CRASH_THREADS, null) } returns Crash.getThreadData(crash, false)

        every { tealiumContext.track(eventDispatch) } returns Unit
        every { mockEditor.remove(CrashReporter.CRASH_EXCEPTION_CAUSE) } returns mockEditor
        every { mockEditor.remove(CrashReporter.CRASH_EXCEPTION_NAME) } returns mockEditor
        every { mockEditor.remove(CrashReporter.CRASH_UUID) } returns mockEditor
        every { mockEditor.remove(CrashReporter.CRASH_THREADS) } returns mockEditor
    }

    // Verify exception data
    @Test
    fun testExceptionDataOnException() {
        val thread = Thread()
        val exception = RuntimeException("crash")
        val crash = Crash(thread, exception)
        reporter.saveCrashData(crash)

        val crashData: Map<String, Any>? = reporter.readSavedCrashData()

        Assert.assertEquals(crash.exceptionCause, crashData?.get(CrashReporter.CRASH_EXCEPTION_CAUSE))
        Assert.assertEquals(crash.exceptionName, crashData?.get(CrashReporter.CRASH_EXCEPTION_NAME))
    }

    // Verify UUID exists
    @Test
    fun testUuidOnException() {
        val thread = Thread()
        val exception = RuntimeException("crash")
        val crash = Crash(thread, exception)
        reporter.saveCrashData(crash)

        val crashData: Map<String, Any>? = reporter.readSavedCrashData()

        Assert.assertTrue(crashData?.get(CrashReporter.CRASH_UUID).toString().length.equals(crash.uUid.length))

    }

    // Verify thread data
    @Test
    fun testThreadDataOnException() {
        val th = Thread()
        val exception = RuntimeException("crash")
        val crash = Crash(th, exception)

        Assert.assertEquals(crash.thread.id, th.id)
        Assert.assertEquals(crash.thread.state, th.state)
        Assert.assertEquals(crash.thread.name, th.name)
        Assert.assertEquals(crash.thread.priority, th.priority)
    }

    @Test
    fun verifyCrashDataOnActivityResumed() {
        reporter.onActivityResumed()

        Assert.assertTrue(mockSharedPreferences.contains(CrashReporter.CRASH_COUNT))
    }

}