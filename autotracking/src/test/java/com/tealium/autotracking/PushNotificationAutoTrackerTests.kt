package com.tealium.autotracking

import com.google.firebase.messaging.RemoteMessage
import com.tealium.autotracking.internal.PushNotificationAutoTracker
import com.tealium.core.TealiumContext
import com.tealium.dispatcher.TealiumEvent
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class PushNotificationAutoTrackerTests {

    @RelaxedMockK
    lateinit var mockContext: TealiumContext

    @RelaxedMockK
    lateinit var mockMessage: RemoteMessage

    @RelaxedMockK
    lateinit var mockNotification: RemoteMessage.Notification

    lateinit var pushTracker: PushNotificationTracker

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        pushTracker = PushNotificationAutoTracker(mockContext)

        every { mockMessage.notification } returns mockNotification
    }

    @Test
    fun trackPushNotification_TracksEvenWithoutData() {
        every { mockMessage.notification } returns null
        every { mockMessage.messageType } returns null
        every { mockMessage.data } returns emptyMap()

        pushTracker.trackPushNotification(mockMessage)

        verify {
            mockContext.track(match {
                it is TealiumEvent
                        && it.eventName == "push_notification_received"
                        && it.payload().filterKeys { key -> key.startsWith("push_notification_") }.isEmpty()
            })
        }
    }

    @Test
    fun trackPushNotification_AddsNotificationInfo() {
        every { mockMessage.notification } returns mockNotification
        every { mockNotification.title } returns "title"
        every { mockNotification.body } returns "body"
        every { mockNotification.channelId } returns "channel"
        every { mockMessage.messageType } returns "message_type"
        every { mockMessage.data } returns emptyMap()

        pushTracker.trackPushNotification(mockMessage)

        verify {
            mockContext.track(match {
                it is TealiumEvent
                        && it.eventName == "push_notification_received"
                        && it.payload()["push_notification_title"] == "title"
                        && it.payload()["push_notification_body"] == "body"
                        && it.payload()["push_notification_category"] == "channel"
                        && it.payload()["push_notification_type"] == "message_type"
            })
        }
    }

    @Test
    fun trackPushNotification_AddsCustomData() {
        every { mockMessage.notification } returns null
        every { mockMessage.data } returns mapOf(
                "key_1" to "value_1",
                "key_2" to "value_2"
        )

        pushTracker.trackPushNotification(mockMessage)

        verify {
            mockContext.track(match {
                it is TealiumEvent
                        && it.eventName == "push_notification_received"
                        && it.payload()["push_notification_data_key_1"] == "value_1"
                        && it.payload()["push_notification_data_key_2"] == "value_2"
            })
        }
    }
}