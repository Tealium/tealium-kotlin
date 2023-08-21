package com.tealium.adidentifier.internal

import com.tealium.core.messaging.ExternalMessenger

internal class AdIdInfoUpdatedMessenger(
    val adId: String?,
    val isLimitAdTrackingEnabled: Boolean?
) : ExternalMessenger<AdvertisingInfoUpdatedListener>(AdvertisingInfoUpdatedListener::class) {
    override fun deliver(listener: AdvertisingInfoUpdatedListener) {
        listener.onAdIdInfoUpdated(adId, isLimitAdTrackingEnabled)
    }
}