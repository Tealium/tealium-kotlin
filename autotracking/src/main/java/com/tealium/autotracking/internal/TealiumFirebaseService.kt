package com.tealium.autotracking.internal

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tealium.core.Tealium
import com.tealium.dispatcher.TealiumEvent

class TealiumFirebaseService: FirebaseMessagingService() {

    override fun onMessageReceived(p0: RemoteMessage) {
        super.onMessageReceived(p0)
        Tealium.names().forEach {
//            Tealium[it]?.track(TealiumEvent())
        }

    }
}