package com.tealium.autotracking.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tealium.core.Tealium

class TealiumFirebaseService: FirebaseMessagingService() {

    override fun onMessageReceived(notification: RemoteMessage) {
        super.onMessageReceived(notification)
        Tealium.names().forEach {
            Tealium[it]?.pushTracking?.trackPushNotification(notification)
        }
    }
}