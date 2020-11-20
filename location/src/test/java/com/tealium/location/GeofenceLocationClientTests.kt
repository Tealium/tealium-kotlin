package com.tealium.location

import android.app.Application
import android.app.PendingIntent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GeofenceLocationClientTests {

    @RelaxedMockK
    lateinit var mockGeofencingClient: GeofencingClient

    @MockK
    lateinit var mockApplication: Application

    @MockK
    lateinit var mockConfig: TealiumConfig

    @MockK
    lateinit var mockContext: TealiumContext

    lateinit var geofenceLocationClient: GeofenceLocationClient

    val geofence1: Geofence = Geofence.Builder()
            .setRequestId("geofence_1")
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
            .setExpirationDuration(-1)
            .setCircularRegion(50.0, 50.0, 10f)
            .build()
    val geofence2: Geofence = Geofence.Builder()
            .setRequestId("geofence_2")
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL)
            .setExpirationDuration(-1)
            .setCircularRegion(50.0, 50.0, 10f)
            .setLoiteringDelay(50)
            .build()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { mockContext.config } returns mockConfig
        every { mockConfig.application } returns mockApplication
        every { mockApplication.applicationContext } returns mockApplication

        mockkStatic(LocationServices::class)
        every { LocationServices.getGeofencingClient(mockApplication) } returns mockGeofencingClient

        geofenceLocationClient = GeofenceLocationClient(mockContext)
    }

    @Test
    fun addGeofence_AddsGeofencesToClient() {
        val geofenceLocation1: GeofenceLocation = mockk()
        val geofenceLocation2: GeofenceLocation = mockk()
        every { geofenceLocation1.geofence } returns geofence1
        every { geofenceLocation2.geofence } returns geofence2

        mockkStatic(PendingIntent::class)
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns mockk()

        geofenceLocationClient.addGeofence(listOf(geofenceLocation1, geofenceLocation2))

        verify {
            mockGeofencingClient.addGeofences(match {
                it.geofences.count() == 2
                        && it.geofences.find { it.requestId == "geofence_1" } != null
                        && it.geofences.find { it.requestId == "geofence_2" } != null
            }, any())
        }
    }

    @Test
    fun removeGeofence_ShouldRemoveFromClientAndManager() {
        LocationManager.activeGeofences.add("test")
        geofenceLocationClient.removeGeofence("test")

        assertFalse(LocationManager.activeGeofences.contains("test"))
        verify {
            mockGeofencingClient.removeGeofences(mutableListOf("test"))
        }
    }

    @Test
    fun geofencingRequest_ContainsAllGeofences() {
        val geofenceLocation1: GeofenceLocation = mockk()
        val geofenceLocation2: GeofenceLocation = mockk()
        every { geofenceLocation1.geofence } returns geofence1
        every { geofenceLocation2.geofence } returns geofence2

        val geofenceRequest = geofenceLocationClient.geofencingRequest(listOf(geofenceLocation1, geofenceLocation2))

        assertEquals(GeofencingRequest.INITIAL_TRIGGER_DWELL, geofenceRequest.initialTrigger)
        assertEquals(2, geofenceRequest.geofences.count())
        assertNotNull(geofenceRequest.geofences.find { it.requestId == "geofence_1" })
        assertNotNull(geofenceRequest.geofences.find { it.requestId == "geofence_2" })
    }
}