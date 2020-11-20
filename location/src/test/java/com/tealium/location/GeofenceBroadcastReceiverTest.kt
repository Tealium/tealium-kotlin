package com.tealium.location

import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test

class GeofenceBroadcastReceiverTest {

    @MockK
    lateinit var mockContext: Context

    @MockK
    lateinit var mockIntent: Intent

    @MockK
    lateinit var mockGeofencingEvent: GeofencingEvent

    @MockK
    lateinit var mockGeofence: Geofence

    val geofenceBroadcastReceiver = GeofenceBroadcastReceiver()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        // default to valid event
        every { mockGeofencingEvent.hasError() } returns false
        every { mockGeofence.requestId } returns "geofenceName"

        mockkObject(LocationManager)
        every { LocationManager.sendGeofenceEvent(any(), any()) } just Runs

        mockkStatic(GeofencingEvent::class)
        every { GeofencingEvent.fromIntent(any()) } returns mockGeofencingEvent
    }

    @Test
    fun onReceive_ShouldNotTrackWhenError() {
        every { mockGeofencingEvent.hasError() } returns true
        geofenceBroadcastReceiver.onReceive(mockContext, mockIntent)

        verify(exactly = 0) {
            LocationManager.sendGeofenceEvent(any(), any())
        }
    }

    @Test
    fun onReceive_ShouldNotTrackWhenInvalidTransition() {
        every { mockGeofencingEvent.geofenceTransition } returns -1
        geofenceBroadcastReceiver.onReceive(mockContext, mockIntent)

        verify(exactly = 0) {
            LocationManager.sendGeofenceEvent(any(), any())
        }
    }

    @Test
    fun onReceive_ShouldTrackOnEnter() {
        every { mockGeofencingEvent.geofenceTransition } returns Geofence.GEOFENCE_TRANSITION_ENTER
        every { mockGeofencingEvent.triggeringGeofences } returns listOf(mockGeofence)
        geofenceBroadcastReceiver.onReceive(mockContext, mockIntent)

        verify(exactly = 1) {
            LocationManager.sendGeofenceEvent("geofenceName", GeofenceTransitionType.ENTER)
        }
    }

    @Test
    fun onReceive_ShouldTrackOnDwell() {
        every { mockGeofencingEvent.geofenceTransition } returns Geofence.GEOFENCE_TRANSITION_DWELL
        every { mockGeofencingEvent.triggeringGeofences } returns listOf(mockGeofence)
        geofenceBroadcastReceiver.onReceive(mockContext, mockIntent)

        verify(exactly = 1) {
            LocationManager.sendGeofenceEvent("geofenceName", GeofenceTransitionType.DWELL)
        }
    }

    @Test
    fun onReceive_ShouldTrackOnExit() {
        every { mockGeofencingEvent.geofenceTransition } returns Geofence.GEOFENCE_TRANSITION_EXIT
        every { mockGeofencingEvent.triggeringGeofences } returns listOf(mockGeofence)
        geofenceBroadcastReceiver.onReceive(mockContext, mockIntent)

        verify(exactly = 1) {
            LocationManager.sendGeofenceEvent("geofenceName", GeofenceTransitionType.EXIT)
        }
    }

    @Test
    fun onReceive_ShouldTrackForEachGeofence() {
        every { mockGeofencingEvent.geofenceTransition } returns Geofence.GEOFENCE_TRANSITION_ENTER
        every { mockGeofencingEvent.triggeringGeofences } returns listOf(mockGeofence, mockGeofence, mockGeofence)
        geofenceBroadcastReceiver.onReceive(mockContext, mockIntent)

        verify(exactly = 3) {
            LocationManager.sendGeofenceEvent("geofenceName", GeofenceTransitionType.ENTER)
        }
    }
}