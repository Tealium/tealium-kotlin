package com.tealium.lifecycle

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class LifecycleServiceTest {

    @RelaxedMockK
    private lateinit var mockLifecycleSharedPreferences: LifecycleSharedPreferences

    private lateinit var lifecycleService: LifecycleService

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        lifecycleService = LifecycleService(mockLifecycleSharedPreferences)
    }

    @Test
    fun didDetectCrash_ShouldBeFalse() {
        every { mockLifecycleSharedPreferences.lastLifecycleEvent } returns null
        assertFalse(lifecycleService.didDetectCrash(LifecycleEvent.LAUNCH))

        every { mockLifecycleSharedPreferences.lastLifecycleEvent } returns LifecycleEvent.SLEEP
        assertFalse(lifecycleService.didDetectCrash(LifecycleEvent.WAKE))

        every { mockLifecycleSharedPreferences.lastLifecycleEvent } returns LifecycleEvent.WAKE
        assertFalse(lifecycleService.didDetectCrash(LifecycleEvent.SLEEP))

        every { mockLifecycleSharedPreferences.lastLifecycleEvent } returns LifecycleEvent.SLEEP
        assertFalse(lifecycleService.didDetectCrash(LifecycleEvent.LAUNCH))

        every { mockLifecycleSharedPreferences.lastLifecycleEvent } returns LifecycleEvent.SLEEP
        assertFalse(lifecycleService.didDetectCrash(LifecycleEvent.WAKE))
    }

    @Test
    fun didDetectCrash_ShouldBeTrue() {
        every { mockLifecycleSharedPreferences.lastLifecycleEvent } returns LifecycleEvent.LAUNCH
        assertTrue(lifecycleService.didDetectCrash(LifecycleEvent.LAUNCH))

        every { mockLifecycleSharedPreferences.lastLifecycleEvent } returns LifecycleEvent.LAUNCH
        assertTrue(lifecycleService.didDetectCrash(LifecycleEvent.WAKE))

        every { mockLifecycleSharedPreferences.lastLifecycleEvent } returns LifecycleEvent.WAKE
        assertTrue(lifecycleService.didDetectCrash(LifecycleEvent.LAUNCH))

        every { mockLifecycleSharedPreferences.lastLifecycleEvent } returns LifecycleEvent.WAKE
        assertTrue(lifecycleService.didDetectCrash(LifecycleEvent.WAKE))
    }

    @Test
    fun didUpdate_ShouldBeFalse() {
        every { mockLifecycleSharedPreferences.currentAppVersion } returns "10"
        assertFalse(lifecycleService.didUpdate(1L, "10"))

        every { mockLifecycleSharedPreferences.currentAppVersion } returns null
        assertFalse(lifecycleService.didUpdate(1L, "10"))
    }

    @Test
    fun didUpdate_ShouldBeTrue() {
        every { mockLifecycleSharedPreferences.currentAppVersion } returns "10"
        assertTrue(lifecycleService.didUpdate(1L, "11"))

        verify {
            mockLifecycleSharedPreferences.resetCountsAfterAppUpdate(1L, "11")
        }
    }

    @Test
    fun daysSince_ValidStartAndEndDates_IsPositive() {
        val currentTimeMs = getCurrentTime()
        var futureTimeMs = addDays(currentTimeMs, 3)

        var daysSince = LifecycleService.daysSince(currentTimeMs, futureTimeMs)
        assertEquals(3L, daysSince)

        futureTimeMs = addDays(currentTimeMs, 10)
        daysSince = LifecycleService.daysSince(currentTimeMs, futureTimeMs)
        assertEquals(10L, daysSince)
    }

    @Test
    fun daysSince_NegativeStartAndEndDates_ReturnsNull() {
        val currentTimeMs = getCurrentTime()
        var futureTimeMs = Long.MIN_VALUE

        var daysSince = LifecycleService.daysSince(currentTimeMs, futureTimeMs)
        assertEquals(null, daysSince)

        futureTimeMs = -1L
        daysSince = LifecycleService.daysSince(currentTimeMs, futureTimeMs)
        assertEquals(null, daysSince)

        daysSince = LifecycleService.daysSince(currentTimeMs, currentTimeMs)
        assertEquals(0L, daysSince)

        daysSince = LifecycleService.daysSince(Long.MIN_VALUE, 0L)
        assertEquals(null, daysSince)
    }

    @Test
    fun setFirstLaunch_ReturnsCurrentDate_WhenTimestampFirstLaunch_IsNull() {
        every { mockLifecycleSharedPreferences.timestampFirstLaunch } returns null

        val currentDate = getCurrentTime()
        val currentDateFormatMmDdYyyy = SimpleDateFormat("MM/dd/yyyy", Locale.ROOT)
        currentDateFormatMmDdYyyy.timeZone = TimeZone.getTimeZone("UTC")

        val firstLaunchDateMmDdYyyy = lifecycleService.setFirstLaunchMmDdYyyy(currentDate)

        assertEquals(currentDateFormatMmDdYyyy.format(currentDate), firstLaunchDateMmDdYyyy)
    }

    @Test
    fun setFormattedEvent_lastLaunch_SavesValue() {
        val expectedFormattedDate = "2023-03-01T00:00:00Z"
        every { mockLifecycleSharedPreferences.getLastEvent(LifecycleSPKey.TIMESTAMP_LAST_LAUNCH) } returns 1677628800000L

        lifecycleService.setFormattedEvent(LifecycleSPKey.TIMESTAMP_LAST_LAUNCH)

        assertEquals(expectedFormattedDate, lifecycleService.lastLaunchString)
    }

    @Test
    fun setFormattedEvent_lastWake_SavesValue() {
        val expectedFormattedDate = "2023-03-01T00:00:00Z"
        every { mockLifecycleSharedPreferences.getLastEvent(LifecycleSPKey.TIMESTAMP_LAST_WAKE) } returns 1677628800000L

        lifecycleService.setFormattedEvent(LifecycleSPKey.TIMESTAMP_LAST_WAKE)

        assertEquals(expectedFormattedDate, lifecycleService.lastWakeString)
    }

    @Test
    fun setFormattedEvent_LastSleep_SavesValue() {
        val expectedFormattedDate = "2023-03-01T00:00:00Z"
        every { mockLifecycleSharedPreferences.getLastEvent(LifecycleSPKey.TIMESTAMP_LAST_SLEEP) } returns 1677628800000L

        lifecycleService.setFormattedEvent(LifecycleSPKey.TIMESTAMP_LAST_SLEEP)

        assertEquals(expectedFormattedDate, lifecycleService.lastSleepString)
    }

    private fun getCurrentTime(): Long {
        return System.currentTimeMillis()
    }

    private fun addDays(timestampMs: Long, days: Long): Long {
        return timestampMs + TimeUnit.DAYS.toMillis(days)
    }
}