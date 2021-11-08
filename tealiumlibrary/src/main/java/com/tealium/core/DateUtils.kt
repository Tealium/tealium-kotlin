package com.tealium.core

import android.annotation.TargetApi
import android.os.Build
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

class DateUtils {
    companion object {
        private const val FORMAT_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'"
        private const val TIME_ZONE = "UTC"

        private var formatIso8601 = SimpleDateFormat(FORMAT_ISO_8601, Locale.ROOT)

        fun formatDate(date: Date): String {
            formatIso8601.timeZone = TimeZone.getTimeZone(TIME_ZONE)
            return formatIso8601.format(date)
        }

        @TargetApi(Build.VERSION_CODES.O)
        fun formatZonedDateTime(date: ZonedDateTime): String {
            return date.format(DateTimeFormatter.ISO_INSTANT)
        }

        @TargetApi(Build.VERSION_CODES.O)
        fun formatLocalDate(date: LocalDate): String {
            return date.atStartOfDay().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
        }

        @TargetApi(Build.VERSION_CODES.O)
        fun formatLocalDateTime(date: LocalDateTime): String {
            return date.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
        }

        @TargetApi(Build.VERSION_CODES.O)
        fun formatLocalTime(date: LocalTime): String {
            return date.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
        }

        @TargetApi(Build.VERSION_CODES.O)
        fun formatInstant(date: Instant): String {
            return date.toString()
        }
    }
}