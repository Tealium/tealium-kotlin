package com.tealium.dispatcher

import com.tealium.core.JsonUtils
import com.tealium.core.persistence.PersistentItem
import com.tealium.core.persistence.Serdes
import org.json.JSONObject

internal class JsonDispatch (json: PersistentItem) : Dispatch {

    override val id: String = json.key
    override var timestamp: Long? = json.timestamp
    private val json: JSONObject = Serdes.jsonObjectSerde().deserializer.deserialize(json.value)

    override fun payload(): Map<String, Any> {
        return JsonUtils.mapFor(json)
    }

    override fun addAll(data: Map<String, Any>) {
        data.forEach {(key, value) ->
            json.put(key, value)
        }
    }

    override fun remove(key: String) {
        json.remove(key)
    }
}