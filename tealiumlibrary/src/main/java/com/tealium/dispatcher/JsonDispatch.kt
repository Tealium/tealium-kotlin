package com.tealium.dispatcher

import com.tealium.core.JsonUtils
import com.tealium.core.persistence.PersistentJsonObject

internal class JsonDispatch (private val json: PersistentJsonObject) : Dispatch {

    override val id: String = json.key
    override var timestamp: Long? = json.timestamp

    override fun payload(): Map<String, Any> {
        return JsonUtils.mapFor(json.value)
    }

    override fun addAll(data: Map<String, Any>) {
        data.forEach {(key, value) ->
            json.value.put(key, value)
        }
    }
}