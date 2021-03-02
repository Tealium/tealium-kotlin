package com.tealium.autotracking

import android.app.Activity
import com.google.firebase.messaging.RemoteMessage
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.messaging.MessengerService
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
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

    @RelaxedMockK
    lateinit var mockPushTracker: PushNotificationTracker

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

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
        val autoTracking = AutoTracking(mockContext, mockActivityTracker, mockPushTracker)

        verify {
            mockEvents.subscribe(mockActivityTracker)
        }
    }

    @Test
    fun trackActivity_DelegatesToActivityTracker() {
        val autoTracking = AutoTracking(mockContext, mockActivityTracker, mockPushTracker)
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

    @Test
    fun trackPushNotification_TracksWhenEnabled() {
        every { mockConfig.autoTrackingPushEnabled } returns true
        val autotracking = AutoTracking(mockContext, mockActivityTracker, mockPushTracker)

        val mockMessage: RemoteMessage = mockk(relaxed = true)
        autotracking.trackPushNotification(mockMessage)

        verify {
            mockPushTracker.trackPushNotification(mockMessage)
        }
    }

    @Test
    fun trackPushNotification_DoesNotTrackWhenDisabled() {
        every { mockConfig.autoTrackingPushEnabled } returns false
        val autotracking = AutoTracking(mockContext, mockActivityTracker, mockPushTracker)

        val mockMessage: RemoteMessage = mockk(relaxed = true)
        autotracking.trackPushNotification(mockMessage)

        verify(exactly = 0) {
            mockPushTracker.trackPushNotification(mockMessage)
        }
    }

    @Test
    fun trackPushNotification_DoesNotTrackWhenOmitted() {
        every { mockConfig.autoTrackingPushEnabled } returns null
        val autotracking = AutoTracking(mockContext, mockActivityTracker, mockPushTracker)

        val mockMessage: RemoteMessage = mockk(relaxed = true)
        autotracking.trackPushNotification(mockMessage)

        verify(exactly = 0) {
            mockPushTracker.trackPushNotification(mockMessage)
        }
    }
}