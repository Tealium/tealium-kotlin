package com.tealium.location

import com.google.android.gms.location.Geofence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeofenceLocationTests {
    val default = GeofenceLocation.create("name",
            0.0,
            0.0,
            10,
            0,
            0,
            true,
            false)!!

    @Test
    fun create_ReturnsNull_WhenInvalidName() {
        val geofenceLocation = GeofenceLocation.create("",
                default.latitude,
                default.longitude,
                default.radius,
                default.expireTime,
                default.loiterTime,
                default.triggerEnter,
                default.triggerExit
        )
        assertNull(geofenceLocation)
    }

    @Test
    fun create_ReturnsNull_WhenInvalidLatitude() {
        val geofenceLocation = GeofenceLocation.create(default.name,
                -91.0,
                default.longitude,
                default.radius,
                default.expireTime,
                default.loiterTime,
                default.triggerEnter,
                default.triggerExit
        )
        assertNull(geofenceLocation)

        val geofenceLocation2 = GeofenceLocation.create(default.name,
                91.0,
                default.longitude,
                default.radius,
                default.expireTime,
                default.loiterTime,
                default.triggerEnter,
                default.triggerExit
        )
        assertNull(geofenceLocation2)
    }

    @Test
    fun create_ReturnsNull_WhenInvalidLongitude() {
        val geofenceLocation = GeofenceLocation.create(default.name,
                default.latitude,
                -181.0,
                default.radius,
                default.expireTime,
                default.loiterTime,
                default.triggerEnter,
                default.triggerExit
        )
        assertNull(geofenceLocation)

        val geofenceLocation2 = GeofenceLocation.create(default.name,
                default.latitude,
                181.0,
                default.radius,
                default.expireTime,
                default.loiterTime,
                default.triggerEnter,
                default.triggerExit
        )
        assertNull(geofenceLocation2)
    }

    @Test
    fun create_ReturnsNull_WhenInvalidRadius() {
        val geofenceLocation = GeofenceLocation.create(default.name,
                default.latitude,
                default.longitude,
                -1,
                default.expireTime,
                default.loiterTime,
                default.triggerEnter,
                default.triggerExit
        )
        assertNull(geofenceLocation)
    }

    @Test
    fun create_ReturnsNull_WhenInvalidExpireTime() {
        val geofenceLocation = GeofenceLocation.create(default.name,
                default.latitude,
                default.longitude,
                default.radius,
                -2,
                default.loiterTime,
                default.triggerEnter,
                default.triggerExit
        )
        assertNull(geofenceLocation)
    }

    @Test
    fun create_ReturnsNull_WhenInvalidLoiterTime() {
        val geofenceLocation = GeofenceLocation.create(default.name,
                default.latitude,
                default.longitude,
                default.radius,
                default.expireTime,
                -1,
                default.triggerEnter,
                default.triggerExit
        )
        assertNull(geofenceLocation)
    }

    @Test
    fun transitionType_SetsCorrectly() {
        var geofenceLocation = default.copy(triggerEnter = true)
        assertEquals(Geofence.GEOFENCE_TRANSITION_ENTER, geofenceLocation.transitionType)

        geofenceLocation = default.copy(triggerEnter = false, triggerExit = true)
        assertEquals(Geofence.GEOFENCE_TRANSITION_EXIT, geofenceLocation.transitionType)

        geofenceLocation = default.copy(triggerEnter = false, loiterTime = 10)
        assertEquals(Geofence.GEOFENCE_TRANSITION_DWELL, geofenceLocation.transitionType)

        geofenceLocation = default.copy(triggerEnter = true, triggerExit = true)
        assertEquals(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT, geofenceLocation.transitionType)

        geofenceLocation = default.copy(triggerEnter = true, loiterTime = 10)
        assertEquals(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL, geofenceLocation.transitionType)

        geofenceLocation = default.copy(triggerEnter = false, triggerExit = true, loiterTime = 10)
        assertEquals(Geofence.GEOFENCE_TRANSITION_EXIT or Geofence.GEOFENCE_TRANSITION_DWELL, geofenceLocation.transitionType)

        geofenceLocation = default.copy(triggerEnter = true, triggerExit = true, loiterTime = 10)
        assertEquals(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT or Geofence.GEOFENCE_TRANSITION_DWELL, geofenceLocation.transitionType)
    }
}