package com.tealium.hosteddatalayer

import com.tealium.core.*
import com.tealium.core.network.*
import com.tealium.core.validation.DispatchValidator
import com.tealium.dispatcher.Dispatch
import org.json.JSONException
import org.json.JSONObject

/**
 * Module to integrate with the Tealium Hosted Data Layer.
 */
class HostedDataLayer(private val config: TealiumConfig,
                      private val dataLayerStorage: DataLayerStorage = DataLayerStore(config),
                      private val httpClient: NetworkClient = HttpClient(config, connectivity = ConnectivityRetriever.getInstance(config.application)))
    : Module, Transformer, DispatchValidator {

    override val name: String
        get() = MODULE_NAME
    override var enabled: Boolean = true

    private val pending = mutableSetOf<String>()

    /**
     * Event Mapping should contain key-value pairs, where the key is equal to the value found in
     * "tealium_event" within the [Dispatch]. The Value should be the key within the [Dispatch] to
     * look in for the DataLayer Id.
     */
    private val eventMappings: Map<String, String> = config.hostedDataLayerEventMappings
            ?: emptyMap()

    /**
     * Base URL for Tealium's Hosted DataLayer CDN location
     */
    private val hostedDataLayerBaseUrl = "https://tags.tiqcdn.com/dle/${config.accountName}/${config.profileName}/"

    /**
     * Keeps track of any failed Data Layer Id's so they are not unnecessarily making requests for
     * items known to have failed in this application launch.
     */
    private val failedDataLayerIds = mutableListOf<String>()

    init {
        dataLayerStorage.purgeExpired()
    }

    /**
     * Looks in the cache for a stored copy of the Data Layer - if found then its data will be
     * merged into the Dispatch. If not found in the cache, then it will fetch the latest version
     * from the Hosted Data Layer CDN.
     */
    override suspend fun transform(dispatch: Dispatch) {
        val eventName = dispatch[Dispatch.Keys.TEALIUM_EVENT]
        val key = eventMappings[eventName]
        if (!key.isNullOrEmpty()) {
            // Found event mapping.
            Logger.dev(BuildConfig.TAG, "Found event mapping from tealium_event=$eventName")
            val id = dispatch[key]?.toString()
            if (!id.isNullOrEmpty()) {
                // Found data layer id in dispatch.
                Logger.dev(BuildConfig.TAG, "Found DataLayerId ($id) in key: $key")
                if (dataLayerStorage.contains(id)) {
                    // data is cached
                    dataLayerStorage.get(id)?.let {
                        Logger.dev(BuildConfig.TAG, "Found DataLayerId ($id) loaded from cache.")
                        merge(dispatch, it)
                    }
                } else {
                    // data is not cached - fetch
                    if (!failedDataLayerIds.contains(id)) {
                        Logger.dev(BuildConfig.TAG, "DataLayerId ($id) not found in cache; fetching.")
                        pending.add(id)

                        try {
                            val dataLayer = fetch(id)
                            if (dataLayer != null) {
                                Logger.dev(BuildConfig.TAG, "DataLayerId ($id) found on CDN; caching.")
                                merge(dispatch, dataLayer)
                                dataLayerStorage.insert(dataLayer)
                            } else {
                                Logger.dev(BuildConfig.TAG, "DataLayerId ($id) not found on CDN; will not be requested again this session.")
                                failedDataLayerIds.add(id)
                            }
                        } finally {
                            pending.remove(id)
                        }
                    }
                }
            }
        }
    }

    /**
     * Fetches the Data Layer from the CDN and returns it if it has been found.
     */
    private suspend fun fetch(id: String): HostedDataLayerEntry? {
        val retriever = ResourceRetriever(config, createUrlForResource(id), httpClient).apply {
            maxRetries = 3
            useIfModifed = false
        }

        return retriever.fetch()?.let {
            try {
                val json = JSONObject(it)
                HostedDataLayerEntry(id, System.currentTimeMillis(), json)
            } catch (ex: JSONException) {
                Logger.qa(BuildConfig.TAG, "Exception parsing retrieved JSON.")
                null
            }
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
        Logger.dev(BuildConfig.TAG, "HostedDataLayer entry found for ${dataLayerEntry.id}; merging.")
        val map = JsonUtils.mapFor(dataLayerEntry.data)
        dispatch.addAll(map)
    }

    override fun shouldQueue(dispatch: Dispatch?): Boolean {
        return (pending.count() > 0).also { queueing ->
            if (queueing) Logger.qa(BuildConfig.TAG, "Awaiting Hosted Data Layer responses for $pending")
        }
    }

    override fun shouldDrop(dispatch: Dispatch): Boolean {
        return false
    }

    /**
     * Clears any locally cached data layers - This will prompt subsequent events to recheck the
     * Hosted Data Layer by making a new HTTP request
     */
    fun clearCache() {
        Logger.qa(BuildConfig.TAG, "Clearing HostedDataLayer cache.")
        dataLayerStorage.clear()
        failedDataLayerIds.clear()
    }

    companion object : ModuleFactory {

        const val MODULE_NAME = "HostedDataLayer"
        const val MODULE_VERSION = BuildConfig.LIBRARY_VERSION

        override fun create(context: TealiumContext): Module {
            return HostedDataLayer(context.config)
        }
    }
}

val Tealium.hostedDataLayer: HostedDataLayer?
    get() = modules.getModule(HostedDataLayer::class.java)

val Modules.HostedDataLayer: ModuleFactory
    get() = com.tealium.hosteddatalayer.HostedDataLayer