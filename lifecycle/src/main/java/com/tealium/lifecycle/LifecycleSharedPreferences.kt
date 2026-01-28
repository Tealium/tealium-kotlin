package com.tealium.lifecycle

import android.content.SharedPreferences
import com.tealium.core.TealiumConfig
import java.text.SimpleDateFormat
import java.util.*

internal class LifecycleSharedPreferences(
    config: TealiumConfig,
    private val lifecycleSharedPreferences: SharedPreferences = config.application.getSharedPreferences(sharedPreferencesName(config), 0)
) {

    var currentAppVersion: String?
        get() { return lifecycleSharedPreferences.getString("app_version", null) }
        set(value) { lifecycleSharedPreferences.edit()
                .putString("app_version", value)
                .apply()
        }

    var priorSecondsAwake: Long
        get() { return (lifecycleSharedPreferences.getLong(LifecycleSPKey.PRIOR_SECONDS_AWAKE, 0)) }
        set(value) { lifecycleSharedPreferences.edit().putLong(LifecycleSPKey.PRIOR_SECONDS_AWAKE, value).apply() }

    var timestampUpdate: Long?
        get() { return lifecycleSharedPreferences.getNullableLong(LifecycleSPKey.TIMESTAMP_UPDATE) }
        set(value) {
            if (value == null) {
                lifecycleSharedPreferences.edit().remove(LifecycleSPKey.TIMESTAMP_UPDATE).apply()
            }
            else {
                lifecycleSharedPreferences.edit()
                    .putLong(LifecycleSPKey.TIMESTAMP_UPDATE, value)
                    .apply()
            }
        }

    var timestampFirstLaunch: Long?
        get() { return lifecycleSharedPreferences.getNullableLong(LifecycleSPKey.TIMESTAMP_FIRST_LAUNCH) }
        set(value) {
            if (value == null) {
                lifecycleSharedPreferences.edit().remove(LifecycleSPKey.TIMESTAMP_FIRST_LAUNCH)
                    .apply()
            }
            else {
                lifecycleSharedPreferences.edit()
                    .putLong(LifecycleSPKey.TIMESTAMP_FIRST_LAUNCH, value)
                    .apply()
            }
        }

    var timestampLastLaunch: Long?
        get() { return lifecycleSharedPreferences.getNullableLong(LifecycleSPKey.TIMESTAMP_LAST_LAUNCH) }
        set(value) {
            if (value == null) {
                lifecycleSharedPreferences.edit().remove(LifecycleSPKey.TIMESTAMP_LAST_LAUNCH).apply()
            } else {
                lifecycleSharedPreferences.edit().putLong(LifecycleSPKey.TIMESTAMP_LAST_LAUNCH, value).apply()
            }
        }

    var timestampLastSleep: Long?
        get() { return lifecycleSharedPreferences.getNullableLong(LifecycleSPKey.TIMESTAMP_LAST_SLEEP) }
        set(value) {
            if (value == null) {
                lifecycleSharedPreferences.edit().remove(LifecycleSPKey.TIMESTAMP_LAST_SLEEP).apply()
            } else {
                lifecycleSharedPreferences.edit().putLong(LifecycleSPKey.TIMESTAMP_LAST_SLEEP, value).apply()
            }
        }

    var timestampLastWake: Long?
        get() { return lifecycleSharedPreferences.getNullableLong(LifecycleSPKey.TIMESTAMP_LAST_WAKE) }
        set(value) {
            if (value == null) {
                lifecycleSharedPreferences.edit().remove(LifecycleSPKey.TIMESTAMP_LAST_WAKE).apply()
            } else {
                lifecycleSharedPreferences.edit().putLong(LifecycleSPKey.TIMESTAMP_LAST_WAKE, value).apply()
            }
        }

    var totalSecondsAwake: Int
        get() { return lifecycleSharedPreferences.getInt(LifecycleSPKey.TOTAL_SECONDS_AWAKE, 0) }
        set(value) { lifecycleSharedPreferences.edit().putInt(LifecycleSPKey.TOTAL_SECONDS_AWAKE, value).apply() }

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

    fun registerLaunch(timestamp: Long) {
        timestampLastLaunch = timestamp
        timestampLastWake = timestamp

        incrementLaunch()
        incrementWake()
    }

    fun registerSleep(timestamp: Long, secondsAwake: Int) {
        timestampLastSleep = timestamp
        lastLifecycleEvent = LifecycleEvent.SLEEP
        incrementSleep()
        updateSecondsAwake(secondsAwake)
    }

    fun registerWake(timestamp: Long) {
        timestampLastWake = timestamp
        incrementWake()
    }

    fun registerLastLifecycleEvent(eventName: String) {
        lastLifecycleEvent = eventName
    }

    fun updateSecondsAwake(seconds: Int) {
        totalSecondsAwake += seconds
        priorSecondsAwake += seconds
    }

    fun setFirstLaunchTimestamp(timestamp: Long) {
        timestampFirstLaunch = timestamp
    }

    fun getLastEvent(eventName: String): Long? {
        return lifecycleSharedPreferences.getNullableLong(eventName)
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

    fun resetPriorSecondsAwake() {
        lifecycleSharedPreferences.edit().remove(LifecycleSPKey.PRIOR_SECONDS_AWAKE).apply()
    }

    companion object {
        private fun sharedPreferencesName(config: TealiumConfig): String {
            return "tealium.lifecycle.${Integer.toHexString((config.accountName + config.profileName + config.environment.environment).hashCode())}"
        }
    }
}