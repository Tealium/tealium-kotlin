package com.tealium.core.network

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.tealium.core.Logger
import com.tealium.tealiumlibrary.BuildConfig
import java.lang.Exception

interface Connectivity {
    fun isConnected(): Boolean

    fun isConnectedWifi(): Boolean

    fun connectionType(): String
}

class ConnectivityRetriever private constructor(val context: Application): Connectivity {

    private val connectivityManager: ConnectivityManager
        get() = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    internal val activeNetworkCapabilities: NetworkCapabilities?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                } catch (ex: Exception) {
                    Logger.qa(BuildConfig.TAG, "Error retrieving active network capabilities, ${ex.message}")
                    null
                }
            } else null

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

        @Volatile private var instance: Connectivity? = null

        fun getInstance(application: Application) = instance ?: synchronized(this){
            instance ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ConnectivityRetriever(application)
            } else {
                LegacyConnectivityRetriever(application)
            }.also { instance = it }
        }

        @JvmSynthetic
        internal fun getTestInstance(application: Application): Connectivity {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ConnectivityRetriever(application)
            } else {
                LegacyConnectivityRetriever(application)
            }
        }
    }
}

@Suppress("DEPRECATION")
class LegacyConnectivityRetriever(private val context: Application): Connectivity {

    private val connectivityManager: ConnectivityManager
        get() = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun isConnected(): Boolean {
        return connectivityManager.activeNetworkInfo?.isConnected ?: false
    }

    override fun isConnectedWifi(): Boolean {
        return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)?.isConnected ?:
        connectivityManager.allNetworks.fold(false) { input, network ->
            input || (connectivityManager.getNetworkInfo(network)?.isConnected ?: false)
        }
    }

    override fun connectionType(): String {
        return when {
            isConnectedWifi() -> "wifi"
            isConnected() -> "cellular"
            else -> "none"
        }
    }
}