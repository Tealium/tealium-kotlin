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

        val date = Date(System.currentTimeMillis())
        val formattedDate = DateUtils.formatDate(date)

        assertEquals(iso.format(date), formattedDate)
    }

    @Test
    fun validFormatZoneDateTime() {
        val now = ZonedDateTime.now()
        val formattedNow = DateUtils.formatZonedDateTime(now)

        assertEquals(now.format(DateTimeFormatter.ISO_INSTANT), formattedNow)
    }

    @Test
    fun validFormatLocalDate() {
        val now = LocalDate.now()
        val formattedNow = DateUtils.formatLocalDate(now)

        assertEquals(now.atStartOfDay(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT), formattedNow)
    }

    @Test
    fun validFormatLocalDateTime() {
        val now = LocalDateTime.now()
        val formattedNow = DateUtils.formatLocalDateTime(now)

        assertEquals(now.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT), formattedNow)
    }

    @Test
    fun validFormatLocalTime() {
        val now = LocalTime.now()
        val formattedNow = DateUtils.formatLocalTime(now)

        assertEquals(now.toString(), formattedNow)
    }

    @Test
    fun validFormatInstant() {
        val now = Instant.now()
        val formattedNow = DateUtils.formatInstant(now)

        assertEquals(now.toString(), formattedNow)
    }
}