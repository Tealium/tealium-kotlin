package com.tealium.core.network

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.tealium.dispatcher.Dispatch
import com.tealium.core.validation.DispatchValidator

interface Connectivity {
    fun isConnected(): Boolean

    fun isConnectedWifi(): Boolean

    fun connectionType(): String
}

class ConnectivityRetriever(val context: Application): Connectivity {

    private val connectivityManager: ConnectivityManager
        get() = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val activeNetworkCapabilities: NetworkCapabilities?
        get() = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

    override fun connectionType(): String {
        return activeNetworkCapabilities?.let {
            return when {
                it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
                it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
                it.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "vpn"
                else -> UNKNOWN_CONNECTIVITY
            }
        } ?: UNKNOWN_CONNECTIVITY
    }

    override fun isConnected(): Boolean {
        return activeNetworkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
    }

    override fun isConnectedWifi(): Boolean {
        return isConnected() &&
                activeNetworkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
    }

    companion object {
        const val UNKNOWN_CONNECTIVITY = "unknown"
    }
}