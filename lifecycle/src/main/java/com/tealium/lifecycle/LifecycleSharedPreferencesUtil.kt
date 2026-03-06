package com.tealium.lifecycle

import android.content.SharedPreferences

fun SharedPreferences.getNullableLong(key: String): Long? {
    return if (contains(key)) {
        getLong(key, 0L)
    } else {
        null
    }
}