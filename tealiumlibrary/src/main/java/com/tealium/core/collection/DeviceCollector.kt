package com.tealium.core.collection

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.view.Surface
import android.view.WindowManager
import com.tealium.core.*

interface DeviceData {
    val device: String
    val deviceModel: String
    val deviceManufacturer: String
    val deviceArchitecture: String
    val deviceCpuType: String
    val deviceResolution: String
    val deviceRuntime: String
    val deviceOrigin: String
    val devicePlatform: String
    val deviceOsBuild: String
    val deviceOsVersion: String
    val deviceAvailableSystemStorage: Long
    val deviceAvailableExternalStorage: Long
    val deviceOrientation: String
}

class DeviceCollector private constructor(context: Context) : Collector, DeviceData {

    override val name: String
        get() = "DEVICE_COLLECTOR"
    override var enabled: Boolean = true

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    private val point = Point()

    override val device = if (Build.MODEL.startsWith(Build.MANUFACTURER)) Build.MODEL
            ?: "" else "${Build.MANUFACTURER} ${Build.MODEL}"
    override val deviceModel: String = Build.MODEL
    override val deviceManufacturer: String = Build.MANUFACTURER
    override val deviceArchitecture = if (Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()) "64bit" else "32bit"
    override val deviceCpuType = System.getProperty("os.arch") ?: "unknown"
    override val deviceResolution = point.let {
        windowManager.defaultDisplay.getSize(it)
        "${it.x}x${it.y}"
    }
    override val deviceRuntime = System.getProperty("java.vm.version") ?: "unknown"
    override val deviceOrigin = if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) "tv" else "mobile"
    override val devicePlatform = "android"
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

    override suspend fun collect(): Map<String, Any> {
        return mapOf(
                DeviceCollectorConstants.DEVICE to device,
                DeviceCollectorConstants.DEVICE_MODEL to deviceModel,
                DeviceCollectorConstants.DEVICE_MANUFACTURER to deviceManufacturer,
                DeviceCollectorConstants.DEVICE_ARCHITECTURE to deviceArchitecture,
                DeviceCollectorConstants.DEVICE_CPU_TYPE to deviceCpuType,
                DeviceCollectorConstants.DEVICE_RESOLUTION to deviceResolution,
                DeviceCollectorConstants.DEVICE_RUNTIME to deviceRuntime,
                DeviceCollectorConstants.DEVICE_ORIGIN to deviceOrigin,
                DeviceCollectorConstants.DEVICE_PLATFORM to devicePlatform,
                DeviceCollectorConstants.DEVICE_OS_BUILD to deviceOsBuild,
                DeviceCollectorConstants.DEVICE_OS_VERSION to deviceOsVersion,
                DeviceCollectorConstants.DEVICE_AVAILABLE_SYSTEM_STORAGE to deviceAvailableSystemStorage,
                DeviceCollectorConstants.DEVICE_AVAILABLE_EXTERNAL_STORAGE to deviceAvailableExternalStorage,
                DeviceCollectorConstants.DEVICE_ORIENTATION to deviceOrientation
        )
    }

    companion object : CollectorFactory {
        @Volatile private var instance: Collector? = null

        override fun create(context: TealiumContext): Collector = instance ?: synchronized(this){
            instance ?: DeviceCollector(context.config.application).also { instance = it }
        }
    }
}

val Collectors.Device: CollectorFactory
    get() = DeviceCollector