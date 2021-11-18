package com.tealium.core

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

class DateUtilsTest {

    @Test
    fun validFormatDate() {
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT)
        iso.timeZone = TimeZone.getTimeZone("UTC")

        val date = Date(1196676930000) // Time in millis for: 2007-12-03T10:15:30Z
        val formattedDate = DateUtils.formatDate(date)

        assertEquals("2007-12-03T10:15:30Z", formattedDate)
    }

    @Test
    fun validFormatZoneDateTime() {
        val now = ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Paris]")
        val formattedNow = DateUtils.formatZonedDateTime(now)

        assertEquals("2007-12-03T09:15:30Z", formattedNow)
    }

    @Test
    fun validFormatLocalDate() {
        val now = LocalDate.parse("2007-12-03")
        val formattedNow = DateUtils.formatLocalDate(now)

        assertEquals("2007-12-03T00:00:00Z", formattedNow)
    }

    @Test
    fun validFormatLocalDateTime() {
        val now = LocalDateTime.parse("2007-12-03T10:15:30")
        val formattedNow = DateUtils.formatLocalDateTime(now)

        assertEquals("2007-12-03T10:15:30Z", formattedNow)
    }

    @Test
    fun validFormatLocalTime() {
        val now = LocalTime.parse("10:15:30")
        val formattedNow = DateUtils.formatLocalTime(now)

        assertEquals("10:15:30", formattedNow)
    }

    @Test
    fun validFormatInstant() {
        val now = Instant.parse("2007-12-03T10:15:30.00Z")
        val formattedNow = DateUtils.formatInstant(now)

        assertEquals("2007-12-03T10:15:30Z", formattedNow)
    }
}