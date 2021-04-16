package com.tealium.collectdispatcher

import com.tealium.core.*
import com.tealium.core.messaging.*
import com.tealium.core.network.HttpClient
import com.tealium.core.network.NetworkClient
import com.tealium.core.network.NetworkClientListener
import com.tealium.dispatcher.BatchDispatch
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.Dispatcher
import com.tealium.dispatcher.DispatcherListener
import org.json.JSONObject
import javax.net.ssl.HttpsURLConnection

interface CollectDispatcherListener : DispatcherListener {
}

class CollectDispatcher(private val config: TealiumConfig,
                        private val encoder: Encoder = TealiumEncoder,
                        private val client: NetworkClient = HttpClient(config),
                        private val collectDispatchListener: CollectDispatcherListener? = null) :
        Dispatcher {

    val eventUrl: String
        get() = config.overrideCollectUrl ?: config.overrideCollectDomain?.let {
            "https://$it/event"
        } ?: COLLECT_URL

    val batchEventUrl: String
        get() = config.overrideCollectBatchUrl ?: config.overrideCollectDomain?.let {
            "https://$it/bulk-event"
        } ?: BULK_URL

    val profileOverride: String? = config.overrideCollectProfile

    init {
        client.networkClientListener = object : NetworkClientListener {

            override fun onNetworkResponse(status: Int, response: String) {
                Logger.dev(BuildConfig.TAG, "status code: $status, message: $response")
                if (status == HttpsURLConnection.HTTP_OK) {
                    collectDispatchListener?.successfulTrack()
                } else {
                    collectDispatchListener?.unsuccessfulTrack("Network error, response: $response")
                }
            }

            override fun onNetworkError(message: String) {
                Logger.dev(BuildConfig.TAG, message)
            }
        }
    }

    override suspend fun onDispatchSend(dispatch: Dispatch) {
        if (profileOverride != null) {
            dispatch.addAll(
                    mapOf(TEALIUM_PROFILE to profileOverride)
            )
        }
        Logger.dev(BuildConfig.TAG, "Sending dispatch: ${dispatch.payload()}")
        client.post(JSONObject(dispatch.payload()).toString(), eventUrl, false)
    }

    override suspend fun onBatchDispatchSend(dispatches: List<Dispatch>) {
        Logger.dev(BuildConfig.TAG, "Sending ${dispatches.count()} dispatches")

        val batchDispatch = BatchDispatch.create(dispatches)
        batchDispatch?.let {
            if (profileOverride != null) {
                batchDispatch.shared[TEALIUM_PROFILE] = profileOverride
            }
            client.post(JSONObject(batchDispatch.payload()).toString(), batchEventUrl, true)
        }
    }

    companion object : DispatcherFactory {
        const val COLLECT_URL = "https://collect.tealiumiq.com/event"
        const val BULK_URL = "https://collect.tealiumiq.com/bulk-event"

        override fun create(context: TealiumContext,
                            callbacks: AfterDispatchSendCallbacks): Dispatcher {

            return CollectDispatcher(context.config)
        }
    }

    override val name = "COLLECT_DISPATCHER"
    override var enabled: Boolean = true
}

val Dispatchers.Collect: DispatcherFactory
    get() = CollectDispatcher
