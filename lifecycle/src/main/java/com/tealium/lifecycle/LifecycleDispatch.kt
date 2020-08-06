package com.tealium.lifecycle

import com.tealium.dispatcher.Dispatch
import java.util.*

data class LifecycleDispatch(var lifecycleName: String) : Dispatch {

    override val id: String = UUID.randomUUID().toString()
    private var mutableMap: MutableMap<String, Any> = mutableMapOf()

    init {
        mutableMap[CoreConstant.TEALIUM_EVENT] = lifecycleName
        mutableMap[CoreConstant.TEALIUM_EVENT_TYPE] = DispatchType.EVENT
    }

    constructor(lifecycleName: String, data: Map<String, Any>? = null) : this(lifecycleName) {
        data?.forEach { (key, value) ->
            mutableMap.getOrPut(key) { value }
        }
    }

    override fun payload(): Map<String, Any> {
        return mutableMap.toMap()
    }

    override fun addAll(data: Map<String, Any>) {
        mutableMap.putAll(data)
    }
}