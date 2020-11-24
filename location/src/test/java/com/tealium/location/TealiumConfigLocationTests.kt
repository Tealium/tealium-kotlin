package com.tealium.location

import android.app.Application
import com.google.android.gms.location.FusedLocationProviderClient
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
    fun setters_SetOverrideGeofenceFileName() {
        assertNull(config.geofenceFilename)
        config.geofenceFilename = "geofence.json"
        assertNotNull(config.geofenceFilename)
        assertEquals("geofence.json", config.geofenceFilename)
        assertEquals("geofence.json", config.options[GEOFENCE_FILENAME])
    }

    @Test
    fun setters_SetOverrideGeofenceUrl() {
        assertNull(config.overrideGeofenceUrl)
        config.overrideGeofenceUrl = "my.geofence.url"
        assertNotNull(config.overrideGeofenceUrl)
        assertEquals("my.geofence.url", config.overrideGeofenceUrl)
        assertEquals("my.geofence.url", config.options[GEOFENCE_URL])
    }

    @Test
    fun setters_SetOverrideFusedLocationProviderClient() {
        assertNull(config.overrideFusedLocationProviderClient)
        val mockClient: FusedLocationProviderClient = mockk()
        config.overrideFusedLocationProviderClient = mockClient
        assertNotNull(config.overrideFusedLocationProviderClient)
        assertEquals(mockClient, config.overrideFusedLocationProviderClient)
        assertEquals(mockClient, config.options[LOCATION_CLIENT])
    }
}