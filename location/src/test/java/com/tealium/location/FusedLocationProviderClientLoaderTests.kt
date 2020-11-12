package com.tealium.location

import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FusedLocationProviderClientLoaderTests {

    @MockK
    lateinit var mockApplication: Application

    @MockK
    lateinit var mockContext: TealiumContext

    @MockK
    lateinit var mockConfig: TealiumConfig

    @RelaxedMockK
    lateinit var mockGeofenceLocationClient: GeofenceLocationClient

    @RelaxedMockK
    lateinit var mockLocationClient: FusedLocationProviderClient

    lateinit var fusedLocationProviderClientLoader: FusedLocationProviderClientLoader

    val geofenceLocation = GeofenceLocation("Tealium_Reading", 51.4610304, -0.9707625, 100, -1, 0, true, true)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { mockContext.config } returns mockConfig
        every { mockApplication.applicationContext } returns mockApplication
        // Default to PERMISSION_GRANTED
        every { mockApplication.checkSelfPermission(any()) } returns PackageManager.PERMISSION_GRANTED
        every { mockConfig.application } returns mockApplication
        every { mockConfig.options } returns mutableMapOf()
        every { mockConfig.overrideFusedLocationProviderClient } returns mockLocationClient

        fusedLocationProviderClientLoader = FusedLocationProviderClientLoader(mockContext)
    }

    @After
    fun tearDown() {
        LocationManager.activeGeofences.clear()
        LocationManager.allGeofenceLocations.clear()
    }

    @Test
    fun addGeofenceToClient_returnsWhenNoPermission() {
        every { mockApplication.checkSelfPermission(any()) } returns PackageManager.PERMISSION_DENIED
        fusedLocationProviderClientLoader.geofenceLocationClient = mockGeofenceLocationClient

        fusedLocationProviderClientLoader.addGeofenceToClient(listOf(geofenceLocation))

        verify {
            mockGeofenceLocationClient wasNot Called
        }
    }

    @Test
    fun addGeofenceToClient_addsGeofenceWhenGrantedPersmission() {
        fusedLocationProviderClientLoader.geofenceLocationClient = mockGeofenceLocationClient

        fusedLocationProviderClientLoader.addGeofenceToClient(listOf(geofenceLocation))

        verify {
            mockGeofenceLocationClient.addGeofence(listOf(geofenceLocation))
        }
        assertTrue(LocationManager.activeGeofences.contains(geofenceLocation.name))
    }

    @Test
    fun removeGeofencefromClient_removesFromBothClientAndCompanion() {
        fusedLocationProviderClientLoader.geofenceLocationClient = mockGeofenceLocationClient
        LocationManager.activeGeofences.add(geofenceLocation.name)
        assertTrue(LocationManager.activeGeofences.contains(geofenceLocation.name))

        fusedLocationProviderClientLoader.removeGeofenceFromClient(geofenceLocation.name)
        assertFalse(LocationManager.activeGeofences.contains(geofenceLocation.name))
        verify {
            mockGeofenceLocationClient.removeGeofence(geofenceLocation.name)
        }
    }

    @Test
    fun startLocationTracking_ReturnsWhenNoPermissionGranted() {
        every { mockApplication.checkSelfPermission(any()) } returns PackageManager.PERMISSION_DENIED
        assertNull(fusedLocationProviderClientLoader.locationRequest)

        fusedLocationProviderClientLoader.startLocationTracking(true, 10)
        assertNull(fusedLocationProviderClientLoader.locationRequest)
    }

    @Test
    fun startLocationTracking_ReturnsWhenInvalidInterval() {
        assertNull(fusedLocationProviderClientLoader.locationRequest)

        fusedLocationProviderClientLoader.startLocationTracking(true, -1)
        assertNull(fusedLocationProviderClientLoader.locationRequest)
    }

    @Test
    fun startLocationTracking_StartsLocationTracking() {
        assertNull(fusedLocationProviderClientLoader.locationRequest)

        fusedLocationProviderClientLoader.startLocationTracking(true, 10)
        assertNotNull(fusedLocationProviderClientLoader.locationRequest)
        assertNotNull(fusedLocationProviderClientLoader.locationCallback)
        assertEquals(LocationRequest.PRIORITY_HIGH_ACCURACY, fusedLocationProviderClientLoader.locationRequest?.priority)
        assertEquals(10L, fusedLocationProviderClientLoader.locationRequest?.interval)

        verify {
            mockLocationClient.requestLocationUpdates(fusedLocationProviderClientLoader.locationRequest,
                    fusedLocationProviderClientLoader.locationCallback,
                    any())
        }
    }

    @Test
    fun stopLocationTracking_RemovesLocationUpdates() {
        val mockLocationCallback: LocationCallback = mockk(relaxed = true)

        fusedLocationProviderClientLoader.locationCallback = mockLocationCallback
        fusedLocationProviderClientLoader.stopLocationUpdates()
        verify {
            mockLocationClient.removeLocationUpdates(fusedLocationProviderClientLoader.locationCallback)
        }
    }

    @Test
    fun callback_SetsLastLocation() {
        fusedLocationProviderClientLoader.geofenceLocationClient = mockk(relaxed = true)
        fusedLocationProviderClientLoader.startLocationTracking(true, 10)

        val locationResult: LocationResult = mockk(relaxed = true)
        val location: Location = mockk(relaxed = true)
        every { locationResult.lastLocation } returns location
        fusedLocationProviderClientLoader.locationCallback.onLocationResult(locationResult)
        assertSame(location, fusedLocationProviderClientLoader.lastLocation)
    }

    @Test
    fun callback_AddsNearbyGeofences() {
        fusedLocationProviderClientLoader.startLocationTracking(true, 10)
        fusedLocationProviderClientLoader.geofenceLocationClient = mockGeofenceLocationClient

        val locationResult: LocationResult = mockk(relaxed = true)
        val location: Location = mockk(relaxed = true)
        every { locationResult.lastLocation } returns location

        val alreadyActive = geofenceLocation.copy(name = "already_active")
        val withinRadius = geofenceLocation.copy(name = "within_radius")
        val outsideRadius = geofenceLocation.copy(name = "outside_radius")
        LocationManager.allGeofenceLocations.add(alreadyActive)
        LocationManager.activeGeofences.add(alreadyActive.name)
        LocationManager.allGeofenceLocations.add(withinRadius)
        LocationManager.allGeofenceLocations.add(outsideRadius)

        every { location.distanceTo(any()) } returnsMany mutableListOf(150.0f, 250.0f, 1000f)

        fusedLocationProviderClientLoader.locationCallback.onLocationResult(locationResult)

        verify {
            mockGeofenceLocationClient.addGeofence(listOf(withinRadius))
        }
    }

    @Test
    fun callback_RemovesFarAwayGeofences() {
        fusedLocationProviderClientLoader.startLocationTracking(true, 10)
        fusedLocationProviderClientLoader.geofenceLocationClient = mockGeofenceLocationClient

        val locationResult: LocationResult = mockk(relaxed = true)
        val location: Location = mockk(relaxed = true)
        every { locationResult.lastLocation } returns location

        val alreadyActive = geofenceLocation.copy(name = "already_active")
        val movedOutsideRadius = geofenceLocation.copy(name = "moved_outside_radius")
        LocationManager.allGeofenceLocations.add(alreadyActive)
        LocationManager.activeGeofences.add(alreadyActive.name)
        LocationManager.allGeofenceLocations.add(movedOutsideRadius)
        LocationManager.activeGeofences.add(movedOutsideRadius.name)

        every { location.distanceTo(any()) } returnsMany mutableListOf(150.0f, 1000f)

        fusedLocationProviderClientLoader.locationCallback.onLocationResult(locationResult)

        verify {
            mockGeofenceLocationClient.removeGeofence(movedOutsideRadius.name)
        }
    }
}
