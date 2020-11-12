package com.tealium.visitorservice

import android.app.Application
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

class TealiumConfigVisitorServiceTest {

    @MockK
    lateinit var mockApplication: Application

    @MockK
    lateinit var mockFile: File

    private val accountName = "account"
    private val profileName = "profile"
    private val environment = Environment.DEV

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { mockApplication.filesDir } returns mockFile
    }

    @Test
    fun overrides_VisitorServiceUrlOverride_SetsCorrectly() {
        val config = TealiumConfig(mockApplication, accountName, profileName, environment)
        val override = "my.visitorservice.com"
        assertNull(config.overrideVisitorServiceUrl)
        config.overrideVisitorServiceUrl = override
        assertEquals(override, config.overrideVisitorServiceUrl)
        assertEquals(override, config.options[VISITOR_SERVICE_OVERRIDE_URL])
    }

    @Test
    fun overrides_VisitorServiceIntervalOverride_SetsCorrectly() {
        val config = TealiumConfig(mockApplication, accountName, profileName, environment)
        val override = TimeUnit.MINUTES.toSeconds(10)
        assertNull(config.visitorServiceRefreshInterval)
        config.visitorServiceRefreshInterval = override
        assertEquals(override, config.visitorServiceRefreshInterval!!)
        assertEquals(override, config.options[VISITOR_SERVICE_REFRESH_INTERVAL])
    }
}
