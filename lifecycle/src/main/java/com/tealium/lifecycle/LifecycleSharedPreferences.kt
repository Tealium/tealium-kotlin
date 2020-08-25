package com.tealium.lifecycle

import android.content.SharedPreferences
import com.tealium.core.TealiumConfig
import java.text.SimpleDateFormat
import java.util.*

internal class LifecycleSharedPreferences(config: TealiumConfig) {
    var lifecycleSharedPreferences: SharedPreferences = config.application.getSharedPreferences(sharedPreferencesName(config), 0)
    var formatIso8601 = SimpleDateFormat(LifecycleDefaults.FORMAT_ISO_8601, Locale.ROOT)
    var reusableDate: Date = Date(LifecycleDefaults.TIMESTAMP_INVALID)

    var updateLaunchDate: String? = null
    var firstLaunch: String? = null

    var firstLaunchMmDdYyyy: String? = null
    var lastLaunch: String? = null
    var lastSleep: String? = null
    var lastWake: String? = null

    var currentAppVersion: String?
        get() { return lifecycleSharedPreferences.getString("app_version", null) }
        set(value) { lifecycleSharedPreferences.edit()
                .putString("app_version", value)
                .apply()
        }

    var priorSecondsAwake: String
        get() { return (lifecycleSharedPreferences.getInt(LifecycleSPKey.PRIOR_SECONDS_AWAKE, 0)).toString() }
        set(value) { lifecycleSharedPreferences.edit().putString(LifecycleSPKey.PRIOR_SECONDS_AWAKE, value).apply() }

    var timestampUpdate: Long
        get() { return lifecycleSharedPreferences.getLong(LifecycleSPKey.TIMESTAMP_UPDATE, LifecycleDefaults.TIMESTAMP_INVALID) }
        set(value) { lifecycleSharedPreferences.edit().putLong(LifecycleSPKey.TIMESTAMP_UPDATE, value).apply() }

    var timestampFirstLaunch: Long
        get() { return lifecycleSharedPreferences.getLong(LifecycleSPKey.TIMESTAMP_FIRST_LAUNCH, LifecycleDefaults.TIMESTAMP_INVALID) }
        set(value) {
            lifecycleSharedPreferences.edit()
                    .putLong(LifecycleSPKey.TIMESTAMP_FIRST_LAUNCH, value)
                    .putLong(LifecycleSPKey.TIMESTAMP_LAST_LAUNCH, value)
                    .putLong(LifecycleSPKey.TIMESTAMP_LAST_WAKE, value)
                    .apply()
        }

    var timestampLaunch: Long
        get() { return lifecycleSharedPreferences.getLong(LifecycleSPKey.TIMESTAMP_LAUNCH, LifecycleDefaults.TIMESTAMP_INVALID) }
        set(value) { lifecycleSharedPreferences.edit().putLong(LifecycleSPKey.TIMESTAMP_LAUNCH, value).apply()}

    var timestampLastLaunch: Long
        get() { return lifecycleSharedPreferences.getLong(LifecycleSPKey.TIMESTAMP_LAST_LAUNCH, LifecycleDefaults.TIMESTAMP_INVALID) }
        set(value) { lifecycleSharedPreferences.edit().putLong(LifecycleSPKey.TIMESTAMP_LAST_LAUNCH, value).apply() }

    var timestampLastSleep: Long
        get() { return lifecycleSharedPreferences.getLong(LifecycleSPKey.TIMESTAMP_LAST_SLEEP, LifecycleDefaults.TIMESTAMP_INVALID) }
        set(value) { lifecycleSharedPreferences.edit().putLong(LifecycleSPKey.TIMESTAMP_LAST_SLEEP, value).apply() }

    var timestampLastWake: Long
        get() { return lifecycleSharedPreferences.getLong(LifecycleSPKey.TIMESTAMP_LAST_WAKE, LifecycleDefaults.TIMESTAMP_INVALID) }
        set(value) { lifecycleSharedPreferences.edit().putLong(LifecycleSPKey.TIMESTAMP_LAST_WAKE, value).apply() }

    var totalSecondsAwake: Int
        get() { return lifecycleSharedPreferences.getInt(LifecycleSPKey.TOTAL_SECONDS_AWAKE, 0) }
        set(value) { lifecycleSharedPreferences.edit().putInt(LifecycleSPKey.TOTAL_SECONDS_AWAKE, value).apply() }

    var secondsAwakeSinceLaunch: Int
        get() { return lifecycleSharedPreferences.getInt(LifecycleSPKey.PRIOR_SECONDS_AWAKE, 0) }
        set(value) { lifecycleSharedPreferences.edit().putInt(LifecycleSPKey.PRIOR_SECONDS_AWAKE, value).apply() }

