package com.tealium.hosteddatalayer

import com.tealium.core.*
import com.tealium.core.CoreConstant.TEALIUM_EVENT
import com.tealium.core.network.*
import com.tealium.core.validation.DispatchValidator
import com.tealium.dispatcher.Dispatch
import org.json.JSONObject

class HostedDataLayer(private val config: TealiumConfig,
                      private val dataLayerStorage: DataLayerStore = DataLayerStore(config),
                      connectivity: Connectivity = ConnectivityRetriever(config.application))
    : Module,
        Transformer
//        DispatchValidator
{

    override val name: String
        get() = moduleName
    override var enabled: Boolean = true

    private val httpClient: NetworkClient = HttpClient(config, connectivity = connectivity)

    /**
     * Event Mapping should contain key-value pairs, where the key is equal to the value found in
     * "tealium_event" within the [Dispatch]. The Value should be the key within the [Dispatch] to
     * look in for the DataLayer Id.
     */
    private val eventMappings: Map<String, String> = config.hostedDataLayerEventMappings ?: emptyMap()

    /**
     * Base URL for Tealium's Hosted DataLayer CDN location
     */
    private val hostedDataLayerBaseUrl = "https://tags.tiqcdn.com/dle/${config.accountName}/${config.profileName}/"

    /**
     * Keeps track of any failed Data Layer Id's so they are not unnecessarily making requests for
     * items known to have failed in this application launch.
     */
    private val failedDataLayerIds = mutableListOf<String>()

    /**
     * Looks in the cache for a stored copy of the Data Layer - if found then its data will be
     * merged into the Dispatch. If not found in the cache, then it will fetch the latest version
     * from the Hosted Data Layer CDN.
     */
    override suspend fun transform(dispatch: Dispatch) {
        val eventName = dispatch[TEALIUM_EVENT]
        eventMappings[eventName]?.let { key ->
            // Found event mapping.
            dispatch[key]?.toString()?.also { id ->
                // Found data layer id in dispatch.
                if (dataLayerStorage.contains(id)) {
                    // data is cached
                    dataLayerStorage.get(id)?.let {
                        merge(dispatch, it)
                    }
                } else {
                    // data is not cached - fetch
                    if (!failedDataLayerIds.contains(id)) {
                        val dataLayer = fetch(id)
                        if (dataLayer != null) {
                            merge(dispatch, dataLayer)
                            dataLayerStorage.insert(dataLayer)
                        } else {
                            failedDataLayerIds.add(id)
                        }
                    }
                }
            }
        }
    }

    /**
     * Fetches the Data Layer form the CDN and returns it if it has been found.
     */
    private suspend fun fetch(id: String): HostedDataLayerEntry?  {
        val retriever = ResourceRetriever(config, createUrlForResource(id), httpClient).apply {
            maxRetries = 3
            useIfModifed = false
        }

        return retriever.fetch()?.let {
            val json = JSONObject(it)
            HostedDataLayerEntry(id, System.currentTimeMillis(), json)
        }
    }

    /**
     * Generates the URL for a specific Data Layer Id based on the [hostedDataLayerBaseUrl]
     */
    private fun createUrlForResource(id: String): String {
        return "${hostedDataLayerBaseUrl}$id.json"
    }

    /**
     * Merges the data into the
     */
    private fun merge(dispatch: Dispatch, dataLayerEntry: HostedDataLayerEntry) {

    }


    //TODO: DispatchValidator to queue to try and ensure sequential event sending
//    override fun shouldQueue(dispatch: Dispatch?): Boolean {
//        return false
//    }
//
//    override fun shouldDrop(dispatch: Dispatch): Boolean {
//        return false
//    }

    companion object : ModuleFactory {

        const val moduleName = "HOSTED_DATA_LAYER"

        override fun create(context: TealiumContext): Module {
            return HostedDataLayer(context.config)
        }
    }
}