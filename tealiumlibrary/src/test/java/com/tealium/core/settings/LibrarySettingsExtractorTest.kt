package com.tealium.core.settings

import org.junit.Assert.*
import org.junit.Test

class LibrarySettingsExtractorTest {

    @Test
    fun convertMinutesSuccess() {
        val time = "10m"
        val result = LibrarySettingsExtractor.timeConverter(time)
        assertEquals(600, result)
    }

    @Test
    fun convertHoursSuccess() {
        val time = "3h"
        val result = LibrarySettingsExtractor.timeConverter(time)
        assertEquals(10800, result)
    }

    @Test
    fun convertDaysSuccess() {
        val time = "2d"
        val result = LibrarySettingsExtractor.timeConverter(time)
        assertEquals(172800, result)
    }

    @Test
    fun convertSecondsSuccess() {
        val time = "10s"
        val result = LibrarySettingsExtractor.timeConverter(time)
        assertEquals(10, result)
    }

    @Test
    fun convertMinutesWithSpace() {
        val time = "10 m"
        val result = LibrarySettingsExtractor.timeConverter(time)
        assertEquals(600, result)
    }

    @Test
    fun convertMinutesWithExtraSpaces() {
        val time = "10      m"
        val result = LibrarySettingsExtractor.timeConverter(time)
        assertEquals(600, result)
    }

    @Test
    fun convertSecondsWithExtraSpaces() {
        val time = "10  s"
        val result = LibrarySettingsExtractor.timeConverter(time)
        assertEquals(10, result)
    }

    @Test
    fun convertMinutesFailure() {
        val time = "Am"
        val result = LibrarySettingsExtractor.timeConverter(time)
        assertEquals(-1, result)
    }

    @Test
    fun convertHoursFailure() {
        val time = "A   h"
        val result = LibrarySettingsExtractor.timeConverter(time)
        assertEquals(-1, result)
    }

    @Test
    fun convertDaysFailure() {
        val time = "A d"
        val result = LibrarySettingsExtractor.timeConverter(time)
        assertEquals(-1, result)
    }
}