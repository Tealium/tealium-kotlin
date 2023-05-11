package com.tealium.lifecycle

import java.text.SimpleDateFormat
import java.util.*

internal class LifecycleService(private val lifecycleSharedPreferences: LifecycleSharedPreferences) {

    private val calendar: Calendar = Calendar.getInstance()
    var formatIso8601 = SimpleDateFormat(LifecycleDefaults.FORMAT_ISO_8601, Locale.ROOT)
    var reusableDate: Date = Date(LifecycleDefaults.TIMESTAMP_INVALID)

    init {
        formatIso8601.timeZone = TimeZone.getTimeZone("UTC")
    }

    fun getCurrentState(timestamp: Long): MutableMap<String, Any> {
        val data = mutableMapOf<String, Any>(
            LifecycleStateKey.LIFECYCLE_DAYOFWEEK_LOCAL to getDayOfWeekLocal(timestamp),
            LifecycleStateKey.LIFECYCLE_DAYSSINCELAUNCH to lifecycleSharedPreferences.timestampFirstLaunch.let { firstLaunch ->
                daysSince(
                    startEventMs = firstLaunch,
                    endEventMs = timestamp
                ).toString()
            },
            LifecycleStateKey.LIFECYCLE_DAYSSINCELASTWAKE to lifecycleSharedPreferences.timestampLastWake.let { lastWake ->
                daysSince(
                    startEventMs = lastWake,
                    endEventMs = timestamp
                ).toString()
            },
            LifecycleStateKey.LIFECYCLE_DAYSSINCEUPDATE to lifecycleSharedPreferences.timestampUpdate.let { lastUpdate ->
                daysSince(
                    startEventMs = lastUpdate,
                    endEventMs = timestamp
                ).toString()
            },
            LifecycleStateKey.LIFECYCLE_HOUROFDAY_LOCAL to getHourOfDayLocal(timestamp).toString(),
            LifecycleStateKey.LIFECYCLE_LAUNCHCOUNT to lifecycleSharedPreferences.countLaunch,
            LifecycleStateKey.LIFECYCLE_SLEEPCOUNT to lifecycleSharedPreferences.countSleep,
            LifecycleStateKey.LIFECYCLE_WAKECOUNT to lifecycleSharedPreferences.countWake,
            LifecycleStateKey.LIFECYCLE_TOTALCRASHCOUNT to lifecycleSharedPreferences.countTotalCrash,
            LifecycleStateKey.LIFECYCLE_TOTALLAUNCHCOUNT to lifecycleSharedPreferences.countTotalLaunch,
            LifecycleStateKey.LIFECYCLE_TOTALSLEEPCOUNT to lifecycleSharedPreferences.countSleep.toString(),
            LifecycleStateKey.LIFECYCLE_TOTALWAKECOUNT to lifecycleSharedPreferences.countWake.toString(),
            LifecycleStateKey.LIFECYCLE_TOTALSECONDSAWAKE to lifecycleSharedPreferences.totalSecondsAwake.toString()
        )

        lifecycleSharedPreferences.firstLaunch?.let {
            data[LifecycleStateKey.LIFECYCLE_FIRSTLAUNCHDATE] = it
        } ?: run {
            val launch = lifecycleSharedPreferences.setFirstLaunch(timestamp)
            launch?.let {
                data[LifecycleStateKey.LIFECYCLE_FIRSTLAUNCHDATE] = it
            }
        }

        lifecycleSharedPreferences.firstLaunchMmDdYyyy?.let {
            data[LifecycleStateKey.LIFECYCLE_FIRSTLAUNCHDATE_MMDDYYYY] = it
        } ?: run {
            val launchDataMmDdYyyy = lifecycleSharedPreferences.setFirstLaunchMmDdYyyy(timestamp)
            launchDataMmDdYyyy?.let {
                data[LifecycleStateKey.LIFECYCLE_FIRSTLAUNCHDATE_MMDDYYYY] = it
            }
        }

        lifecycleSharedPreferences.lastLaunch?.let {
            data[LifecycleStateKey.LIFECYCLE_LASTLAUNCHDATE] = it
        } ?: run {
            lifecycleSharedPreferences.lastLaunch = getLastEvent(
                LifecycleSPKey.TIMESTAMP_LAST_LAUNCH,
                lifecycleSharedPreferences.timestampLaunch
            )
        }

        lifecycleSharedPreferences.lastWake?.let {
            data[LifecycleStateKey.LIFECYCLE_LASTWAKEDATE] = it
        } ?: run {
            lifecycleSharedPreferences.lastWake = getLastEvent(
                LifecycleSPKey.TIMESTAMP_LAST_WAKE,
                lifecycleSharedPreferences.timestampLaunch
            )
        }

        lifecycleSharedPreferences.lastSleep?.let {
            data[LifecycleStateKey.LIFECYCLE_LASTSLEEPDATE] = it
        } ?: run {
            lifecycleSharedPreferences.lastSleep = getLastEvent(
                LifecycleSPKey.TIMESTAMP_LAST_SLEEP,
                LifecycleDefaults.TIMESTAMP_INVALID
            )
        }

        if (lifecycleSharedPreferences.timestampUpdate != LifecycleDefaults.TIMESTAMP_INVALID) {
            lifecycleSharedPreferences.updateLaunchDate?.let {
                data[LifecycleStateKey.LIFECYCLE_UPDATELAUNCHDATE] = it
            } ?: run {
                lifecycleSharedPreferences.updateLaunchDate = getUpdateLaunchDate()
            }
        }

        return data
            .filterNot { it.value == LifecycleDefaults.TIMESTAMP_INVALID }.toMutableMap()
    }

