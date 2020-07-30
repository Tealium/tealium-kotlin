package com.tealium.core.persistence

internal fun getTimestamp(): Long {
    return System.currentTimeMillis() / 1000
}

internal fun getTimestampMilliseconds(): Long {
    return System.currentTimeMillis()
}