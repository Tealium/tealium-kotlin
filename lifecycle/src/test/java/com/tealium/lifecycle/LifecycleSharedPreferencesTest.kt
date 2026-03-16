package com.tealium.lifecycle

import android.app.Application
import android.content.SharedPreferences
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.*

class LifecycleSharedPreferencesTest {

    @MockK
    private lateinit var mockApplication: Application

    @MockK(relaxed = true)
    private lateinit var mockSharedPreferences: SharedPreferences

    @MockK(relaxed = true)
    private lateinit var mockEditor: SharedPreferences.Editor

    @MockK
    private lateinit var mockConfig: TealiumConfig

    private lateinit var lifecycleSharedPreferences: LifecycleSharedPreferences

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        TimeZone.getTimeZone("UTC").also { TimeZone.setDefault(it) }

        every { mockConfig.application } returns mockApplication
        every { mockConfig.accountName } returns "account"
        every { mockConfig.profileName } returns "profile"
        every { mockConfig.environment } returns Environment.DEV
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor
        every { mockEditor.putFloat(any(), any()) } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.putLong(any(), any()) } returns mockEditor
        every { mockEditor.putStringSet(any(), any()) } returns mockEditor
        every { mockEditor.remove(any()) } returns mockEditor

        lifecycleSharedPreferences = LifecycleSharedPreferences(mockConfig, mockSharedPreferences)
    }

    @Test
    fun setters_PriorSecondsAwake_SavesValue() {
        lifecycleSharedPreferences.priorSecondsAwake = 100L
        verify {
            mockEditor.putLong(LifecycleSPKey.PRIOR_SECONDS_AWAKE, 100L)
            mockEditor.apply()
        }
    }

    @Test
    fun setters_TimestampUpdate_SavesValue() {
        lifecycleSharedPreferences.timestampUpdate = 100L
        verify {
            mockEditor.putLong(LifecycleSPKey.TIMESTAMP_UPDATE, 100L)
            mockEditor.apply()
        }
    }

    @Test
    fun setters_TimestampFirstLaunch_SavesValue() {
        lifecycleSharedPreferences.timestampFirstLaunch = 100L
        verify {
            mockEditor.putLong(LifecycleSPKey.TIMESTAMP_FIRST_LAUNCH, 100L)
            mockEditor.apply()
        }
    }

    @Test
    fun setters_TimestampLastLaunch_SavesValue() {
        lifecycleSharedPreferences.timestampLastLaunch = 100L
        verify {
            mockEditor.putLong(LifecycleSPKey.TIMESTAMP_LAST_LAUNCH, 100L)
            mockEditor.apply()
        }
    }

    @Test
    fun setters_TimestampLastSleep_SavesValue() {
        lifecycleSharedPreferences.timestampLastSleep = 100L
        verify {
            mockEditor.putLong(LifecycleSPKey.TIMESTAMP_LAST_SLEEP, 100L)
            mockEditor.apply()
        }
    }

    @Test
    fun setters_TimestampLastWake_SavesValue() {
        lifecycleSharedPreferences.timestampLastWake = 100L
        verify {
            mockEditor.putLong(LifecycleSPKey.TIMESTAMP_LAST_WAKE, 100L)
            mockEditor.apply()
        }
    }

    @Test
    fun setters_TotalSecondsAwake_SavesValue() {
        lifecycleSharedPreferences.totalSecondsAwake = 100
        verify {
            mockEditor.putInt(LifecycleSPKey.TOTAL_SECONDS_AWAKE, 100)
            mockEditor.apply()
        }
    }

    @Test
    fun setters_CountLaunch_SavesValue() {
        lifecycleSharedPreferences.countLaunch = 1
        verify {
            mockEditor.putInt(LifecycleSPKey.COUNT_LAUNCH, 1)
            mockEditor.apply()
        }
    }

    @Test
    fun setters_CountSleep_SavesValue() {
        lifecycleSharedPreferences.countSleep = 1
        verify {
            mockEditor.putInt(LifecycleSPKey.COUNT_SLEEP, 1)
            mockEditor.apply()
        }
    }

    @Test
    fun setters_CountWake_SavesValue() {
        lifecycleSharedPreferences.countWake = 1
        verify {
            mockEditor.putInt(LifecycleSPKey.COUNT_WAKE, 1)
            mockEditor.apply()
        }
    }

    @Test
    fun setters_CountTotalLaunch_SavesValue() {
        lifecycleSharedPreferences.countTotalLaunch = 1
        verify {
            mockEditor.putInt(LifecycleSPKey.COUNT_TOTAL_LAUNCH, 1)
            mockEditor.apply()
        }
    }

    @Test
    fun setters_CountTotalSleep_SavesValue() {
        lifecycleSharedPreferences.countTotalSleep = 1
        verify {
            mockEditor.putInt(LifecycleSPKey.COUNT_TOTAL_SLEEP, 1)
            mockEditor.apply()
        }
    }

    @Test
    fun setters_CountTotalWake_SavesValue() {
        lifecycleSharedPreferences.countTotalWake = 1
        verify {
            mockEditor.putInt(LifecycleSPKey.COUNT_TOTAL_WAKE, 1)
            mockEditor.apply()
        }
    }

    @Test
    fun setters_CountTotalCrash_SavesValue() {
        lifecycleSharedPreferences.countTotalCrash = 1
        verify {
            mockEditor.putInt(LifecycleSPKey.COUNT_TOTAL_CRASH, 1)
            mockEditor.apply()
        }
    }

    @Test
    fun setters_LastLifecycleEvent_SavesValue() {
        lifecycleSharedPreferences.lastLifecycleEvent = "event"
        verify {
            mockEditor.putString(LifecycleSPKey.LAST_EVENT, "event")
            mockEditor.apply()
        }
    }

    @Test
    fun setters_IncrementLaunch_SavesValue() {
        val spykLifecycleSharedPreferences = spyk(lifecycleSharedPreferences)
        every { spykLifecycleSharedPreferences.countLaunch } returns 1
        every { spykLifecycleSharedPreferences.countTotalLaunch } returns 1
        spykLifecycleSharedPreferences.incrementLaunch()
        verify {
            spykLifecycleSharedPreferences.countLaunch = 2
            spykLifecycleSharedPreferences.countTotalLaunch = 2
        }
    }

    @Test
    fun setters_IncrementWake_SavesValue() {
        val spykLifecycleSharedPreferences = spyk(lifecycleSharedPreferences)
        every { spykLifecycleSharedPreferences.countWake } returns 1
        every { spykLifecycleSharedPreferences.countTotalWake } returns 1
        spykLifecycleSharedPreferences.incrementWake()
        verify {
            spykLifecycleSharedPreferences.countWake = 2
            spykLifecycleSharedPreferences.countTotalWake = 2
        }
    }

    @Test
    fun setters_IncrementSleep_SavesValue() {
        val spykLifecycleSharedPreferences = spyk(lifecycleSharedPreferences)
        every { spykLifecycleSharedPreferences.countSleep } returns 1
        every { spykLifecycleSharedPreferences.countTotalSleep } returns 1
        spykLifecycleSharedPreferences.incrementSleep()
        verify {
            spykLifecycleSharedPreferences.countSleep = 2
            spykLifecycleSharedPreferences.countTotalSleep = 2
        }
    }

    @Test
    fun setters_IncrementCrash_SavesValue() {
        val spykLifecycleSharedPreferences = spyk(lifecycleSharedPreferences)
        every { spykLifecycleSharedPreferences.countTotalCrash } returns 1
        spykLifecycleSharedPreferences.incrementCrash()
        verify {
            spykLifecycleSharedPreferences.countTotalCrash = 2
        }
    }

    @Test
    fun setters_UpdateSecondsAwake_SavesValue() {
        val spykLifecycleSharedPreferences = spyk(lifecycleSharedPreferences)
        every { spykLifecycleSharedPreferences.totalSecondsAwake } returns 50
        spykLifecycleSharedPreferences.updateSecondsAwake(100)
        verify {
            spykLifecycleSharedPreferences.totalSecondsAwake = 150
        }
    }

    @Test
    fun setters_ResetCounts_SavesValue() {
        val midnightMillennium = 946684800000L // 2000-01-01 00:00:00
        val spykLifecycleSharedPreferences = spyk(lifecycleSharedPreferences)
        spykLifecycleSharedPreferences.resetCountsAfterAppUpdate(midnightMillennium, "10")

        verify {
            spykLifecycleSharedPreferences.timestampUpdate = midnightMillennium
            spykLifecycleSharedPreferences.currentAppVersion = "10"
            mockEditor.putString("app_version", "10")
            mockEditor.remove(LifecycleSPKey.COUNT_LAUNCH)
            mockEditor.remove(LifecycleSPKey.COUNT_SLEEP)
            mockEditor.remove(LifecycleSPKey.COUNT_WAKE)
            mockEditor.apply()
        }
    }

    private val Date.absoluteYear: Int
        get() = this.year + 1900
}