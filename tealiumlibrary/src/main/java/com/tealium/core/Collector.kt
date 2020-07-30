package com.tealium.core

/**
 * A Collector is responsible for gathering data and placing it in a Map of key value pairs. This
 * data will eventually be merged into each dispatch.
 */
interface Collector : Module {

    /**
     *  Called for each new [Dispatch] sent through [Tealium.track] with each key-value pair being
     *  merged into the [Dispatch]
     *
     *  @return Map of relevant key-value pairs
     */
    suspend fun collect(): Map<String, Any>
}