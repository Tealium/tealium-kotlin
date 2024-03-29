package com.tealium.autotracking

import android.app.Activity
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.messaging.MessengerService
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Assert.assertNotSame
import org.junit.Before
import org.junit.Test

class AutoTrackingModuleTests {

    @RelaxedMockK
    lateinit var mockContext: TealiumContext

    @RelaxedMockK
    lateinit var mockConfig: TealiumConfig

    @RelaxedMockK
    lateinit var mockEvents: MessengerService

    @RelaxedMockK
    lateinit var mockActivityTracker: ActivityTracker

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        mockkStatic("com.tealium.autotracking.TealiumConfigAutoTrackingKt")
        every { mockContext.config } returns mockConfig
        every { mockContext.events } returns mockEvents
        every { mockConfig.autoTrackingMode } returns AutoTrackingMode.FULL
        every { mockConfig.autoTrackingCollectorDelegate } returns null
    }

    @Test
    fun init_CreatesNewInstance() {
        val autoTracking1 = AutoTracking.create(mockContext)
        val autoTracking2 = AutoTracking.create(mockContext)

        assertNotSame(autoTracking1, autoTracking2)
    }

    @Test
    fun init_RegistersActivityTrackerForEvents() {
        val autoTracking = AutoTracking(mockContext, mockActivityTracker)

        verify {
            mockEvents.subscribe(mockActivityTracker)
        }
    }

    @Test
    fun trackActivity_DelegatesToActivityTracker() {
        val autoTracking = AutoTracking(mockContext, mockActivityTracker)
        val mockActivity: Activity = mockk()
        val mockActivityDataCollector: ActivityDataCollector = mockk()
        val mockMap: Map<String, Any> = mockk()
        autoTracking.trackActivity(mockActivity)
        autoTracking.trackActivity(mockActivity, null)
        autoTracking.trackActivity(mockActivity, mockMap)
        autoTracking.trackActivity(mockActivityDataCollector)
        autoTracking.trackActivity(mockActivityDataCollector, null)
        autoTracking.trackActivity(mockActivityDataCollector, mockMap)

        verify {
            mockActivityTracker.trackActivity(mockActivity, null)
            mockActivityTracker.trackActivity(mockActivity, null)
            mockActivityTracker.trackActivity(mockActivity, mockMap)
            mockActivityTracker.trackActivity(mockActivityDataCollector, null)
            mockActivityTracker.trackActivity(mockActivityDataCollector, null)
            mockActivityTracker.trackActivity(mockActivityDataCollector, mockMap)
        }
    }
}