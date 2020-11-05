package com.tealium.core.collection

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.persistence.DataLayer
import com.tealium.tealiumlibrary.BuildConfig
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class TealiumCollectorTests {

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
    fun testTealiumCollector() = runBlocking {
        every { tealiumContext.visitorId } returns "visitor_id"
        val tealiumCollector = TealiumCollector(tealiumContext)
        var data = tealiumCollector.collect()

        assertEquals(config.accountName, data[TealiumCollectorConstants.TEALIUM_ACCOUNT])
        assertEquals(config.profileName, data[TealiumCollectorConstants.TEALIUM_PROFILE])
        assertEquals(config.environment.environment, data[TealiumCollectorConstants.TEALIUM_ENVIRONMENT])
        assertEquals(config.dataSourceId, data[TealiumCollectorConstants.TEALIUM_DATASOURCE_ID])
        assertEquals("visitor_id", data[TealiumCollectorConstants.TEALIUM_VISITOR_ID])
        assertEquals(BuildConfig.LIBRARY_VERSION, data[TealiumCollectorConstants.TEALIUM_LIBRARY_VERSION])
        assertEquals(BuildConfig.LIBRARY_NAME, data[TealiumCollectorConstants.TEALIUM_LIBRARY_NAME])

        every { config.dataSourceId } returns null
        data = tealiumCollector.collect()
        assertNotNull(data[TealiumCollectorConstants.TEALIUM_DATASOURCE_ID])
    }

    @Test
    fun testFactoryCreation() {
        val factory = TealiumCollector
        assertNotNull(factory.create(tealiumContext))
    }
}