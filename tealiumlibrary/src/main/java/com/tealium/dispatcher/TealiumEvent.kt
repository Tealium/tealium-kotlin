package com.tealium.dispatcher

import com.tealium.core.DispatchType
import java.util.*

data class TealiumEvent(var eventName: String) : Dispatch {

    override val id: String = UUID.randomUUID().toString()
    override var timestamp: Long? = System.currentTimeMillis()
    private var mutableMap: MutableMap<String, Any> = mutableMapOf(
        Dispatch.Keys.TEALIUM_EVENT_TYPE to DispatchType.EVENT,
        Dispatch.Keys.TEALIUM_EVENT to eventName,
        Dispatch.Keys.REQUEST_UUID to id
    )

    constructor(eventName: String, data: Map<String, Any>? = null) : this(eventName) {
        data?.forEach {
            mutableMap[it.key] = it.value
        }
    }

    override fun payload(): Map<String, Any> {
        return mutableMap.toMap()
    }

    override fun addAll(data: Map<String, Any>) {
        mutableMap.putAll(data)
    }

    override fun remove(key: String) {
        mutableMap.remove(key)
    }
}

