package com.tealium.core.collection

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.network.Connectivity
import com.tealium.core.persistence.DataLayer
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ConnectivityCollectorTests {

    @MockK
    lateinit var tealiumContext: TealiumContext

    @MockK
    lateinit var dataLayer: DataLayer

    @MockK
    lateinit var config: TealiumConfig

    lateinit var context: Application

    val account = "teal-account"
    val profile = "teal-profile"
    val environment = Environment.DEV
    val dataSource = "teal-data-source"

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        context = ApplicationProvider.getApplicationContext()

        every { config.accountName } returns account
        every { config.profileName } returns profile
        every { config.environment } returns environment
        every { config.dataSourceId } returns dataSource
        every { config.application } returns context
        every { tealiumContext.config } returns config
        every { tealiumContext.dataLayer } returns dataLayer
    }

    @Test
    fun testConnectivityCollector() = runBlocking {
        val mockConnectivityRetriever = mockk<Connectivity>()
        every { mockConnectivityRetriever.connectionType() } returns "wifi"
        every { mockConnectivityRetriever.isConnected() } returns true
        every { mockConnectivityRetriever.isConnectedWifi() } returns true

        val connectivityCollector = ConnectivityCollector(context, mockConnectivityRetriever)
        var data = connectivityCollector.collect()
        assertNotNull(data[ConnectivityCollectorConstants.CARRIER])
        assertNotNull(data[ConnectivityCollectorConstants.CARRIER_ISO])
        assertNotNull(data[ConnectivityCollectorConstants.CARRIER_MCC])
        assertNotNull(data[ConnectivityCollectorConstants.CARRIER_MNC])
        assertEquals(true, data[ConnectivityCollectorConstants.IS_CONNECTED])
        assertEquals("wifi", data[ConnectivityCollectorConstants.CONNECTION_TYPE])

        every { mockConnectivityRetriever.isConnected() } returns false
        every { mockConnectivityRetriever.connectionType() } returns "cellular"
        data = connectivityCollector.collect()
        assertEquals(false, data[ConnectivityCollectorConstants.IS_CONNECTED])
        assertEquals("cellular", data[ConnectivityCollectorConstants.CONNECTION_TYPE])
    }

    @Test
    fun testFactoryCreation() {
        val factory = ConnectivityCollector
        assertNotNull(factory.create(tealiumContext))
    }

    @Test
    fun testSingletonsCreateOnlyOneInstance() {
        val connectivityCollector1 = ConnectivityCollector.create(tealiumContext)
        val connectivityCollector2 = ConnectivityCollector.create(tealiumContext)
        assertSame(connectivityCollector1, connectivityCollector2)
    }
}