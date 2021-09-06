package com.tealium.core.collection

import android.app.Service
import android.content.Context
import android.telephony.TelephonyManager
import com.tealium.core.*
import com.tealium.core.network.Connectivity
import com.tealium.core.network.ConnectivityRetriever
import com.tealium.dispatcher.Dispatch
import com.tealium.tealiumlibrary.BuildConfig

interface ConnectivityData : Collector {
    val carrier: String
    val carrierIso: String
    val carrierMcc: String
    val carrierMnc: String
}

class ConnectivityCollector(context: Context, private val connectivityRetriever: Connectivity) : Collector, ConnectivityData {

    override val name: String
        get() = "Connectivity"
    override var enabled: Boolean = true

    private val telephonyManager = context.applicationContext.getSystemService(Service.TELEPHONY_SERVICE) as TelephonyManager

    override val carrier: String = telephonyManager.networkOperatorName ?: ""
    override val carrierIso: String = telephonyManager.networkCountryIso ?: ""
    override val carrierMcc: String = if (telephonyManager.networkOperator.length > 3) telephonyManager.networkOperator.substring(0, 3) else ""
    override val carrierMnc: String = if (telephonyManager.networkOperator.length > 3) telephonyManager.networkOperator.substring(3) else ""

    override suspend fun collect(): Map<String, Any> {
        return mapOf(
                Dispatch.Keys.CONNECTION_TYPE to connectivityRetriever.connectionType(),
                Dispatch.Keys.IS_CONNECTED to connectivityRetriever.isConnected(),
                Dispatch.Keys.CARRIER to carrier,
                Dispatch.Keys.CARRIER_ISO to carrierIso,
                Dispatch.Keys.CARRIER_MCC to carrierMcc,
                Dispatch.Keys.CARRIER_MNC to carrierMnc
        )
    }

    companion object : CollectorFactory {
        const val MODULE_VERSION = BuildConfig.LIBRARY_VERSION
        @Volatile private var instance: ConnectivityCollector? = null
        override fun create(context: TealiumContext): Collector = instance ?: synchronized(this) {
            instance ?: ConnectivityCollector(
                    context.config.application.applicationContext,
                    ConnectivityRetriever.getInstance(context.config.application)
            ).also { instance = it }
        }
    }
}

val Collectors.Connectivity : CollectorFactory
    get() = ConnectivityCollector