package com.tealium.hosteddatalayer

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

class TealiumConfigHostedDataLayerTest {

    @MockK
    lateinit var mockApplication: Application

    @MockK
    lateinit var mockFile: File

    lateinit var config: TealiumConfig

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { mockApplication.filesDir } returns mockFile
        config = TealiumConfig(mockApplication, "test", "test", Environment.DEV)
    }

    @Test
    fun config_OverrideEventMappings_SetsCorrectly() {
        assertNull(config.hostedDataLayerEventMappings)

        val eventMappings = mapOf("key" to "value")
        config.hostedDataLayerEventMappings = eventMappings

        assertEquals(eventMappings, config.hostedDataLayerEventMappings)
        assertEquals(eventMappings, config.options[HOSTED_DATA_LAYER_EVENT_MAPPINGS])
    }

    @Test
    fun config_OverrideMaxCacheSize_SetsCorrectly() {
        assertNull(config.hostedDataLayerMaxCacheSize)

        val cacheSize = 10
        config.hostedDataLayerMaxCacheSize = cacheSize

        assertEquals(cacheSize, config.hostedDataLayerMaxCacheSize)
        assertEquals(cacheSize, config.options[HOSTED_DATA_LAYER_MAX_CACHE_SIZE])
    }

    @Test
    fun config_OverrideMaxCacheTime_SetsCorrectly() {
        assertNull(config.hostedDataLayerMaxCacheTimeMinutes)

        val cacheTime = 10L
        config.hostedDataLayerMaxCacheTimeMinutes = cacheTime

        assertEquals(cacheTime, config.hostedDataLayerMaxCacheTimeMinutes!!)
        assertEquals(cacheTime, config.options[HOSTED_DATA_LAYER_MAX_CACHE_TIME])
    }
}