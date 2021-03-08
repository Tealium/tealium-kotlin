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
        assertNull(config.autoTrackingBlocklistFilename)
        config.autoTrackingBlocklistFilename = "file"
        assertEquals("file", config.autoTrackingBlocklistFilename)
        assertEquals("file", config.options[AUTOTRACKING_BLOCKLIST_FILENAME])

        config.autoTrackingBlocklistFilename = null
        assertNull(config.autoTrackingBlocklistFilename)
        assertNull(config.options[AUTOTRACKING_BLOCKLIST_FILENAME])
    }

    @Test
    fun setters_SetAutoTrackingUrl() {
        assertNull(config.autoTrackingBlocklistUrl)
        config.autoTrackingBlocklistUrl = "file"
        assertEquals("file", config.autoTrackingBlocklistUrl)
        assertEquals("file", config.options[AUTOTRACKING_BLOCKLIST_URL])

        config.autoTrackingBlocklistUrl = null
        assertNull(config.autoTrackingBlocklistUrl)
        assertNull(config.options[AUTOTRACKING_BLOCKLIST_URL])
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

    @Test
    fun setters_SetPushTrackingEnabled() {
        assertNull(config.autoTrackingPushEnabled)
        config.autoTrackingPushEnabled = true
        assertEquals(true, config.autoTrackingPushEnabled)
        assertEquals(true, config.options[AUTOTRACKING_PUSH_ENABLED])
        config.autoTrackingPushEnabled = false
        assertEquals(false, config.autoTrackingPushEnabled)
        assertEquals(false, config.options[AUTOTRACKING_PUSH_ENABLED])
    }
}