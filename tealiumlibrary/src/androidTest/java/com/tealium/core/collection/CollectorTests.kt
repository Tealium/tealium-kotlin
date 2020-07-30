package com.tealium.core.collection

import AppCollectorConstants
import ConnectivityCollectorConstants
import DeviceCollectorConstants
import TealiumCollectorConstants
import TimeCollectorConstants
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.runner.AndroidJUnit4
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.network.ConnectivityRetriever
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import junit.framework.TestCase.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class CollectorTestsAndroid {

    val tealiumContext = mockk<TealiumContext>()
    val account = "teal-account"
    val profile = "teal-profile"
    val environment = Environment.DEV
    val dataSource = "teal-data-source"
    lateinit var config: TealiumConfig
    lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        config = spyk(TealiumConfig(context.applicationContext as Application,
                account,
                profile,
                environment, dataSourceId = dataSource))

        every { tealiumContext.config } returns config
    }

    @Test
    fun testAppCollector() = runBlocking {
        val appCollector = AppCollector(context.applicationContext)
        val data = appCollector.collect()
        assertNotNull(data[AppCollectorConstants.APP_NAME])
        assertNotNull(data[AppCollectorConstants.APP_BUILD])
        assertNotNull(data[AppCollectorConstants.APP_VERSION])

        assertTrue(data[AppCollectorConstants.APP_RDNS] is String)
        assertTrue((data[AppCollectorConstants.APP_RDNS] as String).startsWith("com.tealium"))

        assertTrue(data[AppCollectorConstants.APP_MEMORY_USAGE] is Long)
        assertTrue((data[AppCollectorConstants.APP_MEMORY_USAGE] as Long) > 0)
    }

    @Test
    fun testConnectivityCollector() = runBlocking {
        val mockConnectivityRetriever = mockk<ConnectivityRetriever>()
        every { mockConnectivityRetriever.connectionType() } returns "wifi"
        every { mockConnectivityRetriever.isConnected() } returns true
        every { mockConnectivityRetriever.isConnectedWifi() } returns true

        val connectivityCollector = ConnectivityCollector(context.applicationContext, mockConnectivityRetriever)
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
    fun testDeviceCollector() = runBlocking {
        val deviceCollector = DeviceCollector.create(tealiumContext)
        val data = deviceCollector.collect()

        assertNotNull(data[DeviceCollectorConstants.DEVICE])
        assertTrue(
                (data[DeviceCollectorConstants.DEVICE_ARCHITECTURE] as String).startsWith("64") ||
                        (data[DeviceCollectorConstants.DEVICE_ARCHITECTURE] as String).startsWith("32")
        )
        assertTrue((data[DeviceCollectorConstants.DEVICE_AVAILABLE_SYSTEM_STORAGE] as Long) >= 0)
        assertTrue((data[DeviceCollectorConstants.DEVICE_AVAILABLE_EXTERNAL_STORAGE] as Long) >= 0)
        assertNotNull(data[DeviceCollectorConstants.DEVICE_CPU_TYPE])
        assertNotNull(data[DeviceCollectorConstants.DEVICE_ORIGIN])
        assertTrue(
                (data[DeviceCollectorConstants.DEVICE_ORIENTATION] as String).startsWith("Landscape") ||
                        (data[DeviceCollectorConstants.DEVICE_ORIENTATION] as String).startsWith("Portrait")
        )
        assertNotNull(data[DeviceCollectorConstants.DEVICE_OS_BUILD])
        assertNotNull(data[DeviceCollectorConstants.DEVICE_OS_VERSION])
        assertEquals("android", data[DeviceCollectorConstants.DEVICE_PLATFORM])
        assertNotNull(data[DeviceCollectorConstants.DEVICE_RUNTIME])
        assertTrue("[0-9]+x[0-9]+".toRegex().matches(data[DeviceCollectorConstants.DEVICE_RESOLUTION] as String))

        assertSame(DeviceCollector.create(tealiumContext), DeviceCollector.create(tealiumContext))
    }

    @Test
    fun testTealiumCollector() = runBlocking {
        every { tealiumContext.visitorId } returns "visitor_id"
        val tealiumCollector = TealiumCollector(tealiumContext)
        var data = tealiumCollector.collect()

        assertEquals(config.accountName, data[TealiumCollectorConstants.TEALIUM_ACCOUNT])
        assertEquals(config.profileName, data[TealiumCollectorConstants.TEALIUM_PROFILE])
        assertEquals(config.environment.environment, data[TealiumCollectorConstants.TEALIUM_ENVIRONMENT])
        assertEquals(config.dataSourceId, data[TealiumCollectorConstants.TEALIUM_DATASOURCE_ID])
        assertEquals("visitor_id", data[TealiumCollectorConstants.TEALIUM_VISITOR_ID])

        every { config.dataSourceId } returns null
        data = tealiumCollector.collect()
        assertNotNull(data[TealiumCollectorConstants.TEALIUM_DATASOURCE_ID])
    }

    @Test
    fun testTimeCollection() = runBlocking {
        var timeCollector = spyk<TimeCollector>()
        // known date - 01-01-2000 00:00:00
        every { timeCollector.timestampUnixMilliseconds } returns 946684800000
        // fix the default TimeZone to California (-8 on the date given above)
        val losAngeles = TimeZone.getTimeZone("America/Los_Angeles")
        TimeZone.setDefault(losAngeles)
        timeCollector.localDateFormat.timeZone = losAngeles

        val data = timeCollector.collect()
        assertEquals("2000-01-01T00:00:00Z", data[TimeCollectorConstants.TIMESTAMP])
        assertEquals("1999-12-31T16:00:00", data[TimeCollectorConstants.TIMESTAMP_LOCAL])
        assertEquals("-8", data[TimeCollectorConstants.TIMESTAMP_OFFSET])
        assertEquals(946684800L, data[TimeCollectorConstants.TIMESTAMP_UNIX])
        assertEquals(946684800000L, data[TimeCollectorConstants.TIMESTAMP_UNIX_MILLISECONDS])
    }

    @Test
    fun testFactoryCreation() {
        val collectors = listOf(
                AppCollector,
                ConnectivityCollector,
                DeviceCollector,
                TealiumCollector,
                TimeCollector
        )
        for (factory in collectors) {
            assertNotNull(factory.create(tealiumContext))
        }
    }
}