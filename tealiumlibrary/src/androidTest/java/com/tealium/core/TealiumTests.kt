package com.tealium.core

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.mockk.*
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class TealiumTests {

    lateinit var tealium: Tealium
    val application = ApplicationProvider.getApplicationContext<Application>()
    val configWithNoModules = TealiumConfig(application,
            "test",
            "test",
            Environment.DEV)

    @Before
    fun setUp() {
        tealium = Tealium("name", configWithNoModules)
    }

    @Test
    fun testVisitorIdIsGenerated() {
        assertNotNull(tealium.visitorId)
        assertEquals(32, tealium.visitorId.length)
        assertEquals(tealium.visitorId, tealium.dataLayer.getString("tealium_visitor_id"))
    }

    @Test
    fun testCallbackGetsExecuted() {
        val block : Tealium.() -> Unit = mockk(relaxed = true)
        every { block(hint(Tealium::class).any()) } just Runs

        val tealium = Tealium("name", configWithNoModules, block)

        verify( timeout = 1000 ) {
            block(tealium)
        }
    }
}