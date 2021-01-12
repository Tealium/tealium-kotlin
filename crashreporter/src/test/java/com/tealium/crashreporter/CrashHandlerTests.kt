package com.tealium.crashreporter

import android.app.Application
import android.content.SharedPreferences
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.crashreporter.internal.CrashHandler
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CrashHandlerTests {

    @MockK
    lateinit var mockContext: Application

    @MockK
    lateinit var mockSharedPreferences: SharedPreferences

    @MockK
    lateinit var mockEditor: SharedPreferences.Editor

    @MockK
    lateinit var mockTealiumContext: TealiumContext

    @MockK
    lateinit var mockConfig: TealiumConfig

    @RelaxedMockK
    lateinit var mockHandler: Thread.UncaughtExceptionHandler

    private lateinit var crashHandler: CrashHandler
    private val thread = Thread()
    private val exception = RuntimeException("crash")

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor
        every { mockEditor.apply() } just Runs
        every { mockEditor.commit() } returns true
        every { mockSharedPreferences.getInt(CrashHandler.CRASH_COUNT, 0) } returns 0
        every { mockSharedPreferences.getString(CrashHandler.CRASH_BUILD_ID, any()) } returns "buildId"
        every { mockTealiumContext.config } returns mockConfig
        every { mockConfig.application } returns mockContext
        every { mockConfig.truncateCrashReporterStackTraces } returns null
        every { mockTealiumContext.track(any()) } just Runs
        every { mockTealiumContext.events } returns mockk(relaxed = true)
    }

    @Test
    fun crashCount_IsSetFromStorage() {
        every { mockSharedPreferences.getInt(CrashHandler.CRASH_COUNT, 0) } returns 5
        crashHandler = CrashHandler(mockTealiumContext, mockSharedPreferences)
        assertEquals(5, crashHandler.crashCount)
    }

    @Test
    fun crashCount_SavesToStorage() {
        every { mockSharedPreferences.getInt(CrashHandler.CRASH_COUNT, 0) } returns 5
        crashHandler = CrashHandler(mockTealiumContext, mockSharedPreferences)
        assertEquals(5, crashHandler.crashCount)
        crashHandler.uncaughtException(thread, exception)

        assertEquals(6, crashHandler.crashCount)
        verify {
            mockEditor.putInt(CrashHandler.CRASH_COUNT, 6)
        }
    }

    @Test
    fun buildId_IsSetFromStorage() {
        val savedBuildId = UUID.randomUUID().toString()
        every { mockSharedPreferences.getString(CrashHandler.CRASH_BUILD_ID, null) } returns savedBuildId
        crashHandler = CrashHandler(mockTealiumContext, mockSharedPreferences)
        assertEquals(savedBuildId, crashHandler.buildId)
    }

    @Test
    fun buildId_IsGeneratedAndSaved_WhenNull() {
        val savedBuildId: String? = null
        every { mockSharedPreferences.getString(CrashHandler.CRASH_BUILD_ID, null) } returns savedBuildId
        crashHandler = CrashHandler(mockTealiumContext, mockSharedPreferences)

        assertFalse(crashHandler.buildId.isEmpty())
        verify {
            mockEditor.putString(CrashHandler.CRASH_BUILD_ID, any())
        }
    }

    @Test
    fun uncaughtException_PersistsCrashData() {
        crashHandler = CrashHandler(mockTealiumContext, mockSharedPreferences)
        crashHandler.uncaughtException(thread, exception)

        verify {
            mockEditor.putString(CrashHandler.CRASH_EXCEPTION_CAUSE, "java.lang.RuntimeException")
            mockEditor.putString(CrashHandler.CRASH_EXCEPTION_NAME, "crash")
            mockEditor.putString(CrashHandler.CRASH_UUID, any())
            mockEditor.putString(CrashHandler.CRASH_THREADS, any())
        }
    }

    @Test
    fun uncaughtException_CallsOriginalHandler() {
        val crashHandler = CrashHandler(mockTealiumContext, mockSharedPreferences, mockHandler)
        crashHandler.uncaughtException(thread, exception)
        verify {
            mockHandler.uncaughtException(thread, exception)
        }
    }

    @Test
    fun onActivityResumed_SendsStoredCrashData() {
        every { mockSharedPreferences.getInt(CrashHandler.CRASH_COUNT, 0) } returns 5
        every { mockSharedPreferences.getString(CrashHandler.CRASH_EXCEPTION_CAUSE, null) } returns "java.lang.RuntimeException"
        every { mockSharedPreferences.getString(CrashHandler.CRASH_EXCEPTION_NAME, null) } returns "crash"
        every { mockSharedPreferences.getString(CrashHandler.CRASH_BUILD_ID, null) } returns "buildId"
        every { mockSharedPreferences.getString(CrashHandler.CRASH_UUID, null) } returns "uuid"
        every { mockSharedPreferences.getString(CrashHandler.CRASH_THREADS, null) } returns "thread_data"
        every { mockEditor.remove(any()) } returns mockEditor

        crashHandler = CrashHandler(mockTealiumContext, mockSharedPreferences)
        crashHandler.onActivityResumed(null)

        verify {
            mockTealiumContext.track(match {
                it[CrashHandler.CRASH_COUNT] == 5
                        && it[CrashHandler.CRASH_EXCEPTION_CAUSE] == "java.lang.RuntimeException"
                        && it[CrashHandler.CRASH_EXCEPTION_NAME] == "crash"
                        && it[CrashHandler.CRASH_BUILD_ID] == "buildId"
                        && it[CrashHandler.CRASH_UUID] == "uuid"
                        && it[CrashHandler.CRASH_THREADS] == "thread_data"
            })
        }
    }

    @Test
    fun sendCrashData_SendsDataWhenAvailable() {
        every { mockSharedPreferences.getInt(CrashHandler.CRASH_COUNT, 0) } returns 5
        every { mockSharedPreferences.getString(CrashHandler.CRASH_EXCEPTION_CAUSE, null) } returns "java.lang.RuntimeException"
        every { mockSharedPreferences.getString(CrashHandler.CRASH_EXCEPTION_NAME, null) } returns "crash"
        every { mockSharedPreferences.getString(CrashHandler.CRASH_BUILD_ID, null) } returns "buildId"
        every { mockSharedPreferences.getString(CrashHandler.CRASH_UUID, null) } returns "uuid"
        every { mockSharedPreferences.getString(CrashHandler.CRASH_THREADS, null) } returns "thread_data"
        every { mockEditor.remove(any()) } returns mockEditor

        crashHandler = CrashHandler(mockTealiumContext, mockSharedPreferences)
        crashHandler.onActivityResumed(null)

        verify {
            mockTealiumContext.track(match {
                it[CrashHandler.CRASH_COUNT] == 5
                        && it[CrashHandler.CRASH_EXCEPTION_CAUSE] == "java.lang.RuntimeException"
                        && it[CrashHandler.CRASH_EXCEPTION_NAME] == "crash"
                        && it[CrashHandler.CRASH_BUILD_ID] == "buildId"
                        && it[CrashHandler.CRASH_UUID] == "uuid"
                        && it[CrashHandler.CRASH_THREADS] == "thread_data"
            })
        }
    }

    @Test
    fun sendCrashData_SendsPartialDataWhenAvailable() {
        every { mockSharedPreferences.getInt(CrashHandler.CRASH_COUNT, 0) } returns 5
        every { mockSharedPreferences.getString(CrashHandler.CRASH_EXCEPTION_CAUSE, null) } returns "java.lang.RuntimeException"
        every { mockSharedPreferences.getString(CrashHandler.CRASH_EXCEPTION_NAME, null) } returns null
        every { mockSharedPreferences.getString(CrashHandler.CRASH_BUILD_ID, null) } returns "buildId"
        every { mockSharedPreferences.getString(CrashHandler.CRASH_UUID, null) } returns "uuid"
        every { mockSharedPreferences.getString(CrashHandler.CRASH_THREADS, null) } returns null
        every { mockEditor.remove(any()) } returns mockEditor

        crashHandler = CrashHandler(mockTealiumContext, mockSharedPreferences)
        crashHandler.onActivityResumed(null)

        verify {
            mockTealiumContext.track(match {
                it[CrashHandler.CRASH_COUNT] == 5
                        && it[CrashHandler.CRASH_EXCEPTION_CAUSE] == "java.lang.RuntimeException"
                        && it[CrashHandler.CRASH_BUILD_ID] == "buildId"
                        && it[CrashHandler.CRASH_UUID] == "uuid"
                        && !it.payload().containsKey(CrashHandler.CRASH_EXCEPTION_NAME)
                        && !it.payload().containsKey(CrashHandler.CRASH_THREADS)
            })
        }
    }

    @Test
    fun sendCrashData_DoesNotSendDataWhenUnavailable() {
        every { mockSharedPreferences.getInt(CrashHandler.CRASH_COUNT, 0) } returns 5
        every { mockSharedPreferences.getString(CrashHandler.CRASH_EXCEPTION_CAUSE, null) } returns null
        every { mockSharedPreferences.getString(CrashHandler.CRASH_EXCEPTION_NAME, null) } returns null
        every { mockSharedPreferences.getString(CrashHandler.CRASH_BUILD_ID, null) } returns "buildId"
        every { mockSharedPreferences.getString(CrashHandler.CRASH_UUID, null) } returns null
        every { mockSharedPreferences.getString(CrashHandler.CRASH_THREADS, null) } returns null

        crashHandler = CrashHandler(mockTealiumContext, mockSharedPreferences)
        crashHandler.onActivityResumed(null)

        verify(exactly = 0) {
            mockTealiumContext.track(any())
        }
    }

    @Test
    fun sendCrashData_ClearsDataAfterSending() {
        every { mockSharedPreferences.getInt(CrashHandler.CRASH_COUNT, 0) } returns 5
        every { mockSharedPreferences.getString(CrashHandler.CRASH_EXCEPTION_CAUSE, null) } returns "java.lang.RuntimeException"
        every { mockSharedPreferences.getString(CrashHandler.CRASH_EXCEPTION_NAME, null) } returns "crash"
        every { mockSharedPreferences.getString(CrashHandler.CRASH_BUILD_ID, null) } returns "buildId"
        every { mockSharedPreferences.getString(CrashHandler.CRASH_UUID, null) } returns "uuid"
        every { mockSharedPreferences.getString(CrashHandler.CRASH_THREADS, null) } returns "thread_data"
        every { mockEditor.remove(any()) } returns mockEditor

        crashHandler = CrashHandler(mockTealiumContext, mockSharedPreferences)
        crashHandler.onActivityResumed(null)

        verify {
            mockTealiumContext.track(any())
            mockEditor.remove(CrashHandler.CRASH_EXCEPTION_CAUSE)
            mockEditor.remove(CrashHandler.CRASH_EXCEPTION_NAME)
            mockEditor.remove(CrashHandler.CRASH_UUID)
            mockEditor.remove(CrashHandler.CRASH_THREADS)
        }
    }

    @Test
    fun clearSavedCrashData_RemovesDataFromStorage() {
        every { mockEditor.remove(CrashHandler.CRASH_EXCEPTION_CAUSE) } returns mockEditor
        every { mockEditor.remove(CrashHandler.CRASH_EXCEPTION_NAME) } returns mockEditor
        every { mockEditor.remove(CrashHandler.CRASH_UUID) } returns mockEditor
        every { mockEditor.remove(CrashHandler.CRASH_THREADS) } returns mockEditor

        crashHandler = CrashHandler(mockTealiumContext, mockSharedPreferences)
        crashHandler.clearSavedCrashData()
        verify {
            mockEditor.remove(CrashHandler.CRASH_EXCEPTION_CAUSE)
            mockEditor.remove(CrashHandler.CRASH_EXCEPTION_NAME)
            mockEditor.remove(CrashHandler.CRASH_UUID)
            mockEditor.remove(CrashHandler.CRASH_THREADS)
        }
    }

    @Test
    fun truncateThreads_SetsFromConfig() {
        every { mockConfig.truncateCrashReporterStackTraces } returns true
        crashHandler = CrashHandler(mockTealiumContext, mockSharedPreferences)
        assertTrue(crashHandler.truncateStackTraces)

        every { mockConfig.truncateCrashReporterStackTraces } returns false
        crashHandler = CrashHandler(mockTealiumContext, mockSharedPreferences)
        assertFalse(crashHandler.truncateStackTraces)

        every { mockConfig.truncateCrashReporterStackTraces } returns null
        crashHandler = CrashHandler(mockTealiumContext, mockSharedPreferences)
        assertFalse(crashHandler.truncateStackTraces)
    }
}