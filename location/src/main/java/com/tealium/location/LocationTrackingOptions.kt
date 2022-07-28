package com.tealium.location

data class LocationTrackingOptions(
    val accuracy: LocationTrackingAccuracy = LocationTrackingAccuracy.BalancedAccuracy,
    val minTime: Long = 5000L,
    val minDistance: Float = 0.0f
)