package com.tealium.lifecycle

import android.app.Application
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import com.tealium.core.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File

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

        Assert.assertFalse(lifecycle.isAutoTracking)
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

        Assert.assertTrue(lifecycle.lifecycleSharedPreferences.countLaunch == 1)
        Assert.assertTrue(lifecycle.lifecycleSharedPreferences.countTotalLaunch == 1)
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

        Assert.assertTrue(lifecycle.lifecycleSharedPreferences.countSleep == 1)
        Assert.assertTrue(lifecycle.lifecycleSharedPreferences.countTotalSleep == 1)
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

        Assert.assertTrue(lifecycle.lifecycleSharedPreferences.countWake == 1)
        Assert.assertTrue(lifecycle.lifecycleSharedPreferences.countTotalWake == 1)
    }

    @Test
    fun getCurrentState() {
        config.options["is_lifecycle_autotracking"] = false
        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)

        lifecycle = Lifecycle(tealiumContext)
        lifecycle.trackLaunchEvent()

        val dataMap = lifecycle.lifecycleService.getCurrentState(System.currentTimeMillis())

        Assert.assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_DAYOFWEEK_LOCAL))
        Assert.assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_DAYSSINCELAUNCH))
        Assert.assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_DAYSSINCELASTWAKE))
        Assert.assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_HOUROFDAY_LOCAL))
        Assert.assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_LAUNCHCOUNT))
        Assert.assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_SLEEPCOUNT))
        Assert.assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_WAKECOUNT))
        Assert.assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_TOTALCRASHCOUNT))
        Assert.assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_TOTALLAUNCHCOUNT))
        Assert.assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_TOTALSLEEPCOUNT))
        Assert.assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_TOTALWAKECOUNT))
        Assert.assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_TOTALSECONDSAWAKE))
        Assert.assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_LASTWAKEDATE))
        Assert.assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_LASTSLEEPDATE))
        Assert.assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_DAYSSINCEUPDATE))

        // dates for lifecycle events
        Assert.assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_FIRSTLAUNCHDATE))
        Assert.assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_FIRSTLAUNCHDATE_MMDDYYYY))
        Assert.assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_LASTLAUNCHDATE))
        Assert.assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_LASTWAKEDATE))
        Assert.assertTrue(dataMap.contains(LifecycleStateKey.LIFECYCLE_LASTSLEEPDATE))
    }
}