package com.tealium.crashreporter

import com.tealium.crashreporter.internal.Crash
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CrashTests {

    private val thread = Thread()
    private val exception = RuntimeException("crash")

    @Test
    fun crash_DefaultParameters_PopulateSuccessfully() {
        val crash = Crash(thread, exception)

        assertEquals("java.lang.RuntimeException", crash.exceptionCause)
        assertEquals("crash", crash.exceptionName)
        assertFalse(crash.uuid.isBlank())
        assertFalse(crash.threadState.isBlank())
        assertFalse(crash.threadNumber.isBlank())
        assertFalse(crash.threadId.isBlank())
        assertFalse(crash.threadPriority.isBlank())
    }


    @Test
    fun crash_GetThreadData_ReturnsThreadData() {
        val crash = Crash(thread, exception)
        val threadData = Crash.getThreadData(crash, false)

        val jsonObject = JSONObject(JSONArray(threadData).getString(0))

        assertEquals("true", jsonObject.get("crashed"))
        assertFalse(jsonObject.getString(Crash.KEY_THREAD_STATE).isBlank())
        assertFalse(jsonObject.getString(Crash.KEY_THREAD_NUMBER).isBlank())
        assertFalse(jsonObject.getString(Crash.KEY_THREAD_ID).isBlank())
        assertFalse(jsonObject.getString(Crash.KEY_THREAD_PRIORITY).isBlank())
        assertTrue(jsonObject.getJSONArray(Crash.KEY_THREAD_STACK).length() > 1)
    }

    @Test
    fun crash_GetThreadData_ReturnsThreadData_TruncatesStack() {
        val crash = Crash(thread, exception)
        val threadData = Crash.getThreadData(crash, true)

        val jsonObject = JSONObject(JSONArray(threadData).getString(0))

        assertEquals("true", jsonObject.get("crashed"))
        assertFalse(jsonObject.getString(Crash.KEY_THREAD_STATE).isBlank())
        assertFalse(jsonObject.getString(Crash.KEY_THREAD_NUMBER).isBlank())
        assertFalse(jsonObject.getString(Crash.KEY_THREAD_ID).isBlank())
        assertFalse(jsonObject.getString(Crash.KEY_THREAD_PRIORITY).isBlank())
        assertTrue(jsonObject.getJSONArray(Crash.KEY_THREAD_STACK).length() == 1)
    }
}