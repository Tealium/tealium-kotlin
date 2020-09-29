package com.tealium.dispatcher

internal class GenericDispatch(dispatch: Dispatch) : Dispatch {

    override val id: String = dispatch.id
    override var timestamp: Long? = dispatch.timestamp ?: System.currentTimeMillis()

    private val payload = shallowCopy(dispatch.payload())

    override fun payload(): Map<String, Any> {
        return payload.toMap()
    }

    override fun addAll(data: Map<String, Any>) {
        payload.putAll(data)
    }

    companion object Utils {

        /**
         * Performs a shallow copy on the [map] parameter and returns a new [MutableMap] containing
         * copies of the first level of data from the original [map]
         */
        fun shallowCopy(map: Map<String, Any>): MutableMap<String, Any> {
            val newMap = map.toMutableMap()
            newMap.forEach {
                val value = it.value
                when (value) {
                    is Collection<*> -> newMap[it.key] = value.toMutableList()
                    is Map<*, *> -> newMap[it.key] = value.toMutableMap()
                    is Array<*> -> newMap[it.key] = value.copyOf()
                }
            }
            return newMap
        }
    }
}