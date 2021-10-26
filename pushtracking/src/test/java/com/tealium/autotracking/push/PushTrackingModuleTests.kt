package com.tealium.autotracking.push

import com.google.firebase.messaging.RemoteMessage
import com.tealium.autotracking.push.PushNotificationTracker
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.messaging.MessengerService
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class PushTrackingModuleTests {

    @RelaxedMockK
    lateinit var mockContext: TealiumContext

    @RelaxedMockK
    lateinit var mockConfig: TealiumConfig

    @RelaxedMockK
    lateinit var mockEvents: MessengerService

    @RelaxedMockK
    lateinit var mockPushTracker: PushNotificationTracker

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { mockContext.config } returns mockConfig
        every { mockContext.events } returns mockEvents
    }

    @Test
    fun trackPushNotification_TracksWhenEnabled() {
        every { mockConfig.autoTrackingPushEnabled } returns true
        val pushtracking = PushTracking(mockContext, mockPushTracker)

        val mockMessage: RemoteMessage = mockk(relaxed = true)
        pushtracking.trackPushNotification(mockMessage)

        verify {
            mockPushTracker.trackPushNotification(mockMessage)
        }
    }

    @Test
    fun trackPushNotification_DoesNotTrackWhenDisabled() {
        every { mockConfig.autoTrackingPushEnabled } returns false
        val pushtracking = PushTracking(mockContext, mockPushTracker)

        val mockMessage: RemoteMessage = mockk(relaxed = true)
        pushtracking.trackPushNotification(mockMessage)

        verify(exactly = 0) {
            mockPushTracker.trackPushNotification(mockMessage)
        }
    }

    @Test
    fun trackPushNotification_DoesNotTrackWhenOmitted() {
        every { mockConfig.autoTrackingPushEnabled } returns null
        val pushtracking = PushTracking(mockContext, mockPushTracker)

        val mockMessage: RemoteMessage = mockk(relaxed = true)
        pushtracking.trackPushNotification(mockMessage)

        verify(exactly = 0) {
            mockPushTracker.trackPushNotification(mockMessage)
        }
    }

}