package com.tealium.core

/**
 * Base interface for extensible features within the library.
 */
interface Module {

    /**
     * Unique name identifying the module.
     */
    val name: String

    /**
     * Whether or not this module is enabled. Implementations are not required to handle anything
     * further - e.g. for [Collector] implementations, the [Collector.collect] method will not be
     * called for disabled modules.
     */
    var enabled: Boolean
}