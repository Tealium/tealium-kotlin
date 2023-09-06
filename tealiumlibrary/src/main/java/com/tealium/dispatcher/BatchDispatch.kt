package com.tealium.dispatcher

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
                        event.remove(key)
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
            Dispatch.Keys.TEALIUM_ACCOUNT,
            Dispatch.Keys.TEALIUM_PROFILE,
            Dispatch.Keys.TEALIUM_ENVIRONMENT,
            Dispatch.Keys.TEALIUM_DATASOURCE_ID,
            Dispatch.Keys.TEALIUM_VISITOR_ID,
            Dispatch.Keys.DEVICE,
            Dispatch.Keys.DEVICE_ARCHITECTURE,
            Dispatch.Keys.DEVICE_RESOLUTION
        )

        @JvmStatic
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