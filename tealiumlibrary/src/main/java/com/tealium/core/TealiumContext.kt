package com.tealium.core

import com.tealium.core.messaging.Subscribable
import com.tealium.core.persistence.DataLayer
import com.tealium.core.network.NetworkClient
import com.tealium.dispatcher.Dispatch

/**
 * Context object passed to each [Module] during creation.
 */
data class TealiumContext(val config: TealiumConfig,
                          val visitorId: String,
                          val log: Logging,
                          val dataLayer: DataLayer,
                          val httpClient: NetworkClient,
                          val events: Subscribable,
                          private val tealium: Tealium) {

    /**
     * Can be used by modules outside of the core to send tracking requests on this Tealium instance.
     */
    fun track(dispatch: Dispatch) = tealium.track(dispatch)
}