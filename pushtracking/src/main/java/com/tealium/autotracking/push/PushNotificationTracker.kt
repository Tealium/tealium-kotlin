package com.tealium.autotracking

import com.google.firebase.messaging.RemoteMessage

interface PushNotificationTracker {
    fun trackPushNotification(remoteMessage: RemoteMessage)
}