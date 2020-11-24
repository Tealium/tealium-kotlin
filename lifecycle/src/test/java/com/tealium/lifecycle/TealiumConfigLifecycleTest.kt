package com.tealium.lifecycle

import android.app.Application
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class TealiumConfigLifecycleTest {

    @MockK
    lateinit var mockApplication: Application

    @MockK
    lateinit var mockFile: File

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { mockApplication.filesDir } returns mockFile
    }

    @Test
    fun config_OverrideIsAutoTracking_SetsCorrectly() {
        val config = TealiumConfig(mockApplication, "test", "test", Environment.DEV)

        assertNull(config.isAutoTrackingEnabled)

        config.isAutoTrackingEnabled = true
        assertTrue(config.isAutoTrackingEnabled!!)
        config.isAutoTrackingEnabled = false
        assertFalse(config.isAutoTrackingEnabled!!)
    }
}