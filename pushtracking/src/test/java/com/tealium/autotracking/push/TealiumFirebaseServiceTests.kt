package com.tealium.autotracking

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tealium.autotracking.push.PushTracking
import com.tealium.autotracking.push.pushTracking
import com.tealium.core.Tealium
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Before
import org.junit.Test

class TealiumFirebaseServiceTests {

    @RelaxedMockK
    lateinit var mockMessage: RemoteMessage

    @RelaxedMockK
    lateinit var mockPushTracking: PushTracking

    @RelaxedMockK
    lateinit var mockTealium: Tealium

    lateinit var tealiumFirebaseService: FirebaseMessagingService

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        mockkObject(Tealium)
        tealiumFirebaseService = TealiumFirebaseService()
    }

    @Test
    fun onMessageReceived_CallsAllInstances() {
        every { Tealium.names() } returns setOf("instance_1", "instance_2")
        every { Tealium[any()] } returns mockTealium
        every { mockTealium.pushTracking } returns mockPushTracking

        tealiumFirebaseService.onMessageReceived(mockMessage)

        verify(exactly = 2) {
            mockPushTracking.trackPushNotification(mockMessage)
        }
    }

    @Test
    fun onMessageReceived_CallsOnlyAutotrackingInstances() {
        every { Tealium.names() } returns setOf("instance_1", "instance_2")
        every { Tealium[any()] } returns mockTealium
        every { mockTealium.pushTracking } returnsMany listOf(mockPushTracking, null)

        tealiumFirebaseService.onMessageReceived(mockMessage)

        verify(exactly = 1) {
            mockPushTracking.trackPushNotification(mockMessage)
        }
    }
}