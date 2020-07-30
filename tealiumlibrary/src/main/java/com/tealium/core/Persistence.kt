package com.tealium.core

import com.tealium.dispatcher.Dispatch

interface Persistence {

    var count: Int

    fun enqueue(dispatch: Dispatch)

    fun enqueue(dispatches: List<Dispatch>)

    fun dequeue(): List<Dispatch>

    fun putString(key: String, value: String)

    fun getString(key: String): String
}

class InMemoryPersistence: Persistence {

    private var dispatches = mutableListOf<Dispatch>()
    private var stringMap = mutableMapOf<String, String>()

    override var count: Int = 0
        get() = dispatches.count()

    override fun enqueue(dispatch: Dispatch) {
        dispatches.add(dispatch)
    }

    override fun enqueue(dispatches: List<Dispatch>) {
        this.dispatches.addAll(dispatches)
    }

    override fun dequeue(): List<Dispatch> {
        val temp = dispatches.toList()
        dispatches.clear()
        return temp
    }

    override fun putString(key: String, value: String) {
        stringMap[key] = value
    }

    override fun getString(key: String): String {
        return stringMap.getValue(key)
    }
}

