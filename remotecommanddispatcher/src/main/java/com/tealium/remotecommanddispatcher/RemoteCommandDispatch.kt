package com.tealium.remotecommanddispatcher

import com.tealium.core.CoreConstant
import com.tealium.core.DispatchType
import com.tealium.dispatcher.Dispatch
import java.util.*

data class RemoteCommandDispatch(var remoteCommandName: String) : Dispatch {

    override val id: String = UUID.randomUUID().toString()
    private var mutableMap: MutableMap<String, Any> = mutableMapOf()

    init {
        mutableMap[CoreConstant.TEALIUM_EVENT] = remoteCommandName
        mutableMap[CoreConstant.TEALIUM_EVENT_TYPE] = DispatchType.EVENT
        mutableMap[CoreConstant.TEALIUM_EVENT_NAME] = remoteCommandName
    }

    constructor(remoteCommandName: String, data: Map<String, Any>? = null) : this(remoteCommandName) {
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