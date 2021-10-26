package com.tealium.autotracking.push

import com.google.firebase.messaging.RemoteMessage

interface PushNotificationTracker {
    fun trackPushNotification(remoteMessage: RemoteMessage)
}