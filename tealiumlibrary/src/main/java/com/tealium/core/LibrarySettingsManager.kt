package com.tealium.core

import com.tealium.core.messaging.EventRouter
import com.tealium.core.model.LibrarySettings
import com.tealium.core.network.NetworkClient
import com.tealium.core.network.ResourceRetriever
import com.tealium.tealiumlibrary.BuildConfig
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import org.json.JSONObject
import kotlin.properties.Delegates

class LibrarySettingsManager(val config: TealiumConfig,
                             networkClient: NetworkClient,
                             var loader: Loader? = null,
                             private var eventRouter: EventRouter) {

    private val resourceRetriever: ResourceRetriever
    private var job: Deferred<JSONObject?>? = null

    init {
        if (loader == null) {
            loader = JsonLoader(config.application)
        }
        resourceRetriever = ResourceRetriever(config, urlString, networkClient)
    }

    var librarySettings: LibrarySettings by Delegates.observable(
            initialValue = LibrarySettings(),
            onChange = { _, _, new ->
                eventRouter.onLibrarySettingsUpdated(new)
            }
    )
    private var isLocalSettingsLoaded = false

    private val urlString: String
        get() = config.overrideLibrarySettingsUrl
                ?: "https://tags.tiqcdn.com/dle/${config.accountName}/${config.profileName}/tealium-settings.json"

    suspend fun fetchLibrarySettings() = coroutineScope {
        if (!isLocalSettingsLoaded) {
            loadLocalSettings()
        }
        fetchRemoteSettings()
    }

    // todo: this might need to be reworked to read from persistence and not local the second time
    private fun loadLocalSettings() {
        loader?.loadFromAsset("tealium-settings.json")?.let {
            JSONObject(it)
        }?.let {
            isLocalSettingsLoaded = true
            Logger.dev(BuildConfig.TAG, "Loaded local library settings")
            this@LibrarySettingsManager.librarySettings = LibrarySettings.fromJson(it)
        }
    }

    private suspend fun fetchRemoteSettings() = coroutineScope {
        if (job?.isActive == false || job == null) {
            job = async {
                if (isActive) {
                    resourceRetriever.fetch()
                } else {
                    null
                }
            }
            job?.await()?.let {
                Logger.dev(BuildConfig.TAG, "Loaded remote library settings $librarySettings.")
                librarySettings = LibrarySettings.fromJson(it)
            }
        }
    }
}

