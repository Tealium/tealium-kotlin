package com.tealium.lifecycle

import android.app.Application
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import com.tealium.core.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.internal.ResourcesMode
import org.robolectric.shadows.ShadowLooper
import java.io.File
import java.lang.UnsupportedOperationException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LifecycleTest {

    @MockK
    lateinit var context: Application

    @MockK
    lateinit var mockFile: File

    @MockK(relaxed = true)
    private lateinit var mockSharedPreferences: SharedPreferences

    @MockK(relaxed = true)
    private lateinit var mockLifecycleSharedPreferences: LifecycleSharedPreferences

    @MockK
    lateinit var editor: SharedPreferences.Editor

    lateinit var mockPackageInfo: PackageInfo

    lateinit var config: TealiumConfig
    lateinit var tealium: Tealium
    lateinit var tealiumContext: TealiumContext
    lateinit var lifecycle: Lifecycle

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        tealium = mockk<Tealium>()
        every { tealium.track(any()) } returns mockk()

        mockkConstructor(TealiumConfig::class)
        every { context.filesDir } returns mockFile
        every { anyConstructed<TealiumConfig>().tealiumDirectory.mkdir() } returns mockk()

        config = TealiumConfig(context, "test", "profile", Environment.QA)
        every { config.application.getSharedPreferences(any(), any()) } returns mockSharedPreferences

        mockPackageInfo = spyk(PackageInfo())
        every { config.application.packageName } returns "test"
        every { config.application.packageManager.getPackageInfo(any<String>(), any()) } returns mockPackageInfo


        mockkConstructor(SharedPreferences::class)
        every { anyConstructed<SharedPreferences>().getInt(LifecycleSPKey.COUNT_LAUNCH, 1) } returns 1
        every { anyConstructed<SharedPreferences>().getInt(LifecycleSPKey.COUNT_SLEEP, 1) } returns 1
        every { anyConstructed<SharedPreferences>().getInt(LifecycleSPKey.COUNT_WAKE, 1) } returns 1
        every { anyConstructed<SharedPreferences>().edit() } returns editor
        every { editor.apply() } just Runs
        every { editor.putInt(LifecycleSPKey.COUNT_LAUNCH, 1) } returns editor
        every { editor.putInt(LifecycleSPKey.COUNT_SLEEP, 1) } returns editor
        every { editor.putInt(LifecycleSPKey.COUNT_WAKE, 1) } returns editor
    }

    @Test
    fun isAutoTracking() {
        config.options["is_lifecycle_autotracking"] = false
        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)
        lifecycle = Lifecycle(tealiumContext)

        assertFalse(lifecycle.isAutoTracking)
    }

    @Test
    fun incrementLaunch() {
        config.options["is_lifecycle_autotracking"] = false
        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)

        lifecycle = Lifecycle(tealiumContext)
        lifecycle.lifecycleSharedPreferences = mockLifecycleSharedPreferences

        every { mockLifecycleSharedPreferences.incrementLaunch() } just runs
        every { mockLifecycleSharedPreferences.countLaunch } returns 1
        every { mockLifecycleSharedPreferences.countTotalLaunch } returns 1

        lifecycle.trackLaunchEvent()

        assertTrue(lifecycle.lifecycleSharedPreferences.countLaunch == 1)
        assertTrue(lifecycle.lifecycleSharedPreferences.countTotalLaunch == 1)
    }

    @Test
    fun incrementSleep() {
        config.options["is_lifecycle_autotracking"] = false
        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)

        lifecycle = Lifecycle(tealiumContext)
        lifecycle.lifecycleSharedPreferences = mockLifecycleSharedPreferences
        every { mockLifecycleSharedPreferences.incrementSleep() } just runs
        every { mockLifecycleSharedPreferences.countSleep } returns 1
        every { mockLifecycleSharedPreferences.countTotalSleep } returns 1

        lifecycle.trackSleepEvent()

        assertTrue(lifecycle.lifecycleSharedPreferences.countSleep == 1)
        assertTrue(lifecycle.lifecycleSharedPreferences.countTotalSleep == 1)
    }

    @Test
    fun incrementWake() {
        config.options["is_lifecycle_autotracking"] = false
        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)

        lifecycle = Lifecycle(tealiumContext)
        lifecycle.lifecycleSharedPreferences = mockLifecycleSharedPreferences
        every { mockLifecycleSharedPreferences.incrementWake() } just runs
        every { mockLifecycleSharedPreferences.countWake } returns 1
        every { mockLifecycleSharedPreferences.countTotalWake } returns 1

        lifecycle.trackWakeEvent()

        assertTrue(lifecycle.lifecycleSharedPreferences.countWake == 1)
        assertTrue(lifecycle.lifecycleSharedPreferences.countTotalWake == 1)
    }

    @Test
    fun getCurrentState() {
        config.options["is_lifecycle_autotracking"] = false
        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)

        lifecycle = Lifecycle(tealiumContext)
        lifecycle.trackLaunchEvent()

        val dataMap = lifecycle.lifecycleService.getCurrentState(System.currentTimeMillis())

        assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_DAYOFWEEK_LOCAL))
        assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_DAYSSINCELAUNCH))
        assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_DAYSSINCELASTWAKE))
        assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_HOUROFDAY_LOCAL))
        assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_LAUNCHCOUNT))
        assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_SLEEPCOUNT))
        assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_WAKECOUNT))
        assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_TOTALCRASHCOUNT))
        assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_TOTALLAUNCHCOUNT))
        assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_TOTALSLEEPCOUNT))
        assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_TOTALWAKECOUNT))
        assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_TOTALSECONDSAWAKE))
        assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_LASTWAKEDATE))
        assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_LASTSLEEPDATE))
        assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_DAYSSINCEUPDATE))

        // dates for lifecycle events
        assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_FIRSTLAUNCHDATE))
        assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_FIRSTLAUNCHDATE_MMDDYYYY))
        assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_LASTLAUNCHDATE))
        assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_LASTWAKEDATE))
        assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_LASTSLEEPDATE))
    }

    @Test
    fun modules_PointsToCompanionFactory() {
        assertSame(Lifecycle, Modules.Lifecycle)
        assertTrue(Lifecycle is ModuleFactory)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun tracking_LaunchEvent_WhenAutoTrackingEnabled_ShouldThrow() {
        config.options["is_lifecycle_autotracking"] = true
        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)

        lifecycle = Lifecycle(tealiumContext)
        lifecycle.trackLaunchEvent()
    }

    @Test(expected = UnsupportedOperationException::class)
    fun tracking_SleepEvent_WhenAutoTrackingEnabled_ShouldThrow() {
        config.options["is_lifecycle_autotracking"] = true
        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)

        lifecycle = Lifecycle(tealiumContext)
        lifecycle.trackSleepEvent()
    }

    @Test(expected = UnsupportedOperationException::class)
    fun tracking_WakeEvent_WhenAutoTrackingEnabled_ShouldThrow() {
        config.options["is_lifecycle_autotracking"] = true
        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)

        lifecycle = Lifecycle(tealiumContext)
        lifecycle.trackWakeEvent()
    }

    @Test
    fun state_LaunchData_ShouldBePopulated() {
        config.options["is_lifecycle_autotracking"] = false
        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)

        lifecycle = Lifecycle(tealiumContext)
        val mockService = mockk<LifecycleService>()
        every { mockService.getCurrentState(any()) } returns mutableMapOf()
        every { mockService.isFirstLaunch(any()) } returns true
        every { mockService.didUpdate(any(), any()) } returns true
        every { mockService.didDetectCrash(any()) } returns true
        every { mockService.isFirstWake(any(), any()) } returns 1
        every { mockService.isFirstWakeMonth(any()) } returns true
        every { mockService.isFirstWakeToday(any()) } returns true
        lifecycle.lifecycleService = mockService

        lifecycle.trackLaunchEvent()

        verify {
            tealium.track(match {
                it[LifecycleStateKey.LIFECYCLE_ISFIRSTLAUNCH] == "true"
                        && it[LifecycleStateKey.LIFECYCLE_ISFIRSTLAUNCHUPDATE] == "true"
                        && it[LifecycleStateKey.LIFECYCLE_DIDDETECTCRASH] == "true"
                        && it[LifecycleStateKey.LIFECYCLE_ISFIRSTWAKEMONTH] == "true"
                        && it[LifecycleStateKey.LIFECYCLE_ISFIRSTWAKETODAY] == "true"
            })
        }
    }

    @Test
    fun state_LaunchData_ShouldBeNull() {
        config.options["is_lifecycle_autotracking"] = false
        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)

        lifecycle = Lifecycle(tealiumContext)
        val mockService = mockk<LifecycleService>()
        every { mockService.getCurrentState(any()) } returns mutableMapOf()
        every { mockService.isFirstLaunch(any()) } returns false
        every { mockService.didUpdate(any(), any()) } returns false
        every { mockService.didDetectCrash(any()) } returns false
        every { mockService.isFirstWake(any(), any()) } returns 1
        every { mockService.isFirstWakeMonth(any()) } returns false
        every { mockService.isFirstWakeToday(any()) } returns false
        lifecycle.lifecycleService = mockService

        lifecycle.trackLaunchEvent()

        verify {
            tealium.track(match {
                it[LifecycleStateKey.LIFECYCLE_ISFIRSTLAUNCH] == null
                        && it[LifecycleStateKey.LIFECYCLE_ISFIRSTLAUNCHUPDATE] == null
                        && it[LifecycleStateKey.LIFECYCLE_DIDDETECTCRASH] == null
                        && it[LifecycleStateKey.LIFECYCLE_ISFIRSTWAKEMONTH] == null
                        && it[LifecycleStateKey.LIFECYCLE_ISFIRSTWAKETODAY] == null
            })
        }
    }

    @Test
    fun state_TrackLaunchEvent_AddsCustomData() {
        config.options["is_lifecycle_autotracking"] = false
        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)

        lifecycle = Lifecycle(tealiumContext)
        lifecycle.trackLaunchEvent(mapOf("custom" to "data"))

        verify {
            tealium.track(match {
                it["custom"] == "data"
            })
        }
    }

    @Test
    fun state_TrackSleepEvent_AddsCustomData() {
        config.options["is_lifecycle_autotracking"] = false
        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)

        lifecycle = Lifecycle(tealiumContext)
        lifecycle.trackSleepEvent(mapOf("custom" to "data"))

        verify {
            tealium.track(match {
                it["custom"] == "data"
            })
        }
    }

    @Test
    fun state_TrackWakeEvent_AddsCustomData() {
        config.options["is_lifecycle_autotracking"] = false
        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)

        lifecycle = Lifecycle(tealiumContext)
        lifecycle.trackWakeEvent(mapOf("custom" to "data"))

        verify {
            tealium.track(match {
                it["custom"] == "data"
            })
        }
    }

    @Test
    fun activityObserver_TracksNothingWhenAutoTrackingDisabled() {
        config.options["is_lifecycle_autotracking"] = false
        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)

        lifecycle = Lifecycle(tealiumContext)
        lifecycle.onActivityResumed(null)
        lifecycle.onActivityPaused(null)
        lifecycle.onActivityStopped(null, false)

        verify(exactly = 0) {
            tealium.track(any())
        }
    }

    @Test
    fun activityObserver_OnActivityResumed() {
        config.options["is_lifecycle_autotracking"] = true
        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)

        lifecycle = Lifecycle(tealiumContext)
        lifecycle.onActivityResumed(null)

        verify {
            tealium.track(match {
                it[LifecycleStateKey.AUTOTRACKED] == true
                        && it[LifecycleStateKey.LIFECYCLE_TYPE] == LifecycleEvent.LAUNCH
            })
        }
    }

    @Test
    fun activityObserver_OnActivityPaused() {
        config.options["is_lifecycle_autotracking"] = true
        tealiumContext = spyk(TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium))

        lifecycle = Lifecycle(tealiumContext)
        lifecycle.lifecycleSharedPreferences = mockLifecycleSharedPreferences
        lifecycle.onActivityPaused(null)

        verify {
            tealium.track(match {
                it[LifecycleStateKey.AUTOTRACKED] == true
                        && it[LifecycleStateKey.LIFECYCLE_TYPE] == LifecycleEvent.LAUNCH
            })
            mockLifecycleSharedPreferences.lastLifecycleEvent = LifecycleEvent.PAUSE
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        verify {
            tealium.track(match {
                it[LifecycleStateKey.AUTOTRACKED] == true &&
                it[LifecycleStateKey.LIFECYCLE_TYPE] == LifecycleEvent.SLEEP
            })
        }
    }
}