    var countLaunch: Int
        get() { return lifecycleSharedPreferences.getInt(LifecycleSPKey.COUNT_LAUNCH, 0) }
        set(value) { lifecycleSharedPreferences.edit().putInt(LifecycleSPKey.COUNT_LAUNCH, value).apply() }

    var countSleep: Int
        get() { return lifecycleSharedPreferences.getInt(LifecycleSPKey.COUNT_SLEEP, 0) }
        set(value) { lifecycleSharedPreferences.edit().putInt(LifecycleSPKey.COUNT_SLEEP, value).apply() }

    var countWake: Int
        get() { return lifecycleSharedPreferences.getInt(LifecycleSPKey.COUNT_WAKE, 0) }
        set(value) { lifecycleSharedPreferences.edit().putInt(LifecycleSPKey.COUNT_WAKE, value).apply() }

    var countTotalLaunch: Int
        get() { return lifecycleSharedPreferences.getInt(LifecycleSPKey.COUNT_TOTAL_LAUNCH, 0) }
        set(value) { lifecycleSharedPreferences.edit().putInt(LifecycleSPKey.COUNT_TOTAL_LAUNCH, value).apply() }

    var countTotalSleep: Int
        get() { return lifecycleSharedPreferences.getInt(LifecycleSPKey.COUNT_TOTAL_SLEEP, 0) }
        set(value) { lifecycleSharedPreferences.edit().putInt(LifecycleSPKey.COUNT_TOTAL_SLEEP, value).apply() }

    var countTotalWake: Int
        get() { return lifecycleSharedPreferences.getInt(LifecycleSPKey.COUNT_TOTAL_WAKE, 0) }
        set(value) { lifecycleSharedPreferences.edit().putInt(LifecycleSPKey.COUNT_TOTAL_WAKE, value).apply() }

    var countTotalCrash: Int
        get() { return lifecycleSharedPreferences.getInt(LifecycleSPKey.COUNT_TOTAL_CRASH, 0) }
        set(value) { lifecycleSharedPreferences.edit().putInt(LifecycleSPKey.COUNT_TOTAL_CRASH, value).apply() }


    var lastLifecycleEvent: String?
        get() { return lifecycleSharedPreferences.getString(LifecycleSPKey.LAST_EVENT, null) }
        set(value) { lifecycleSharedPreferences.edit().putString(LifecycleSPKey.LAST_EVENT, value).apply() }

    fun incrementLaunch() {
        ++countLaunch
        ++countTotalLaunch
    }

    fun incrementWake() {
        ++countWake
        ++countTotalWake
    }

    fun incrementSleep() {
        ++countSleep
        ++countTotalSleep
    }

    fun incrementCrash() {
        ++countTotalCrash
    }

    fun updateSecondsAwake(seconds: Int) {
        totalSecondsAwake += seconds
        secondsAwakeSinceLaunch += seconds
    }

    fun setFirstLaunch(): String? {
        reusableDate.time = timestampFirstLaunch
        firstLaunch = formatIso8601.format(reusableDate)
        return firstLaunch
    }

    fun setFirstLaunchMmDdYyyy(): String? {
        val formatMmDdYyyy = SimpleDateFormat("MM/dd/yyy", Locale.ROOT)
        formatMmDdYyyy.timeZone = TimeZone.getTimeZone("UTC")
        reusableDate.time = timestampFirstLaunch
        firstLaunchMmDdYyyy = formatMmDdYyyy.format(reusableDate)

        return firstLaunchMmDdYyyy
    }

    fun setLastLaunch(timestamp: Long) {
        reusableDate.time = timestamp
        lastLaunch = formatIso8601.format(reusableDate)
        this.timestampLastLaunch = timestamp
    }

    fun setLastSleep(timestamp: Long) {
        reusableDate.time = timestamp
        lastSleep = formatIso8601.format(reusableDate)
        timestampLastSleep = timestamp
    }

    fun setLastWake(timestamp: Long) {
        reusableDate.time = timestamp
        lastWake = formatIso8601.format(reusableDate)
        timestampLastWake = timestamp
    }

    fun getLastEvent(eventName: String, fallback: Long): Long{
        return lifecycleSharedPreferences.getLong(eventName, fallback)
    }

    fun resetCountsAfterAppUpdate(timestampLaunch: Long, newAppVersion: String) {
        this.timestampUpdate = timestampLaunch
        this.currentAppVersion = newAppVersion
        lifecycleSharedPreferences.edit()
                .remove(LifecycleSPKey.COUNT_LAUNCH)
                .remove(LifecycleSPKey.COUNT_SLEEP)
                .remove(LifecycleSPKey.COUNT_WAKE)
                .apply()
    }

    private fun sharedPreferencesName(config: TealiumConfig): String {
        return "tealium.lifecycle.${Integer.toHexString((config.accountName + config.profileName + config.environment.environment).hashCode())}"
    }
}