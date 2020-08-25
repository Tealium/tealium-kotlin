package com.tealium.core

import com.tealium.dispatcher.Dispatch

/**
 * A Transformer is responsible for updating data currently stored in the Dispatch.
 */
interface Transformer : Module {

    /**
     *  Called for each new [Dispatch] sent through [Tealium.track]. This phase occurs after each
     *  [Collector] has populated the Dispatch.
     *
     *  @param dispatch The current dispatch being transformed
     */
    suspend fun transform(dispatch: Dispatch)
}