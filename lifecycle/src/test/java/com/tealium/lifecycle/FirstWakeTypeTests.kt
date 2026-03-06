package com.tealium.lifecycle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class FirstWakeTypeTests {

    @Test
    fun testEnumValues() {
        assertEquals(3, FirstWakeType.Month.value)
        assertEquals(2, FirstWakeType.Today.value)
        assertEquals(0, FirstWakeType.Neither.value)
    }

    @Test
    fun testIsFirstWakeMonth() {
        assertTrue(FirstWakeType.Month.isFirstWakeMonth)
        assertFalse(FirstWakeType.Today.isFirstWakeMonth)
        assertFalse(FirstWakeType.Neither.isFirstWakeMonth)
    }

    @Test
    fun testIsFirstWakeToday() {
        assertTrue(FirstWakeType.Month.isFirstWakeToday)
        assertTrue(FirstWakeType.Today.isFirstWakeToday)
        assertFalse(FirstWakeType.Neither.isFirstWakeToday)
    }

    @Test
    fun testFromTimestamps_SameDay() {
        val calendar = Calendar.getInstance()
        calendar.set(2023, Calendar.JANUARY, 15, 10, 0, 0)
        val timestamp1 = calendar.timeInMillis

        calendar.set(2023, Calendar.JANUARY, 15, 14, 30, 0)
        val timestamp2 = calendar.timeInMillis

        val result = FirstWakeType.fromTimestamps(timestamp1, timestamp2, calendar)
        assertEquals(FirstWakeType.Neither, result)
    }

    @Test
    fun testFromTimestamps_DifferentDay_SameMonth() {
        val calendar = Calendar.getInstance()
        calendar.set(2023, Calendar.JANUARY, 15, 10, 0, 0)
        val timestamp1 = calendar.timeInMillis

        calendar.set(2023, Calendar.JANUARY, 16, 14, 30, 0)
        val timestamp2 = calendar.timeInMillis

        val result = FirstWakeType.fromTimestamps(timestamp1, timestamp2, calendar)
        assertEquals(FirstWakeType.Today, result)
    }

    @Test
    fun testFromTimestamps_DifferentMonth_SameYear() {
        val calendar = Calendar.getInstance()
        calendar.set(2023, Calendar.JANUARY, 15, 10, 0, 0)
        val timestamp1 = calendar.timeInMillis

        calendar.set(2023, Calendar.FEBRUARY, 16, 14, 30, 0)
        val timestamp2 = calendar.timeInMillis

        val result = FirstWakeType.fromTimestamps(timestamp1, timestamp2, calendar)
        assertEquals(FirstWakeType.Month, result)
    }

    @Test
    fun testFromTimestamps_DifferentYear() {
        val calendar = Calendar.getInstance()
        calendar.set(2022, Calendar.DECEMBER, 31, 23, 59, 0)
        val timestamp1 = calendar.timeInMillis

        calendar.set(2023, Calendar.JANUARY, 1, 0, 1, 0)
        val timestamp2 = calendar.timeInMillis

        val result = FirstWakeType.fromTimestamps(timestamp1, timestamp2, calendar)
        assertEquals(FirstWakeType.Month, result)
    }

    @Test
    fun testFromTimestamps_SameMonth_DifferentYear() {
        val calendar = Calendar.getInstance()
        calendar.set(2022, Calendar.JANUARY, 15, 10, 0, 0)
        val timestamp1 = calendar.timeInMillis

        calendar.set(2023, Calendar.JANUARY, 16, 14, 30, 0)
        val timestamp2 = calendar.timeInMillis

        val result = FirstWakeType.fromTimestamps(timestamp1, timestamp2, calendar)
        assertEquals(FirstWakeType.Month, result)
    }

    @Test
    fun testFromTimestamps_LastDayOfMonth_FirstDayOfNextMonth() {
        val calendar = Calendar.getInstance()
        calendar.set(2023, Calendar.JANUARY, 31, 23, 59, 0)
        val timestamp1 = calendar.timeInMillis

        calendar.set(2023, Calendar.FEBRUARY, 1, 0, 1, 0)
        val timestamp2 = calendar.timeInMillis

        val result = FirstWakeType.fromTimestamps(timestamp1, timestamp2, calendar)
        assertEquals(FirstWakeType.Month, result)
    }

    @Test
    fun testFromTimestamps_LeapYear() {
        val calendar = Calendar.getInstance()
        calendar.set(2020, Calendar.FEBRUARY, 28, 10, 0, 0)
        val timestamp1 = calendar.timeInMillis

        calendar.set(2020, Calendar.FEBRUARY, 29, 14, 30, 0)
        val timestamp2 = calendar.timeInMillis

        val result = FirstWakeType.fromTimestamps(timestamp1, timestamp2, calendar)
        assertEquals(FirstWakeType.Today, result)
    }

    @Test
    fun testFromTimestamps_ReversedTimestamps() {
        val calendar = Calendar.getInstance()
        calendar.set(2023, Calendar.FEBRUARY, 16, 14, 30, 0)
        val timestamp1 = calendar.timeInMillis

        calendar.set(2023, Calendar.JANUARY, 15, 10, 0, 0)
        val timestamp2 = calendar.timeInMillis

        val result = FirstWakeType.fromTimestamps(timestamp1, timestamp2, calendar)
        assertEquals(FirstWakeType.Month, result)
    }

    @Test
    fun testFromTimestamps_IdenticalTimestamps() {
        val calendar = Calendar.getInstance()
        calendar.set(2023, Calendar.JANUARY, 15, 10, 0, 0)
        val timestamp = calendar.timeInMillis

        val result = FirstWakeType.fromTimestamps(timestamp, timestamp, calendar)
        assertEquals(FirstWakeType.Neither, result)
    }

    @Test
    fun testFromTimestamps_CrossingDayOfYear() {
        val calendar = Calendar.getInstance()
        // December 31st
        calendar.set(2022, Calendar.DECEMBER, 31, 10, 0, 0)
        val timestamp1 = calendar.timeInMillis

        // January 1st (different year, different day of year)
        calendar.set(2023, Calendar.JANUARY, 1, 14, 30, 0)
        val timestamp2 = calendar.timeInMillis

        val result = FirstWakeType.fromTimestamps(timestamp1, timestamp2, calendar)
        assertEquals(FirstWakeType.Month, result)
    }

    @Test
    fun testFromTimestamps_WithDefaultCalendar() {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - (60 * 60 * 1000)

        val result = FirstWakeType.fromTimestamps(oneHourAgo, now)
        // Result depends on current time, but should not crash
        assertNotNull(result)
        assertTrue(result in listOf(FirstWakeType.Neither, FirstWakeType.Today, FirstWakeType.Month))
    }
}