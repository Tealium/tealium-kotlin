package com.tealium.core.collection

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.Surface
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.tealium.core.*
import com.tealium.dispatcher.Dispatch
import com.tealium.tealiumlibrary.BuildConfig
import java.util.*
import kotlin.math.roundToInt

interface DeviceData {
    val device: String
    val deviceModel: String
    val deviceManufacturer: String
    val deviceArchitecture: String
    val deviceCpuType: String
    val deviceResolution: String
    val deviceLogicalResolution: String
    val deviceRuntime: String
    val deviceOrigin: String
    val devicePlatform: String
    val deviceOsName: String
    val deviceOsBuild: String
    val deviceOsVersion: String
    val deviceAvailableSystemStorage: Long
    val deviceAvailableExternalStorage: Long
    val deviceOrientation: String
    val deviceLanguage: String
    val deviceBatteryPercent: Int
    val deviceIsCharging: Boolean
}

class DeviceCollector private constructor(private val context: Context) : Collector, DeviceData {

    override val name: String
        get() = "DeviceData"
    override var enabled: Boolean = true

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    private val point = Point()
    private val intent = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    private val batteryStatus = context.registerReceiver(null, intent)

    override val device = if (Build.MODEL.startsWith(Build.MANUFACTURER)) Build.MODEL
            ?: "" else "${Build.MANUFACTURER} ${Build.MODEL}"
    override val deviceModel: String = Build.MODEL
    override val deviceManufacturer: String = Build.MANUFACTURER
    override val deviceArchitecture =
            if (Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()) "64bit" else "32bit"
    override val deviceCpuType = System.getProperty("os.arch") ?: "unknown"
    override val deviceResolution = point.let {
        windowManager.defaultDisplay.getSize(it)
        "${it.x}x${it.y}"
    }
    override val deviceLogicalResolution = DisplayMetrics().let { metrics ->
        windowManager.defaultDisplay.getRealMetrics(metrics)
        val x = (metrics.widthPixels / metrics.density).roundToInt()
        val y = (metrics.heightPixels / metrics.density).roundToInt()
        "${x}x${y}"
    }
    override val deviceRuntime = System.getProperty("java.vm.version") ?: "unknown"
    override val deviceOrigin =
            if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) "tv" else "mobile"
    override val devicePlatform = "android"
    override val deviceOsName = "Android"
    override val deviceOsBuild = Build.VERSION.INCREMENTAL ?: ""
    override val deviceOsVersion = Build.VERSION.RELEASE ?: ""
    override val deviceAvailableSystemStorage: Long
        get() {
            return StatFs(Environment.getRootDirectory().path).let {
                (it.availableBlocksLong * it.blockSizeLong)
            }
        }
    override val deviceAvailableExternalStorage: Long
        get() {
            return StatFs(Environment.getExternalStorageDirectory().path).let { external ->
                (external.availableBlocksLong * external.blockSizeLong)
            }

        }
    override val deviceOrientation: String
        get() {
            return when (windowManager.defaultDisplay.rotation) {
                Surface.ROTATION_90 -> "Landscape Right"
                Surface.ROTATION_180 -> "Portrait UpsideDown"
                Surface.ROTATION_270 -> "Landscape Left"
                else -> "Portrait"
            }
        }
    override val deviceLanguage: String
        get() = Locale.getDefault().toLanguageTag()

    override val deviceBatteryPercent: Int
        get() {
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

            return ((level.toFloat() / scale.toFloat()) * 100).roundToInt()
        }

    override val deviceIsCharging: Boolean
        get() {
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        }

    override suspend fun collect(): Map<String, Any> {
        return mapOf(
                Dispatch.Keys.DEVICE to device,
                Dispatch.Keys.DEVICE_MODEL to deviceModel,
                Dispatch.Keys.DEVICE_MANUFACTURER to deviceManufacturer,
                Dispatch.Keys.DEVICE_ARCHITECTURE to deviceArchitecture,
                Dispatch.Keys.DEVICE_CPU_TYPE to deviceCpuType,
                Dispatch.Keys.DEVICE_RESOLUTION to deviceResolution,
                Dispatch.Keys.DEVICE_LOGICAL_RESOLUTION to deviceLogicalResolution,
                Dispatch.Keys.DEVICE_RUNTIME to deviceRuntime,
                Dispatch.Keys.DEVICE_ORIGIN to deviceOrigin,
                Dispatch.Keys.DEVICE_PLATFORM to devicePlatform,
                Dispatch.Keys.DEVICE_OS_NAME to deviceOsName,
                Dispatch.Keys.DEVICE_OS_BUILD to deviceOsBuild,
                Dispatch.Keys.DEVICE_OS_VERSION to deviceOsVersion,
                Dispatch.Keys.DEVICE_AVAILABLE_SYSTEM_STORAGE to deviceAvailableSystemStorage,
                Dispatch.Keys.DEVICE_AVAILABLE_EXTERNAL_STORAGE to deviceAvailableExternalStorage,
                Dispatch.Keys.DEVICE_ORIENTATION to deviceOrientation,
                Dispatch.Keys.DEVICE_LANGUAGE to deviceLanguage,
                Dispatch.Keys.DEVICE_BATTERY_PERCENT to deviceBatteryPercent,
                Dispatch.Keys.DEVICE_ISCHARGING to deviceIsCharging
        )
    }

    companion object : CollectorFactory {
        const val MODULE_VERSION = BuildConfig.LIBRARY_VERSION

        @Volatile
        private var instance: Collector? = null

        override fun create(context: TealiumContext): Collector = instance ?: synchronized(this) {
            instance ?: DeviceCollector(context.config.application).also { instance = it }
        }
    }
}

val Collectors.Device: CollectorFactory
    get() = DeviceCollector