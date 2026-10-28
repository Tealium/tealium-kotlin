package com.tealium.location

import android.Manifest
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Uses the real Play services clients rather than mocks, so these fail if the module was compiled
 * against a play-services-location version that is not binary compatible with the one resolved.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class PlayServicesClientTests {

    private lateinit var context: TealiumContext

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        context = mockk(relaxed = true)
        every { context.config } returns TealiumConfig(app, "test", "test", Environment.DEV)
    }

    @Test
    fun startLocationTracking_WithPlayServicesClient_DoesNotThrow() {
        FusedLocationProviderClientLoader(context).startLocationTracking(LocationTrackingOptions())
    }

    @Test
    fun addGeofence_WithPlayServicesClient_DoesNotThrow() {
        val geofence = GeofenceLocation.create("Geofence 1", 10.0, 20.0, 100, -1, 0, true, true)!!
        GeofenceLocationClient(context).addGeofence(listOf(geofence))
    }
}
