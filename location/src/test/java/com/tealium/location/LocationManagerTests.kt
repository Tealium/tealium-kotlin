package com.tealium.location

import android.app.Application
import android.os.Build
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class LocationManagerTests {

    @RelaxedMockK
    private lateinit var mockApplication: Application

    @MockK
    private lateinit var context: TealiumContext

    @MockK
    private lateinit var context2: TealiumContext

    private lateinit var config: TealiumConfig

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        config = TealiumConfig(mockApplication, "test", "test", Environment.DEV)

        every { context.config } returns config
        every { context.track(any()) } just Runs
        every { context2.config } returns config
        every { context2.track(any()) } just Runs
    }

    @Test
    fun create_MultipleReturnsSame() {
        val location1 = LocationManager.create(context)
        val location2 = LocationManager.create(context2)

        assertSame(location1, location2)
    }

    @Test
    fun create_MultipleTracksOnAll() {
        LocationManager.create(context)
        LocationManager.create(context2)

        LocationManager.sendGeofenceEvent("fence", GeofenceTransitionType.DWELL)
        verify {
            context.track(
                    withArg {
                        assertEquals("fence", it[GeofenceEventConstants.GEOFENCE_NAME])
                        assertEquals("geofence_dwell", it[GeofenceEventConstants.GEOFENCE_TRANSITION_TYPE])
                    })
            context2.track(
                    withArg {
                        assertEquals("fence", it[GeofenceEventConstants.GEOFENCE_NAME])
                        assertEquals("geofence_dwell", it[GeofenceEventConstants.GEOFENCE_TRANSITION_TYPE])
                    })
        }
    }

    @Test
    fun updates_ChangeOnBoth() {
        val location1 = LocationManager.create(context) as LocationManager
        val location2 = LocationManager.create(context2) as LocationManager

        location1.addGeofence("test", 1.0, 1.0, 10, 0, 10, true, true)
        assertNotNull(location1.allGeofenceNames())
        assertEquals("test", location1.allGeofenceNames()!!.get(0))
        assertNotNull(location2.allGeofenceNames())
        assertEquals("test", location2.allGeofenceNames()!!.get(0))
    }
}