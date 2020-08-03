package com.tealium.dispatcher

import com.tealium.core.CoreConstant
import com.tealium.core.DispatchType
import java.util.*

data class ViewDispatch(var viewName: String) : Dispatch {

    override val id: String = UUID.randomUUID().toString()
    override var timestamp: Long? = System.currentTimeMillis()
    private var mutableMap: MutableMap<String, Any> = mutableMapOf()

    init {
        mutableMap[CoreConstant.SCREEN_TITLE] = viewName
        mutableMap[CoreConstant.TEALIUM_EVENT_TYPE] = DispatchType.VIEW
        mutableMap[CoreConstant.TEALIUM_EVENT] = viewName
    }

    constructor(viewName: String, data: Map<String, Any>? = null) : this(viewName) {
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

