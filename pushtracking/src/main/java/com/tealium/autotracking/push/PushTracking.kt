package com.tealium.autotracking.push

import com.google.firebase.messaging.RemoteMessage
import com.tealium.autotracking.internal.PushNotificationAutoTracker
import com.tealium.core.Module
import com.tealium.core.ModuleFactory
import com.tealium.core.TealiumContext
import com.tealium.autotracking.PushNotificationTracker

class PushTracking(
    private val context: TealiumContext,
    private val pushNotificationTracker: PushNotificationTracker = PushNotificationAutoTracker(context)
) : Module {
    override val name: String = MODULE_NAME
    override var enabled: Boolean = true

    private val pushTrackingEnabled: Boolean = context.config.autoTrackingPushEnabled ?: false

    /**
     * Manually tracks a push notification
     */
    fun trackPushNotification(remoteMessage: RemoteMessage) {
        if (!pushTrackingEnabled) return

        pushNotificationTracker.trackPushNotification(remoteMessage)
    }

    companion object : ModuleFactory {
        private const val MODULE_NAME = "PushTracking"

        override fun create(context: TealiumContext): Module {
            return PushTracking(context)
        }
    }
}