package com.tealium.lifecycle

import android.app.Application
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.Environment
import com.tealium.core.Tealium
import com.tealium.core.TealiumConfig
import com.tealium.lifecycle.LifecycleSPKey.COUNT_LAUNCH
import com.tealium.lifecycle.LifecycleSPKey.COUNT_SLEEP
import com.tealium.lifecycle.LifecycleSPKey.COUNT_TOTAL_CRASH
import com.tealium.lifecycle.LifecycleSPKey.COUNT_TOTAL_LAUNCH
import com.tealium.lifecycle.LifecycleSPKey.COUNT_TOTAL_SLEEP
import com.tealium.lifecycle.LifecycleSPKey.COUNT_TOTAL_WAKE
import com.tealium.lifecycle.LifecycleSPKey.COUNT_WAKE
import com.tealium.lifecycle.LifecycleSPKey.LAST_EVENT
import com.tealium.lifecycle.LifecycleSPKey.PRIOR_SECONDS_AWAKE
import com.tealium.lifecycle.LifecycleSPKey.TIMESTAMP_FIRST_LAUNCH
import com.tealium.lifecycle.LifecycleSPKey.TIMESTAMP_LAST_LAUNCH
import com.tealium.lifecycle.LifecycleSPKey.TIMESTAMP_LAST_SLEEP
import com.tealium.lifecycle.LifecycleSPKey.TIMESTAMP_LAST_WAKE
import com.tealium.lifecycle.LifecycleSPKey.TIMESTAMP_UPDATE
import com.tealium.lifecycle.LifecycleSPKey.TOTAL_SECONDS_AWAKE
import io.mockk.MockKAnnotations
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MigrationTests {

    // file names from legacy java library
    val lifecyclePreferencesNamePrefix = "tealium.lifecycle"
    lateinit var legacyPreferences: SharedPreferences

    lateinit var application: Application
    lateinit var config: TealiumConfig
    lateinit var tealium: Tealium

    private lateinit var lifecycleSharedPreferences: LifecycleSharedPreferences

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        application = ApplicationProvider.getApplicationContext()
        config = TealiumConfig(application, "test", "test", Environment.DEV)
        legacyPreferences = application.getSharedPreferences("$lifecyclePreferencesNamePrefix.${getHashCodeString(config)}", 0)

        lifecycleSharedPreferences = LifecycleSharedPreferences(config)
    }

    @After
    fun tearDown() {
        legacyPreferences.edit().clear()
    }

    @Test
    fun constants_lifecycle_areSameAsLegacyJavaLibrary() {
        // lifecycle constants from legacy library
        assertEquals("timestamp_update", TIMESTAMP_UPDATE)
        assertEquals("timestamp_first_launch", TIMESTAMP_FIRST_LAUNCH)
        assertEquals("timestamp_last_launch", TIMESTAMP_LAST_LAUNCH)
        assertEquals("timestamp_last_wake", TIMESTAMP_LAST_WAKE)
        assertEquals("timestamp_last_sleep", TIMESTAMP_LAST_SLEEP)
        assertEquals("count_launch", COUNT_LAUNCH)
        assertEquals("count_sleep", COUNT_SLEEP)
        assertEquals("count_wake", COUNT_WAKE)
        assertEquals("count_total_crash", COUNT_TOTAL_CRASH)
        assertEquals("count_total_launch", COUNT_TOTAL_LAUNCH)
        assertEquals("count_total_sleep", COUNT_TOTAL_SLEEP)
        assertEquals("count_total_wake", COUNT_TOTAL_WAKE)
        assertEquals("last_event", LAST_EVENT)
        assertEquals("total_seconds_awake", TOTAL_SECONDS_AWAKE)
        assertEquals("prior_seconds_awake", PRIOR_SECONDS_AWAKE)
    }

    @Test
    fun lifecycle_status_timestampsAreMigrated() {
        val now = System.currentTimeMillis()
        with(legacyPreferences.edit()) {
            putLong("timestamp_update", now)
            putLong("timestamp_first_launch", now)
            putLong("timestamp_last_launch", now)
            putLong("timestamp_last_wake", now)
            putLong("timestamp_last_sleep", now)
            putLong("last_event", now)
        }.commit()

        assertEquals(now, lifecycleSharedPreferences.timestampUpdate)
        assertEquals(now, lifecycleSharedPreferences.timestampFirstLaunch)
        assertEquals(now, lifecycleSharedPreferences.timestampLastLaunch)
        assertEquals(now, lifecycleSharedPreferences.timestampLastWake)
        assertEquals(now, lifecycleSharedPreferences.timestampLastSleep)

        assertEquals(now, lifecycleSharedPreferences.getLastEvent("timestamp_last_launch", -1))
        assertEquals(now, lifecycleSharedPreferences.getLastEvent("timestamp_last_sleep", -1))
        assertEquals(now, lifecycleSharedPreferences.getLastEvent("timestamp_last_wake", -1))
    }


    @Test
    fun lifecycle_status_eventCountsAreMigrated() {
        val count = 15
        with(legacyPreferences.edit()) {
            putInt("count_launch", count)
            putInt("count_sleep", count)
            putInt("count_wake", count)
            putInt("count_total_crash", count)
            putInt("count_total_launch", count)
            putInt("count_total_sleep", count)
            putInt("count_total_wake", count)
            putInt("total_seconds_awake", count)
            putInt("prior_seconds_awake", count)
        }.commit()

        assertEquals(count, lifecycleSharedPreferences.countLaunch)
        assertEquals(count, lifecycleSharedPreferences.countSleep)
        assertEquals(count, lifecycleSharedPreferences.countWake)
        assertEquals(count, lifecycleSharedPreferences.countTotalCrash)
        assertEquals(count, lifecycleSharedPreferences.countTotalLaunch)
        assertEquals(count, lifecycleSharedPreferences.countTotalWake)
        assertEquals(count, lifecycleSharedPreferences.countTotalSleep)
        assertEquals(count, lifecycleSharedPreferences.totalSecondsAwake)
        assertEquals(count.toString(), lifecycleSharedPreferences.priorSecondsAwake)
    }

    fun getHashCodeString(config: TealiumConfig, delimiter: String = ""): String {
        return Integer.toHexString(
                (config.accountName + delimiter +
                        config.profileName + delimiter +
                        config.environment.environment).hashCode())
    }
}