package com.tealium.adidentifier.internal

import com.tealium.core.messaging.ExternalMessenger

internal class AppSetInfoUpdatedMessenger(
    val appSetId: String?,
    val appSetScope: Int?
) : ExternalMessenger<AdvertisingInfoUpdatedListener>(AdvertisingInfoUpdatedListener::class) {
    override fun deliver(listener: AdvertisingInfoUpdatedListener) {
        listener.onAppSetInfoUpdated(appSetId, appSetScope)
    }
}