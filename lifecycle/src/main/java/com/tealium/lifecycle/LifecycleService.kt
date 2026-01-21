package com.tealium.lifecycle

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

internal class LifecycleService(private val lifecycleSharedPreferences: LifecycleSharedPreferences) {

    private val calendar: Calendar = Calendar.getInstance()
    internal var updateLaunchDate: String? = null
    internal var firstLaunchString: String? = null
    internal var firstLaunchMmDdYyyyString: String? = null
    internal var lastLaunchString: String? = null
    internal var lastSleepString: String? = null
    internal var lastWakeString: String? = null

    fun getCurrentState(timestamp: Long): MutableMap<String, Any> {
        val data = mutableMapOf<String, Any>().also { data ->
            data.putAll(updateDaysSince(timestamp))

            data[LifecycleStateKey.LIFECYCLE_DAYOFWEEK_LOCAL] = getDayOfWeekLocal(timestamp)
            data[LifecycleStateKey.LIFECYCLE_HOUROFDAY_LOCAL] = getHourOfDayLocal(timestamp).toString()
            data[LifecycleStateKey.LIFECYCLE_LAUNCHCOUNT] = lifecycleSharedPreferences.countLaunch
            data[LifecycleStateKey.LIFECYCLE_SLEEPCOUNT] = lifecycleSharedPreferences.countSleep
            data[LifecycleStateKey.LIFECYCLE_WAKECOUNT] = lifecycleSharedPreferences.countWake
            data[LifecycleStateKey.LIFECYCLE_TOTALCRASHCOUNT] = lifecycleSharedPreferences.countTotalCrash
            data[LifecycleStateKey.LIFECYCLE_TOTALLAUNCHCOUNT] = lifecycleSharedPreferences.countTotalLaunch
            data[LifecycleStateKey.LIFECYCLE_TOTALSLEEPCOUNT] = lifecycleSharedPreferences.countSleep.toString()
            data[LifecycleStateKey.LIFECYCLE_TOTALWAKECOUNT] = lifecycleSharedPreferences.countWake.toString()
            data[LifecycleStateKey.LIFECYCLE_TOTALSECONDSAWAKE] = lifecycleSharedPreferences.totalSecondsAwake.toString()
        }

        val firstLaunchString = this.firstLaunchString ?: setFormattedFirstLaunch(timestamp)
        firstLaunchString?.let {
            data[LifecycleStateKey.LIFECYCLE_FIRSTLAUNCHDATE] = it
        }

        val firstLaunchMmDdYyyyString = this.firstLaunchMmDdYyyyString ?: setFirstLaunchMmDdYyyy(timestamp)
        firstLaunchMmDdYyyyString?.let {
            data[LifecycleStateKey.LIFECYCLE_FIRSTLAUNCHDATE_MMDDYYYY] = it
        }

        val lastLaunch = lastLaunchString ?: setFormattedEvent(LifecycleSPKey.TIMESTAMP_LAST_LAUNCH)
        lastLaunch?.let {
            data[LifecycleStateKey.LIFECYCLE_LASTLAUNCHDATE] = it
        }

        val lastWake = lastWakeString ?: setFormattedEvent(LifecycleSPKey.TIMESTAMP_LAST_WAKE)
        lastWake?.let {
            data[LifecycleStateKey.LIFECYCLE_LASTWAKEDATE] = it
        }

        val lastSleep = lastSleepString ?: setFormattedEvent(LifecycleSPKey.TIMESTAMP_LAST_SLEEP)
        lastSleep?.let {
            data[LifecycleStateKey.LIFECYCLE_LASTSLEEPDATE] = it
        }

        if (lifecycleSharedPreferences.timestampUpdate != null) {
            val lastUpdate = updateLaunchDate ?: setUpdateLaunchDate()
            if (lastUpdate != null) {
                data[LifecycleStateKey.LIFECYCLE_UPDATELAUNCHDATE] = lastUpdate
            }
        }

        return data.toMutableMap()
    }

    private fun setUpdateLaunchDate(): String? {
        updateLaunchDate = lifecycleSharedPreferences.timestampUpdate?.let {
            formatTimestamp(it)
        }

        return updateLaunchDate
    }

