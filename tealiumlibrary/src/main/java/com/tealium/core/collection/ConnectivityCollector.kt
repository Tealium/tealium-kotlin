package com.tealium.core.collection

import android.app.Service
import android.content.Context
import android.telephony.TelephonyManager
import com.tealium.core.*
import com.tealium.core.network.ConnectivityRetriever

interface ConnectivityData : Collector {
    val carrier: String
    val carrierIso: String
    val carrierMcc: String
    val carrierMnc: String
}

class ConnectivityCollector(context: Context, val connectivityRetriever: ConnectivityRetriever) : Collector, ConnectivityData {

    override val name: String
        get() = "CONNECTIVITY_COLLECTOR"
    override var enabled: Boolean = true

    private val telephonyManager = context.applicationContext.getSystemService(Service.TELEPHONY_SERVICE) as TelephonyManager

    override val carrier: String = telephonyManager.networkOperatorName ?: ""
    override val carrierIso: String = telephonyManager.networkCountryIso ?: ""
    override val carrierMcc: String = if (telephonyManager.networkOperator.length > 3) telephonyManager.networkOperator.substring(0, 3) else ""
    override val carrierMnc: String = if (telephonyManager.networkOperator.length > 3) telephonyManager.networkOperator.substring(3) else ""

    override suspend fun collect(): Map<String, Any> {
        return mapOf(
                ConnectivityCollectorConstants.CONNECTION_TYPE to connectivityRetriever.connectionType(),
                ConnectivityCollectorConstants.IS_CONNECTED to connectivityRetriever.isConnected(),
                ConnectivityCollectorConstants.CARRIER to carrier,
                ConnectivityCollectorConstants.CARRIER_ISO to carrierIso,
                ConnectivityCollectorConstants.CARRIER_MCC to carrierMcc,
                ConnectivityCollectorConstants.CARRIER_MNC to carrierMnc
        )
    }

    companion object : CollectorFactory {
        override fun create(context: TealiumContext): Collector {
            return ConnectivityCollector(
                    context.config.application.applicationContext,
                    ConnectivityRetriever(context.config.application)
            )
        }
    }
}

val Collectors.Connectivity : CollectorFactory
    get() = ConnectivityCollector
