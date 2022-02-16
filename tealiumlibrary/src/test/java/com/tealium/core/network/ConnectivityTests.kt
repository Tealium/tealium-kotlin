package com.tealium.core.network

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import com.tealium.core.network.ConnectivityRetriever.Companion.UNKNOWN_CONNECTIVITY
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.Exception

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21, 30])
class ConnectivityTests {

    lateinit var connectivity: Connectivity

    @MockK
    lateinit var mockApplication: Application

    @MockK
    lateinit var mockConnectivityManager: ConnectivityManager

    @MockK
    lateinit var mockNetwork: Network

    @MockK
    lateinit var mockCapabilities: NetworkCapabilities

    @MockK
    lateinit var mockNetworkInfo: NetworkInfo

    @MockK
    lateinit var mockWifiNetworkInfo: NetworkInfo

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { mockApplication.getSystemService(any()) } returns mockConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            every { mockConnectivityManager.activeNetwork } returns mockNetwork
            every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockCapabilities
            // default: has internet, and wifi
            every { mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
            every { mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        } else {
            every { mockConnectivityManager.activeNetworkInfo } returns mockNetworkInfo
            every { mockConnectivityManager.allNetworks } returns arrayOf(mockNetwork)
            every { mockConnectivityManager.getNetworkInfo(mockNetwork) } returns mockNetworkInfo
            // default: has internet, and wifi
            every { mockConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI) } returns mockWifiNetworkInfo
            every { mockWifiNetworkInfo.isConnected } returns true
            every { mockNetworkInfo.isConnected } returns true
        }

        connectivity = ConnectivityRetriever.getTestInstance(mockApplication)
    }

    @Test
    fun connectivity_CorrectConnectivityReturned() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            assertTrue(connectivity is ConnectivityRetriever)
        else assertTrue(connectivity is LegacyConnectivityRetriever)
    }

    @Test(expected = Test.None::class)
    fun connectivity_DoesNotThrow() {
        connectivity.isConnected()
        connectivity.isConnectedWifi()
        val type = connectivity.connectionType()
        assertEquals("wifi", type)
    }

    @Test(expected = Test.None::class)
    fun connectivity_IsNotConnected() {
        every { mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns false
        every { mockNetworkInfo.isConnected } returns false

        assertFalse(connectivity.isConnected())
    }

    @Test(expected = Test.None::class)
    fun connectivity_IsNotConnectedWifi() {
        every { mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { mockWifiNetworkInfo.isConnected } returns false

        assertTrue(connectivity.isConnected())
        assertFalse(connectivity.isConnectedWifi())
    }

    @Test(expected = Test.None::class)
    @Config(sdk = [30])
    fun connectivity_DoesNotThrow_WhenNetworkCapabilitiesThrows() {
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } throws Exception()

        val connectionType = connectivity.connectionType()
        assertEquals(UNKNOWN_CONNECTIVITY, connectionType)
    }
}