    fun didDetectCrash(event: String): Boolean {
        val lastEvent = lifecycleSharedPreferences.lastLifecycleEvent

        lastEvent?.let {
            val lastIsForegrounding = LifecycleEvent.LAUNCH == it || LifecycleEvent.WAKE == it
            val currentIsForegrounding =
                LifecycleEvent.LAUNCH == event || LifecycleEvent.WAKE == event

            val crashDetected = lastIsForegrounding && currentIsForegrounding

            if (crashDetected) {
                lifecycleSharedPreferences.incrementCrash()
            }

            return crashDetected
        }
        return false
    }

    fun isFirstLaunch(timestampLaunch: Long): Boolean {
        if (lifecycleSharedPreferences.timestampFirstLaunch == LifecycleDefaults.TIMESTAMP_INVALID) {
            lifecycleSharedPreferences.timestampFirstLaunch = timestampLaunch
            lifecycleSharedPreferences.timestampLastLaunch = timestampLaunch
            lifecycleSharedPreferences.timestampLastWake = timestampLaunch
            return true
        }

        return false
    }

    fun didUpdate(timestamp: Long, initializedCurrentVersion: String): Boolean {
        val cachedVersion = lifecycleSharedPreferences.currentAppVersion

        if (cachedVersion == null) {
            lifecycleSharedPreferences.currentAppVersion = initializedCurrentVersion
        } else if (initializedCurrentVersion != cachedVersion) {
            lifecycleSharedPreferences.resetCountsAfterAppUpdate(
                timestamp,
                initializedCurrentVersion
            )
            return true
        }

        return false
    }

    fun getUpdateLaunchDate(): String? {
        if (lifecycleSharedPreferences.timestampUpdate == LifecycleDefaults.TIMESTAMP_INVALID) {
            return null
        }

        reusableDate.time = lifecycleSharedPreferences.timestampUpdate
        return formatIso8601.format(reusableDate)
    }

    fun getLastEvent(eventKey: String, fallback: Long): String? {
        val lastEventTimestamp = lifecycleSharedPreferences.getLastEvent(eventKey, fallback)
        if (lastEventTimestamp != LifecycleDefaults.TIMESTAMP_INVALID) {
            reusableDate.time = lastEventTimestamp
            return formatIso8601.format(reusableDate)
        }

        return null
    }

    fun getDayOfWeekLocal(currentTime: Long): Int {
        if (calendar.timeInMillis != currentTime) {
            calendar.timeInMillis = currentTime
        }

        return calendar.get(Calendar.DAY_OF_WEEK)
    }

    fun getHourOfDayLocal(currentTime: Long): Int {
        if (calendar.timeInMillis != currentTime) {
            calendar.timeInMillis = currentTime
        }

        return calendar.get(Calendar.HOUR_OF_DAY)
    }

    fun isFirstWake(timestampA: Long, timestampB: Long): Int {
        calendar.timeInMillis = timestampA

        val monthA = calendar.get(Calendar.MONTH)
        val yearA = calendar.get(Calendar.YEAR)
        val dayA = calendar.get(Calendar.DAY_OF_MONTH)

        calendar.timeInMillis = timestampB

        val monthB = calendar.get(Calendar.MONTH)
        val yearB = calendar.get(Calendar.YEAR)
        val dayB = calendar.get(Calendar.DAY_OF_MONTH)

        var result = 0
        val isFirsWakeMonth = yearA != yearB || monthA != monthB
        if (isFirsWakeMonth) {
            result = IS_FIRST_WAKE_MONTH
        }

        if (isFirsWakeMonth || dayA != dayB) {
            result = result or IS_FIRST_WAKE_TODAY
        }

        return result
    }

    fun isFirstWakeMonth(firstWake: Int): Boolean {
        return firstWake and IS_FIRST_WAKE_MONTH == IS_FIRST_WAKE_MONTH
    }

    fun isFirstWakeToday(firstWake: Int): Boolean {
        return firstWake and IS_FIRST_WAKE_TODAY == IS_FIRST_WAKE_TODAY
    }

    companion object {

        private const val IS_FIRST_WAKE_MONTH = 1
        private const val IS_FIRST_WAKE_TODAY = 1 shl 1

        /**
         * If [timestamp] is valid, i.e. that it is not equal to [LifecycleDefaults.TIMESTAMP_INVALID]
         * then it returns the [valid] value, else the [default] provided.
         *
         * @param timestamp The timestamp to check the validity of
         * @param default The result to return if [timestamp] is invalid
         * @param valid The result to return if valid; defaults to the provided [timestamp]
         */
        internal fun validOrDefault(timestamp: Long, default: Long, valid: Long = timestamp): Long {
            return if (timestamp != LifecycleDefaults.TIMESTAMP_INVALID)
                valid
            else
                default
        }

        /**
         * Calculates the number of days, as a whole number of days, between two events recorded in
         * milliseconds.
         *
         * [startEventMs] and [endEventMs] should be positive numbers, but all results will be at
         * least 0.
         */
        internal fun daysSince(startEventMs: Long, endEventMs: Long): Long {
            if (startEventMs == LifecycleDefaults.TIMESTAMP_INVALID ||
                endEventMs == LifecycleDefaults.TIMESTAMP_INVALID
            ) {
                return 0L
            }

            val positiveStartMs = startEventMs.coerceAtLeast(0L)
            val positiveEndMs = endEventMs.coerceAtLeast(0L)
            val deltaMs = if (positiveEndMs <= positiveStartMs) {
                0L
            } else {
                positiveEndMs - positiveStartMs
            }
            return (deltaMs / LifecycleDefaults.DAY_IN_MS)
        }
    }
}