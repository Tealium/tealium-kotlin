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
import com.tealium.core.persistence.DataLayer
import com.tealium.core.persistence.Expiry
import com.tealium.tealiumlibrary.BuildConfig
import java.util.*

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
    val deviceOsName: String
    val deviceOsBuild: String
    val deviceOsVersion: String
    val deviceAvailableSystemStorage: Long
    val deviceAvailableExternalStorage: Long
    val deviceOrientation: String
    val deviceLanguage: String
}

class DeviceCollector private constructor(
        tealiumContext: TealiumContext,
        context: Context,
        private val dataLayer: DataLayer = tealiumContext.dataLayer) : Collector, DeviceData {

    override val name: String
        get() = "DeviceData"
    override var enabled: Boolean = true

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    private val point = Point()

    private val _device: String
    private val _deviceModel: String
    private val _deviceManufacturer: String
    private val _deviceArchitecture: String
    private val _deviceCpuType: String
    private val _deviceResolution: String
    private val _deviceRuntime: String
    private val _deviceOrigin: String
    private val _devicePlatform: String
    private val _deviceOsName: String
    private val _deviceOsBuild: String
    private val _deviceOsVersion: String

    init {
        _device = device
        _deviceModel = deviceModel
        _deviceManufacturer = deviceManufacturer
        _deviceArchitecture = deviceArchitecture
        _deviceCpuType = deviceCpuType
        _deviceResolution = deviceResolution
        _deviceRuntime = deviceRuntime
        _deviceOrigin = deviceOrigin
        _devicePlatform = devicePlatform
        _deviceOsName = deviceOsName
        _deviceOsBuild = deviceOsBuild
        _deviceOsVersion = deviceOsVersion
    }

    override val device: String
        get() = dataLayer.getOrPutString(DeviceCollectorConstants.DEVICE, {
            if (Build.MODEL.startsWith(Build.MANUFACTURER)) Build.MODEL
                    ?: "" else "${Build.MANUFACTURER} ${Build.MODEL}"
        }, Expiry.UNTIL_RESTART)
    override val deviceModel: String
        get() = dataLayer.getOrPutString(DeviceCollectorConstants.DEVICE_MODEL, {
            Build.MODEL
        }, Expiry.UNTIL_RESTART)
    override val deviceManufacturer: String
        get() = dataLayer.getOrPutString(DeviceCollectorConstants.DEVICE_MANUFACTURER, {
            Build.MANUFACTURER
        }, Expiry.UNTIL_RESTART)
    override val deviceArchitecture
        get() = dataLayer.getOrPutString(DeviceCollectorConstants.DEVICE_ARCHITECTURE, {
            if (Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()) "64bit" else "32bit"
        }, Expiry.UNTIL_RESTART)
    override val deviceCpuType
        get() = dataLayer.getOrPutString(DeviceCollectorConstants.DEVICE_CPU_TYPE, {
            System.getProperty("os.arch") ?: "unknown"
        }, Expiry.UNTIL_RESTART)
    override val deviceResolution
        get() = dataLayer.getOrPutString(DeviceCollectorConstants.DEVICE_RESOLUTION, {
            point.let {
                windowManager.defaultDisplay.getSize(it)
                "${it.x}x${it.y}"
            }
        }, Expiry.UNTIL_RESTART)

    override val deviceRuntime
        get() = dataLayer.getOrPutString(DeviceCollectorConstants.DEVICE_RUNTIME, {
            System.getProperty("java.vm.version") ?: "unknown"
        }, Expiry.UNTIL_RESTART)
    override val deviceOrigin
        get() = dataLayer.getOrPutString(DeviceCollectorConstants.DEVICE_ORIGIN, {
            if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) "tv" else "mobile"
        }, Expiry.UNTIL_RESTART)
    override val devicePlatform
        get() = dataLayer.getOrPutString(DeviceCollectorConstants.DEVICE_PLATFORM, {
            "android"
        }, Expiry.UNTIL_RESTART)
    override val deviceOsName
        get() = dataLayer.getOrPutString(DeviceCollectorConstants.DEVICE_OS_NAME, {
            "Android"
        }, Expiry.UNTIL_RESTART)
    override val deviceOsBuild
        get() = dataLayer.getOrPutString(DeviceCollectorConstants.DEVICE_OS_BUILD, {
            Build.VERSION.INCREMENTAL ?: ""
        }, Expiry.UNTIL_RESTART)
    override val deviceOsVersion
        get() = dataLayer.getOrPutString(DeviceCollectorConstants.DEVICE_OS_VERSION, {
            Build.VERSION.RELEASE ?: ""
        }, Expiry.UNTIL_RESTART)

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

    override suspend fun collect(): Map<String, Any> {
        return mapOf(
//                DeviceCollectorConstants.DEVICE to device,
//                DeviceCollectorConstants.DEVICE_MODEL to deviceModel,
//                DeviceCollectorConstants.DEVICE_MANUFACTURER to deviceManufacturer,
//                DeviceCollectorConstants.DEVICE_ARCHITECTURE to deviceArchitecture,
//                DeviceCollectorConstants.DEVICE_CPU_TYPE to deviceCpuType,
//                DeviceCollectorConstants.DEVICE_RESOLUTION to deviceResolution,
//                DeviceCollectorConstants.DEVICE_RUNTIME to deviceRuntime,
//                DeviceCollectorConstants.DEVICE_ORIGIN to deviceOrigin,
//                DeviceCollectorConstants.DEVICE_PLATFORM to devicePlatform,
//                DeviceCollectorConstants.DEVICE_OS_NAME to deviceOsName,
//                DeviceCollectorConstants.DEVICE_OS_BUILD to deviceOsBuild,
//                DeviceCollectorConstants.DEVICE_OS_VERSION to deviceOsVersion,
                DeviceCollectorConstants.DEVICE_AVAILABLE_SYSTEM_STORAGE to deviceAvailableSystemStorage,
                DeviceCollectorConstants.DEVICE_AVAILABLE_EXTERNAL_STORAGE to deviceAvailableExternalStorage,
                DeviceCollectorConstants.DEVICE_ORIENTATION to deviceOrientation,
                DeviceCollectorConstants.DEVICE_LANGUAGE to deviceLanguage
        )
    }

    companion object : CollectorFactory {
        const val MODULE_VERSION = BuildConfig.LIBRARY_VERSION

        @Volatile
        private var instance: Collector? = null

        override fun create(context: TealiumContext): Collector = instance ?: synchronized(this) {
            instance ?: DeviceCollector(context, context.config.application).also { instance = it }
        }
    }
}

val Collectors.Device: CollectorFactory
    get() = DeviceCollector