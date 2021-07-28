package com.tealium.core.collection

import AppCollectorConstants.APP_BUILD
import AppCollectorConstants.APP_NAME
import AppCollectorConstants.APP_RDNS
import AppCollectorConstants.APP_UUID
import AppCollectorConstants.APP_VERSION
import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Process
import com.tealium.core.*
import com.tealium.core.persistence.DataLayer
import com.tealium.core.persistence.Expiry
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

class AppCollector(private val context: Context, private val dataLayer: DataLayer) : Collector, AppData {

    override val name: String
        get() = "AppData"
    override var enabled: Boolean = true

    private val activityManager = context.applicationContext.getSystemService(Service.ACTIVITY_SERVICE) as ActivityManager

    private var _appUuid: String
    private var _appRdns: String
    private var _appName: String
    private var _appBuild: String
    private var _appVersion: String

    init {
        _appUuid = appUuid
        _appRdns = appRdns
        _appName = appName
        _appBuild = appBuild
        _appVersion = appVersion
    }

    override val appUuid: String
        get() {
            return dataLayer.getOrPutString(APP_UUID, { UUID.randomUUID().toString() }, Expiry.FOREVER)
          }
    override val appRdns: String
        get() = dataLayer.getOrPutString(APP_RDNS, { context.applicationContext.packageName }, Expiry.UNTIL_RESTART)
    override val appName: String
        get() = dataLayer.getOrPutString(APP_NAME, { if (context.applicationInfo.labelRes != 0) context.getString(context.applicationInfo.labelRes) else "" }, Expiry.UNTIL_RESTART)
    override val appBuild: String
        get() = dataLayer.getOrPutString(APP_BUILD, { getPackageContext().versionCode.toString() }, Expiry.UNTIL_RESTART)
    override val appVersion: String
        get() = dataLayer.getOrPutString(APP_VERSION, { getPackageContext().versionName?.toString() ?: "" }, Expiry.UNTIL_RESTART)
    override val appMemoryUsage: Long
        get() {
            var memoryUsage = 0L
            try {
                val pids = arrayOf( Process.myPid() )
                activityManager.getProcessMemoryInfo(pids.toIntArray()).forEach {
                    memoryUsage += it.totalPss
                }
                memoryUsage = memoryUsage.div(1024)
            } catch (e: Exception) {

            }
            return memoryUsage
        }

    private fun getPackageContext() : PackageInfo {
        return context.packageManager.getPackageInfo(context.packageName, 0)
    }

    override suspend fun collect(): Map<String, Any> {
        return mapOf(
//                AppCollectorConstants.APP_RDNS to appRdns,
//                    AppCollectorConstants.APP_NAME to appName,
//                    AppCollectorConstants.APP_VERSION to appVersion,
//                    AppCollectorConstants.APP_BUILD to appBuild,
                    AppCollectorConstants.APP_MEMORY_USAGE to appMemoryUsage)
    }

    companion object: CollectorFactory {
        const val MODULE_VERSION = BuildConfig.LIBRARY_VERSION
        override fun create(context: TealiumContext): Collector {
            return AppCollector(context.config.application, context.dataLayer)
        }
    }
}

val Collectors.App : CollectorFactory
    get() = AppCollector
