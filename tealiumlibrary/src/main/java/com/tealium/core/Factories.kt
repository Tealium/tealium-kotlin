package com.tealium.core

import com.tealium.core.messaging.AfterDispatchSendCallbacks
import com.tealium.dispatcher.Dispatcher

/**
 * Factory class required to produce a [Collector]
 */
interface CollectorFactory {

    /**
     * Returns an instance of a [Collector]
     */
    fun create(context: TealiumContext): Collector
}

/**
 * Factory class required to produce a [Dispatcher]
 */
interface DispatcherFactory {

    /**
     * Returns an instance of a [Dispatcher]
     */
    fun create(context: TealiumContext,
               callbacks: AfterDispatchSendCallbacks): Dispatcher
}

/**
 * Factory class required to produce a [Module]
 */
interface ModuleFactory {

    /**
     * Returns an instance of a [Module]
     */
    fun create(context: TealiumContext): Module
}