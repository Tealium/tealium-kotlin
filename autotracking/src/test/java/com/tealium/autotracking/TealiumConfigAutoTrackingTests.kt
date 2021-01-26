package com.tealium.autotracking


import android.app.Application
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class TealiumConfigLocationTests {

    @MockK
    lateinit var mockApplication: Application

    @MockK
    lateinit var mockFile: File

    lateinit var config: TealiumConfig

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { mockApplication.filesDir } returns mockFile
        config = TealiumConfig(mockApplication, "tealiummobile", "test", Environment.DEV)
    }

    @Test
    fun setters_SetAutoTrackingMode() {
        assertEquals(AutoTrackingMode.FULL, config.autoTrackingMode)
        config.autoTrackingMode = AutoTrackingMode.ANNOTATED
        assertEquals(AutoTrackingMode.ANNOTATED, config.autoTrackingMode)
        assertEquals(AutoTrackingMode.ANNOTATED, config.options[AUTOTRACKING_MODE])
    }

    @Test
    fun setters_SetAutoTrackingFileName() {
        assertNull(config.autoTrackingBlacklistFilename)
        config.autoTrackingBlacklistFilename = "file"
        assertEquals("file", config.autoTrackingBlacklistFilename)
        assertEquals("file", config.options[AUTOTRACKING_BLACKLIST_FILENAME])

        config.autoTrackingBlacklistFilename = null
        assertNull(config.autoTrackingBlacklistFilename)
        assertNull(config.options[AUTOTRACKING_BLACKLIST_FILENAME])
    }

    @Test
    fun setters_SetAutoTrackingUrl() {
        assertNull(config.autoTrackingBlacklistUrl)
        config.autoTrackingBlacklistUrl = "file"
        assertEquals("file", config.autoTrackingBlacklistUrl)
        assertEquals("file", config.options[AUTOTRACKING_BLACKLIST_URL])

        config.autoTrackingBlacklistUrl = null
        assertNull(config.autoTrackingBlacklistUrl)
        assertNull(config.options[AUTOTRACKING_BLACKLIST_URL])
    }

    @Test
    fun setters_SetAutoTrackingDelegate() {
        assertNull(config.autoTrackingCollectorDelegate)
        val delegate: ActivityDataCollector = mockk()
        config.autoTrackingCollectorDelegate = delegate
        assertEquals(delegate, config.autoTrackingCollectorDelegate)
        assertEquals(delegate, config.options[AUTOTRACKING_COLLECTOR_DELEGATE])

        config.autoTrackingCollectorDelegate = null
        assertNull(config.autoTrackingCollectorDelegate)
        assertNull(config.options[AUTOTRACKING_COLLECTOR_DELEGATE])
    }
}