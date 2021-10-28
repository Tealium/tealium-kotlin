package com.tealium.autotracking.push

import android.app.Application
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File

class TealiumConfigPushTrackingTests {

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
    fun setters_SetPushTrackingEnabled() {
        Assert.assertNull(config.autoTrackingPushEnabled)
        config.autoTrackingPushEnabled = true
        Assert.assertEquals(true, config.autoTrackingPushEnabled)
        Assert.assertEquals(true, config.options[AUTOTRACKING_PUSH_ENABLED])
        config.autoTrackingPushEnabled = false
        Assert.assertEquals(false, config.autoTrackingPushEnabled)
        Assert.assertEquals(false, config.options[AUTOTRACKING_PUSH_ENABLED])
    }
}