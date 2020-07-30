package com.tealium.dispatcher

import DeviceCollectorConstants.DEVICE
import DeviceCollectorConstants.DEVICE_ARCHITECTURE
import DeviceCollectorConstants.DEVICE_RESOLUTION
import TealiumCollectorConstants.TEALIUM_DATASOURCE_ID
import TealiumCollectorConstants.TEALIUM_ACCOUNT
import TealiumCollectorConstants.TEALIUM_ENVIRONMENT
import TealiumCollectorConstants.TEALIUM_PROFILE
import TealiumCollectorConstants.TEALIUM_VISITOR_ID
import com.tealium.core.Logger
import com.tealium.tealiumlibrary.BuildConfig

class BatchDispatch private constructor(dispatchList: List<Dispatch>) {

    val shared = mutableMapOf<String, Any>()
    val events: List<MutableMap<String, Any>>
    private val payload = mutableMapOf<String, Any>()

    init {
        events = dispatchList.map { dispatch -> dispatch.payload().toMutableMap() }
        compressKnownKeys()
    }

    private fun compressKnownKeys() {
        KNOWN_SHARED_KEYS.forEach { key ->
            var keyFound = false
            events.forEach { event ->
                if (!keyFound) {
                    event[key]?.let {
                        shared[key] = it
                        keyFound = true
                    }
                } else {
                    event.remove(key)
                }
            }
        }
    }

    fun payload(): Map<String, Any> {
        payload[KEY_SHARED] = shared
        payload[KEY_EVENTS] = events
        return payload
    }

    companion object {

        const val KEY_SHARED = "shared"
        const val KEY_EVENTS = "events"
        //TODO: other known keys that could be collapsed, but currently aren't in the library
        // DEVICE_CPUTYPE,
        // DEVICE_LANGUAGE,
        // TEALIUM_VID,
        // TEALIUM_LIBRARY_NAME,
        // UUID
        val KNOWN_SHARED_KEYS = listOf(
                TEALIUM_ACCOUNT,
                TEALIUM_PROFILE,
                TEALIUM_ENVIRONMENT,
                TEALIUM_DATASOURCE_ID,
                TEALIUM_VISITOR_ID,
                DEVICE,
                DEVICE_ARCHITECTURE,
                DEVICE_RESOLUTION)

        fun create(dispatchList: List<Dispatch>): BatchDispatch? {
            //TODO: consider filtering dispatches with other conditions, like missing required data
            if (dispatchList.isNotEmpty()) {
                return BatchDispatch(dispatchList)
            }
            Logger.dev(BuildConfig.TAG, "Batch was not created; dispatchList was empty.")
            return null
        }
    }
}