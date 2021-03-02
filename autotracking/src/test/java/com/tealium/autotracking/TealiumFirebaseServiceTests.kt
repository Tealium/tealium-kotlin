package com.tealium.autotracking

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tealium.core.Tealium
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Before
import org.junit.Test

class TealiumFirebaseServiceTests {

    @RelaxedMockK
    lateinit var mockMessage: RemoteMessage

    @RelaxedMockK
    lateinit var mockAutoTracking: AutoTracking

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
        every { mockTealium.autoTracking } returns mockAutoTracking

        tealiumFirebaseService.onMessageReceived(mockMessage)

        verify(exactly = 2) {
            mockAutoTracking.trackPushNotification(mockMessage)
        }
    }

    @Test
    fun onMessageReceived_CallsOnlyAutotrackingInstances() {
        every { Tealium.names() } returns setOf("instance_1", "instance_2")
        every { Tealium[any()] } returns mockTealium
        every { mockTealium.autoTracking } returnsMany listOf(mockAutoTracking, null)

        tealiumFirebaseService.onMessageReceived(mockMessage)

        verify(exactly = 1) {
            mockAutoTracking.trackPushNotification(mockMessage)
        }
    }
}