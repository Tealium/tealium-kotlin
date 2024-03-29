package com.tealium.core.collection

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.persistence.DataLayer
import com.tealium.dispatcher.Dispatch
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

class TimeCollectorTests {

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
    fun testTimeCollection() = runBlocking {
        var timeCollector = spyk<TimeCollector>()
        // known date - 01-01-2000 00:00:00
        every { timeCollector.timestampUnixMilliseconds } returns 946684800000
        // fix the default TimeZone to California (-8 on the date given above)
        val losAngeles = TimeZone.getTimeZone("America/Los_Angeles")
        TimeZone.setDefault(losAngeles)
        timeCollector.localDateFormat.timeZone = losAngeles

        val data = timeCollector.collect()
        assertEquals("2000-01-01T00:00:00Z", data[Dispatch.Keys.TIMESTAMP])
        assertEquals("1999-12-31T16:00:00", data[Dispatch.Keys.TIMESTAMP_LOCAL])
        assertEquals("-8", data[Dispatch.Keys.TIMESTAMP_OFFSET])
        assertEquals(946684800L, data[Dispatch.Keys.TIMESTAMP_UNIX])
        assertEquals(946684800000L, data[Dispatch.Keys.TIMESTAMP_UNIX_MILLISECONDS])
    }

    @Test
    fun testFactoryCreation() {
        val factory = TimeCollector
        assertNotNull(factory.create(tealiumContext))
    }

    @Test
    fun testSingletonsCreateOnlyOneInstance() {
        val timeCollector1 = TimeCollector.create(tealiumContext)
        val timeCollector2 = TimeCollector.create(tealiumContext)
        assertSame(timeCollector1, timeCollector2)
    }
}