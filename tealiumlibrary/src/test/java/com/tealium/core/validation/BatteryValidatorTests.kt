package com.tealium.core.validation

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import android.os.BatteryManager
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.consent.ConsentManagerConstants
import com.tealium.core.messaging.EventDispatcher
import com.tealium.core.messaging.EventRouter
import com.tealium.core.settings.LibrarySettings
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
class BatteryValidatorTests {

    val eventRouter: EventRouter = spyk(EventDispatcher())

    @MockK
    lateinit var mockFile: File

    @MockK
    lateinit var mockSettings: LibrarySettings

    @MockK
    lateinit var mockContext: Application

    @RelaxedMockK
    lateinit var mockConfig: TealiumConfig

    lateinit var batteryValidator: BatteryValidator

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        mockkStatic("com.tealium.core.validation.BatteryValidatorKt")
        every { mockConfig.application } returns mockContext

        every { mockConfig.lowBatteryThresholdPercentage } returns null // use defaults
        every { mockSettings.batterySaver } returns true // enabled by default

        batteryValidator = BatteryValidator(mockConfig, mockSettings, eventRouter)
    }

    @Test
    fun isLowBattery_IsTrue() {
        every { mockContext.registerReceiver(null, any()) } returns getBatteryLevelIntent(5)
        assertTrue(batteryValidator.isLowBattery)
    }

    @Test
    fun isLowBattery_IsFalse() {
        every { mockContext.registerReceiver(null, any()) } returns getBatteryLevelIntent(20)
        assertFalse(batteryValidator.isLowBattery)
    }

    @Test
    fun shouldQueue_IsTrueWhenBatteryLevelIsLow_DefaultThreshold() {
        every { mockContext.registerReceiver(null, any()) } returns getBatteryLevelIntent(10)

        assertTrue(batteryValidator.shouldQueue(null))
    }

    @Test
    fun shouldQueue_IsFalseWhenBatteryLevelIsNotLow_DefaultThreshold() {
        every { mockContext.registerReceiver(null, any()) } returns getBatteryLevelIntent(20)

        assertFalse(batteryValidator.shouldQueue(null))
    }

    @Test
    fun shouldQueue_IsTrueWhenBatteryLevelIsLow_OverriddenThreshold() {
        every { mockConfig.lowBatteryThresholdPercentage } returns 50
        batteryValidator = spyk(BatteryValidator(mockConfig, mockSettings, eventRouter))

        every { mockContext.registerReceiver(null, any()) } returns getBatteryLevelIntent(49)

        assertTrue(batteryValidator.shouldQueue(null))
    }

    @Test
    fun shouldQueue_IsFalseWhenBatteryLevelIsNotLow_OverriddenThreshold() {
        every { mockConfig.lowBatteryThresholdPercentage } returns 50
        batteryValidator = spyk(BatteryValidator(mockConfig, mockSettings, eventRouter))

        every { mockContext.registerReceiver(null, any()) } returns getBatteryLevelIntent(75)

        assertFalse(batteryValidator.shouldQueue(null))
    }

    @Test
    fun shouldQueue_IsFalseWhenBatterySaverGetsDisabled() {
        eventRouter.onLibrarySettingsUpdated(LibrarySettings(batterySaver = false))

        every { mockContext.registerReceiver(null, any()) } returns getBatteryLevelIntent(75)

        assertFalse(batteryValidator.shouldQueue(null))
        assertFalse(batteryValidator.enabled)
    }

    private fun getBatteryLevelIntent(level: Int,
                                      scale: Int = 100): Intent? {
        return Intent()
                .putExtra(BatteryManager.EXTRA_LEVEL, level)
                .putExtra(BatteryManager.EXTRA_SCALE, scale)
    }
}