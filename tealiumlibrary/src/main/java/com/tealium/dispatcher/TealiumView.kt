package com.tealium.dispatcher

import com.tealium.core.DispatchType
import java.util.*

data class TealiumView(var viewName: String) : Dispatch {

    override val id: String = UUID.randomUUID().toString()
    override var timestamp: Long? = System.currentTimeMillis()
    private var mutableMap: MutableMap<String, Any> = mutableMapOf(
        Dispatch.Keys.TEALIUM_EVENT_TYPE to DispatchType.VIEW,
        Dispatch.Keys.TEALIUM_EVENT to viewName,
        Dispatch.Keys.SCREEN_TITLE to viewName,
        Dispatch.Keys.REQUEST_UUID to id
    )

    constructor(viewName: String, data: Map<String, Any>? = null) : this(viewName) {
        data?.forEach { (key, value) ->
            mutableMap.getOrPut(key) { value }
        }
        // backwards compatible; allow overriding of screen_title on constructor
        mutableMap[Dispatch.Keys.SCREEN_TITLE] = data?.get(Dispatch.Keys.SCREEN_TITLE) ?: viewName
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

