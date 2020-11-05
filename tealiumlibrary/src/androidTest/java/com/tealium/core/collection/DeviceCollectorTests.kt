package com.tealium.core.collection

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.persistence.DataLayer
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DeviceCollectorTests {

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
    fun testFactoryCreation() {
        val factory = DeviceCollector
        assertNotNull(factory.create(tealiumContext))
    }

    @Test
    fun testSingletonsCreateOnlyOneInstance() {
        val deviceCollector1 = DeviceCollector.create(tealiumContext)
        val deviceCollector2 = DeviceCollector.create(tealiumContext)
        assertSame(deviceCollector1, deviceCollector2)
    }
}