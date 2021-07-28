package com.tealium.core.collection

import com.tealium.core.persistence.DataLayer
import com.tealium.core.persistence.Expiry

fun DataLayer.getOrPutString(key: String, fallback: () -> String, expiry: Expiry): String {
    return getString(key) ?: fallback().also { putString(key, it, expiry) }
}