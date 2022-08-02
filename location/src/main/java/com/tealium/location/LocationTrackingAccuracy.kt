package com.tealium.location

import com.google.android.gms.location.LocationRequest

/**
 * Location accuracy options
 */
enum class LocationTrackingAccuracy {
    HighAccuracy,
    BalancedAccuracy,
    LowPower,
    NoPower
}

/**
 * Convert our own tracking accuracy enum to relevant Int value from LocationRequest
 * in case they ever get updated.
 */
internal val LocationTrackingAccuracy.requestPriority: Int
    get() {
        return when (this) {
            LocationTrackingAccuracy.HighAccuracy -> LocationRequest.PRIORITY_HIGH_ACCURACY
            LocationTrackingAccuracy.BalancedAccuracy -> LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            LocationTrackingAccuracy.LowPower -> LocationRequest.PRIORITY_LOW_POWER
            LocationTrackingAccuracy.NoPower -> LocationRequest.PRIORITY_NO_POWER
        }
    }