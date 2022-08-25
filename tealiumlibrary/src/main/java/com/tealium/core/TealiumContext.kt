package com.tealium.core

import android.os.Looper
import com.tealium.core.messaging.MessengerService
import com.tealium.core.persistence.DataLayer
import com.tealium.core.network.NetworkClient
import com.tealium.dispatcher.Dispatch
import com.tealium.test.OpenForTesting
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/**
 * Context object passed to each [Module] during creation.
 */
@OpenForTesting
data class TealiumContext(val config: TealiumConfig,
                          private val _visitorId: String,
                          val log: Logging,
                          val dataLayer: DataLayer,
                          val httpClient: NetworkClient,
                          val events: MessengerService,
                          val tealium: Tealium,
                          val executors: TealiumExecutors
                          ) {

    val visitorId: String
        get() = tealium.visitorId

    /**
     * Can be used by modules outside of the core to send tracking requests on this Tealium instance.
     */
    fun track(dispatch: Dispatch) = tealium.track(dispatch)
}

interface TealiumExecutors {
    val main: Looper
    val background: CoroutineScope
    val io: CoroutineScope
}

class DefaultTealiumExecutors(
    override val main: Looper = Looper.getMainLooper(),
    override val background: CoroutineScope,
    override val io: CoroutineScope
) : TealiumExecutors