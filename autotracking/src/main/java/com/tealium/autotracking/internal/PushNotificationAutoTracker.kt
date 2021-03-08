package com.tealium.autotracking.internal

import com.google.firebase.messaging.RemoteMessage
import com.tealium.autotracking.PushNotificationTracker
import com.tealium.core.TealiumContext
import com.tealium.dispatcher.TealiumEvent

class PushNotificationAutoTracker(private val context: TealiumContext) : PushNotificationTracker {

    override fun trackPushNotification(remoteMessage: RemoteMessage) {
        val eventData = mutableMapOf<String, Any>()

        remoteMessage.notification?.let { notification ->
            notification.title?.let {
                eventData.put(PUSH_NOTIFICATION_TITLE, it)
            }
            notification.body?.let {
                eventData.put(PUSH_NOTIFICATION_BODY, it)
            }
            notification.channelId?.let {
                eventData.put(PUSH_NOTIFICATION_CATEGORY, it)
            }
        }

        remoteMessage.messageType?.let {
            eventData.put(PUSH_NOTIFICATION_TYPE, it)
        }

        remoteMessage.data.mapKeys { e -> "${PUSH_NOTIFICATION_PREFIX}_data_${e.key}" }.let {
            eventData.putAll(it)
        }

        context.track(TealiumEvent(PUSH_NOTIFICATION_EVENT_NAME, eventData))
    }

    private companion object {
        private const val PUSH_NOTIFICATION_PREFIX: String = "push_notification"
        private const val PUSH_NOTIFICATION_EVENT_NAME: String = "${PUSH_NOTIFICATION_PREFIX}_received"
        private const val PUSH_NOTIFICATION_TITLE: String = "${PUSH_NOTIFICATION_PREFIX}_title"
        private const val PUSH_NOTIFICATION_BODY: String = "${PUSH_NOTIFICATION_PREFIX}_body"
        private const val PUSH_NOTIFICATION_CATEGORY: String = "${PUSH_NOTIFICATION_PREFIX}_category"
        private const val PUSH_NOTIFICATION_TYPE: String = "${PUSH_NOTIFICATION_PREFIX}_type"
    }
}