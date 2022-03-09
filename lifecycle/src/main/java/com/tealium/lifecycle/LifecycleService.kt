package com.tealium.lifecycle

import java.text.SimpleDateFormat
import java.util.*

internal class LifecycleService(private val lifecycleSharedPreferences: LifecycleSharedPreferences) {

    private val IS_FIRST_WAKE_MONTH = 1
    private val IS_FIRST_WAKE_TODAY = 1 shl 1

    private val calendar: Calendar = Calendar.getInstance()
    var formatIso8601 = SimpleDateFormat(LifecycleDefaults.FORMAT_ISO_8601, Locale.ROOT)
    var reusableDate: Date = Date(LifecycleDefaults.TIMESTAMP_INVALID)

    init {
        formatIso8601.timeZone = TimeZone.getTimeZone("UTC")
    }

    fun getCurrentState(timestamp: Long): MutableMap<String, Any> {
        val data = mutableMapOf<String, Any>(
            LifecycleStateKey.LIFECYCLE_DAYOFWEEK_LOCAL to getDayOfWeekLocal(timestamp),
            LifecycleStateKey.LIFECYCLE_DAYSSINCELAUNCH to ((timestamp - lifecycleSharedPreferences.timestampFirstLaunch) / LifecycleDefaults.DAY_IN_MS).toString(),
            LifecycleStateKey.LIFECYCLE_DAYSSINCELASTWAKE to (if (lifecycleSharedPreferences.timestampLastWake == LifecycleDefaults.TIMESTAMP_INVALID) "0" else ((timestamp - lifecycleSharedPreferences.timestampLastWake) / LifecycleDefaults.DAY_IN_MS).toString()),
            LifecycleStateKey.LIFECYCLE_HOUROFDAY_LOCAL to getHourOfDayLocal(timestamp).toString(),
            LifecycleStateKey.LIFECYCLE_LAUNCHCOUNT to lifecycleSharedPreferences.countLaunch,
            LifecycleStateKey.LIFECYCLE_SLEEPCOUNT to lifecycleSharedPreferences.countSleep,
            LifecycleStateKey.LIFECYCLE_WAKECOUNT to lifecycleSharedPreferences.countWake,
            LifecycleStateKey.LIFECYCLE_TOTALCRASHCOUNT to lifecycleSharedPreferences.countTotalCrash,
            LifecycleStateKey.LIFECYCLE_TOTALLAUNCHCOUNT to lifecycleSharedPreferences.countTotalLaunch,
            LifecycleStateKey.LIFECYCLE_TOTALSLEEPCOUNT to lifecycleSharedPreferences.countSleep.toString(),
            LifecycleStateKey.LIFECYCLE_TOTALWAKECOUNT to lifecycleSharedPreferences.countWake.toString(),
            LifecycleStateKey.LIFECYCLE_TOTALSECONDSAWAKE to lifecycleSharedPreferences.totalSecondsAwake.toString(),
            LifecycleStateKey.LIFECYCLE_DAYSSINCEUPDATE to ((timestamp - lifecycleSharedPreferences.timestampUpdate) / LifecycleDefaults.DAY_IN_MS).toString()
        )

        lifecycleSharedPreferences.firstLaunch?.let {
            data[LifecycleStateKey.LIFECYCLE_FIRSTLAUNCHDATE] = it
        } ?: run {
            val launch = lifecycleSharedPreferences.setFirstLaunch()
            launch?.let {
                data[LifecycleStateKey.LIFECYCLE_FIRSTLAUNCHDATE] = it
            }
        }

        lifecycleSharedPreferences.firstLaunchMmDdYyyy?.let {
            data[LifecycleStateKey.LIFECYCLE_FIRSTLAUNCHDATE_MMDDYYYY] = it
        } ?: run {
            val launchDataMmDdYyyy = lifecycleSharedPreferences.setFirstLaunchMmDdYyyy()
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
                data[LifecycleStateKey.LIFECYCLE_DAYSSINCEUPDATE] =
                    ((timestamp - lifecycleSharedPreferences.timestampUpdate) / LifecycleDefaults.DAY_IN_MS)
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

        if (initializedCurrentVersion != cachedVersion) {
            lifecycleSharedPreferences.resetCountsAfterAppUpdate(
                timestamp,
                initializedCurrentVersion
            )
            return true
        }

        if (cachedVersion.isNullOrEmpty()) {
            lifecycleSharedPreferences.currentAppVersion = initializedCurrentVersion
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
}