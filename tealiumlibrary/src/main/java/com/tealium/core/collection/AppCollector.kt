package com.tealium.core.collection

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Process
import com.tealium.core.*
import com.tealium.core.persistence.DataLayer
import com.tealium.core.persistence.Expiry
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.Dispatch.Keys.APP_UUID
import com.tealium.tealiumlibrary.BuildConfig
import java.util.*

interface AppData : Collector {
    val appUuid: String
    val appRdns: String
    val appName: String
    val appBuild: String
    val appVersion: String
    val appMemoryUsage: Long
}

class AppCollector(private val context: Context, private val dataLayer: DataLayer) : Collector,
    AppData {

    override val name: String
        get() = "AppData"
    override var enabled: Boolean = true

    private val activityManager =
        context.applicationContext.getSystemService(Service.ACTIVITY_SERVICE) as ActivityManager

    override val appUuid: String
        get() {
            return dataLayer.getString(APP_UUID)
                ?: UUID.randomUUID().toString().also {
                    dataLayer.putString(APP_UUID, it, Expiry.FOREVER)
                }
        }
    override val appRdns: String = context.applicationContext.packageName
    override val appName: String = 
        if (context.applicationInfo.labelRes != 0) context.getString(context.applicationInfo.labelRes) else context.applicationInfo.nonLocalizedLabel.toString()
    override val appBuild: String = getPackageContext().versionCode.toString()
    override val appVersion: String = getPackageContext().versionName?.toString() ?: ""
    override val appMemoryUsage: Long
        get() {
            var memoryUsage = 0L
            try {
                val pids = arrayOf(Process.myPid())
                activityManager.getProcessMemoryInfo(pids.toIntArray()).forEach {
                    memoryUsage += it.totalPss
                }
                memoryUsage = memoryUsage.div(1024)
            } catch (e: Exception) {

            }
            return memoryUsage
        }

    init {
        Logger.dev(BuildConfig.TAG, "Fetching App UUID: $appUuid")
    }

    private fun getPackageContext(): PackageInfo {
        return context.packageManager.getPackageInfo(context.packageName, 0)
    }

    override suspend fun collect(): Map<String, Any> {
        return mapOf(
            Dispatch.Keys.APP_RDNS to appRdns,
            Dispatch.Keys.APP_NAME to appName,
            Dispatch.Keys.APP_VERSION to appVersion,
            Dispatch.Keys.APP_BUILD to appBuild,
            Dispatch.Keys.APP_MEMORY_USAGE to appMemoryUsage
        )
    }

    companion object : CollectorFactory {
        const val MODULE_VERSION = BuildConfig.LIBRARY_VERSION
        override fun create(context: TealiumContext): Collector {
            return AppCollector(context.config.application, context.dataLayer)
        }
    }
}

val Collectors.App: CollectorFactory
    get() = AppCollector