    private fun setFormattedEvent(eventKey: String): String? {
        val timestamp = lifecycleSharedPreferences.getLastEvent(eventKey)
        val formattedTimestamp = timestamp?.let { formatTimestamp(it) }

        when(eventKey) {
            LifecycleSPKey.TIMESTAMP_LAST_LAUNCH -> lastLaunchString = formattedTimestamp
            LifecycleSPKey.TIMESTAMP_LAST_SLEEP -> lastSleepString = formattedTimestamp
            LifecycleSPKey.TIMESTAMP_LAST_WAKE -> lastWakeString = formattedTimestamp
        }

        return formattedTimestamp
    }

    private fun setFirstLaunchMmDdYyyy(fallbackTimestamp: Long = System.currentTimeMillis()): String? {
        val formatMmDdYyyy = SimpleDateFormat("MM/dd/yyyy", Locale.ROOT)
        formatMmDdYyyy.timeZone = TimeZone.getTimeZone("UTC")
        val date = Date(lifecycleSharedPreferences.timestampFirstLaunch ?: fallbackTimestamp)
        firstLaunchMmDdYyyyString = formatMmDdYyyy.format(date)

        return firstLaunchMmDdYyyyString
    }

    private fun setFormattedFirstLaunch(fallbackTimestamp: Long = System.currentTimeMillis()): String? {
        firstLaunchString = formatTimestamp(lifecycleSharedPreferences.timestampFirstLaunch ?: fallbackTimestamp)
        return firstLaunchString
    }

    fun didDetectCrash(event: String): Boolean {
        val lastEvent = lifecycleSharedPreferences.lastLifecycleEvent ?: return false

        val lastIsForegrounding =
            LifecycleEvent.LAUNCH == lastEvent || LifecycleEvent.WAKE == lastEvent
        val currentIsForegrounding =
            LifecycleEvent.LAUNCH == event || LifecycleEvent.WAKE == event

        val crashDetected = lastIsForegrounding && currentIsForegrounding

        return crashDetected
    }

    fun setFirstLaunch(timestamp: Long) {
        firstLaunchString = formatTimestamp(timestamp)
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

    private fun updateDaysSince(
        timestamp: Long,
    ): MutableMap<String, Any> {
        val data: MutableMap<String, Any> = mutableMapOf()
        daysSince(
            startEventMs = lifecycleSharedPreferences.timestampLastLaunch,
            endEventMs = timestamp
        )?.also {
            data[LifecycleStateKey.LIFECYCLE_DAYSSINCELAUNCH] = it.toString()
        }

        daysSince(
            startEventMs = lifecycleSharedPreferences.timestampLastWake,
            endEventMs = timestamp
        )?.also {
            data[LifecycleStateKey.LIFECYCLE_DAYSSINCELASTWAKE] = it.toString()
        }

        daysSince(
            startEventMs = lifecycleSharedPreferences.timestampUpdate,
            endEventMs = timestamp
        )?.also {
            data[LifecycleStateKey.LIFECYCLE_DAYSSINCEUPDATE] = it
        }

        return data
    }

    companion object {
        var formatIso8601 = SimpleDateFormat(LifecycleDefaults.FORMAT_ISO_8601, Locale.ROOT)

        init {
            formatIso8601.timeZone = TimeZone.getTimeZone("UTC")
        }

        /**
         * Calculates the number of days, as a whole number of days, between two events recorded in
         * milliseconds.
         *
         * [startEventMs] and [endEventMs] should be positive numbers, but all results will be at
         * least 0.
         */
        internal fun daysSince(startEventMs: Long?, endEventMs: Long): Long? {
            val daysInMs = TimeUnit.DAYS.toMillis(1)
            return if (startEventMs != null && startEventMs >= 0 && endEventMs >= startEventMs) {
                val deltaMs = endEventMs - startEventMs
                deltaMs / daysInMs
            } else {
                null
            }
        }

        fun formatTimestamp(timestamp: Long): String {
            return formatIso8601.format(Date(timestamp))
        }
    }
}