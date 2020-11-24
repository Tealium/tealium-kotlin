package com.tealium.core.network

import android.app.Application
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConnectivityTests {

    lateinit var connectivity: Connectivity

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        val app = ApplicationProvider.getApplicationContext<Application>()

        connectivity = ConnectivityRetriever.getInstance(app)
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
        assertFalse(type.isEmpty())
    }
}