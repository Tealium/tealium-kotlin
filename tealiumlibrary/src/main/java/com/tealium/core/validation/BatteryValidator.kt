package com.tealium.core.validation

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.tealium.core.Logger
import com.tealium.core.TealiumConfig
import com.tealium.core.messaging.LibrarySettingsUpdatedListener
import com.tealium.core.messaging.Subscribable
import com.tealium.core.settings.LibrarySettings
import com.tealium.dispatcher.Dispatch
import com.tealium.tealiumlibrary.BuildConfig


class BatteryValidator(config: TealiumConfig,
                       librarySettings: LibrarySettings,
                       events: Subscribable) : DispatchValidator {

    override val name: String = "BATTERY_VALIDATOR"
    override var enabled: Boolean = librarySettings.batterySaver

    private val context: Context = config.application
    private val lowBatteryThreshold: Int = config.lowBatteryThresholdPercentage ?: 15
    private val batteryIntentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

    /**
     * Returns the current Battery level in percentage points. For performance, this value is
     * updated for each new event only, so the value will be from the previous event.
     */
    val batteryLevel: Int
        get() {
            return context.registerReceiver(null, batteryIntentFilter)?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                (level * 100 / scale).coerceIn(0, 100)
            } ?: -1
        }

    /**
     * Returns true when the battery is low, otherwise false.
     */
    val isLowBattery: Boolean
        get() = batteryLevel < lowBatteryThreshold

    /**
     * Listener for receiving updates to the current LibrarySettings
     */
    private val listener = object : LibrarySettingsUpdatedListener {
        override fun onLibrarySettingsUpdated(settings: LibrarySettings) {
            enabled = settings.batterySaver
        }
    }

    init {
        events.subscribe(listener)
    }

    /**
     * Returns true when the [LibrarySettings.batterySaver] is true and the current [batteryLevel]
     * is below the preset [lowBatteryThresholdPercentage], otherwise false.
     */
    override fun shouldQueue(dispatch: Dispatch?): Boolean {
        return (enabled && isLowBattery).also { queueing ->
            if (queueing) Logger.qa(BuildConfig.TAG, "Battery is low ($batteryLevel%)")
        }
    }

    override fun shouldDrop(dispatch: Dispatch): Boolean {
        return false
    }
}

const val LOW_BATTERY_THRESHOLD_PERCENTAGE = "low_battery_threshold_percentage"

/**
 * Sets the low battery threshold in percentage points. When the battery level drops below
 * this threshold, events will begin being queued until the battery level is back above this value.
 * default: 15%
 */
var TealiumConfig.lowBatteryThresholdPercentage: Int?
    get() = options[LOW_BATTERY_THRESHOLD_PERCENTAGE] as? Int
    set(value) {
        value?.let {
            options[LOW_BATTERY_THRESHOLD_PERCENTAGE] = it.coerceIn(0, 100)
        }
    }