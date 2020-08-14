package com.tealium.dispatcher

import java.util.*
import com.tealium.core.CoreConstant
import com.tealium.core.DispatchType

data class EventDispatch(var eventName: String) : Dispatch {

    override val id: String = UUID.randomUUID().toString()
    private var mutableMap: MutableMap<String, Any> = mutableMapOf()

    init {
        mutableMap[CoreConstant.TEALIUM_EVENT_TYPE] = DispatchType.EVENT
        mutableMap[CoreConstant.TEALIUM_EVENT] = eventName
    }

    constructor(eventName: String, data: Map<String, Any>? = null) : this(eventName) {
        data?.forEach {
            mutableMap[it.key]= it.value
        }
    }

    override fun payload(): Map<String, Any> {
        return mutableMap.toMap()
    }

    override fun addAll(data: Map<String, Any>) {
        mutableMap.putAll(data)
    }
}

