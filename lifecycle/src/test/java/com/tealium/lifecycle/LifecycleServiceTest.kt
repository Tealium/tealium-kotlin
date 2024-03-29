package com.tealium.lifecycle

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
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

        every { mockLifecycleSharedPreferences.lastLifecycleEvent } returns LifecycleEvent.LAUNCH
        assertFalse(lifecycleService.didDetectCrash(LifecycleEvent.SLEEP))

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

        verify(exactly = 4) {
            mockLifecycleSharedPreferences.incrementCrash()
        }
    }

    @Test
    fun isFirstLaunch_ShouldBeFalse() {
        every { mockLifecycleSharedPreferences.timestampFirstLaunch } returns 1L
        assertFalse(lifecycleService.isFirstLaunch(1L))
    }

    @Test
    fun isFirstLaunch_ShouldBeTrue() {
        every { mockLifecycleSharedPreferences.timestampFirstLaunch } returns LifecycleDefaults.TIMESTAMP_INVALID
        assertTrue(lifecycleService.isFirstLaunch(1L))

        verify {
            mockLifecycleSharedPreferences.timestampFirstLaunch = 1L
            mockLifecycleSharedPreferences.timestampLastLaunch = 1L
            mockLifecycleSharedPreferences.timestampLastWake = 1L
        }
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
        assertEquals(3, daysSince)

        futureTimeMs = addDays(currentTimeMs, 10)
        daysSince = LifecycleService.daysSince(currentTimeMs, futureTimeMs)
        assertEquals(10, daysSince)
    }

    @Test
    fun daysSince_NegativeStartAndEndDates_IsAtLeastZero() {
        val currentTimeMs = getCurrentTime()
        var futureTimeMs = Long.MIN_VALUE

        var daysSince = LifecycleService.daysSince(currentTimeMs, futureTimeMs)
        assertEquals(0L, daysSince)

        futureTimeMs = -1L
        daysSince = LifecycleService.daysSince(currentTimeMs, futureTimeMs)
        assertEquals(0L, daysSince)

        daysSince = LifecycleService.daysSince(currentTimeMs, currentTimeMs)
        assertEquals(0L, daysSince)

        daysSince = LifecycleService.daysSince(Long.MIN_VALUE, 0L)
        assertEquals(0L, daysSince)
    }

    private fun getCurrentTime(): Long {
        return System.currentTimeMillis()
    }

    private fun addDays(timestampMs: Long, days: Long): Long {
        return timestampMs + TimeUnit.DAYS.toMillis(days)
    }
}