package com.tealium.adidentifier.internal

import com.tealium.core.messaging.ExternalListener

internal interface AdvertisingInfoUpdatedListener : ExternalListener {
    fun onAdIdInfoUpdated(
        adId: String?,
        isLimitAdTrackingEnabled: Boolean?
    )

    fun onAppSetInfoUpdated(
        appSetId: String?,
        appSetScope: Int?
    )
}