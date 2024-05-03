package com.tealium.core.settings

import com.tealium.core.*
import com.tealium.core.messaging.EventRouter
import com.tealium.core.network.ResourceEntity
import com.tealium.core.network.NetworkClient
import com.tealium.core.network.ResourceRetriever
import com.tealium.tealiumlibrary.BuildConfig
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.lang.Exception
import kotlin.properties.Delegates

class LibrarySettingsManager(
    private val config: TealiumConfig,
    networkClient: NetworkClient,
    private var loader: Loader = JsonLoader.getInstance(config.application),
    private var eventRouter: EventRouter,
    private val backgroundScope: CoroutineScope,
) {

    internal val resourceRetriever: ResourceRetriever
    private var job: Deferred<ResourceEntity?>? = null

    // Asset
    private val assetString: String = "tealium-settings.json"
    private var isAssetSettingsLoaded = false

    // Remote
    private var cachedSettingsFile: File = File(config.tealiumDirectory.canonicalPath, assetString)
    private val urlString: String
        get() = config.overrideLibrarySettingsUrl
            ?: "https://tags.tiqcdn.com/utag/${config.accountName}/${config.profileName}/${config.environment.environment}/mobile.html"

    private val etagKey: String = "etag"

    init {
        resourceRetriever = ResourceRetriever(config, urlString, networkClient)
    }

    /**
     * This is the startup settings based on what is currently present on the device.
     *
     * If [TealiumConfig.useRemoteLibrarySettings] is true, then this will be the cached set of settings
     * if present. If [TealiumConfig.useRemoteLibrarySettings] if false, then this will be the settings
     * derived from the `tealium-settings.json` asset if present.
     */
    val initialSettings: LibrarySettings? = loadSettings()

    /**
     * This is the computed settings but with defaults returned if no other settings were available
     * at launch.
     * If remote settings are enabled, then the cached settings will be loaded; if remote
     * settings are not enabled then this will load the `tealium-settings.json` asset.
     *
     * If those settings were not available, then it will return defaults, in the following order of
     * preference:
     *  - [TealiumConfig.overrideDefaultLibrarySettings] if present
     *  - [LibrarySettings] defaults
     */
    var librarySettings: LibrarySettings by Delegates.observable(
        initialValue = initialSettings ?: defaultInitialSettings,
        onChange = { _, _, new ->
            setRefreshInterval(new.refreshInterval)
            eventRouter.onLibrarySettingsUpdated(new)
        }
    )

    private val defaultInitialSettings: LibrarySettings
        get() = config.overrideDefaultLibrarySettings ?: LibrarySettings()

    init {
        setRefreshInterval(librarySettings.refreshInterval)
    }

    private fun setRefreshInterval(seconds: Int) {
        resourceRetriever.refreshInterval = seconds / 60
    }

    suspend fun fetchLibrarySettings() = coroutineScope {
        when (config.useRemoteLibrarySettings) {
            true -> fetchRemoteSettings(librarySettings.etag)
            false -> {
                if (!isAssetSettingsLoaded) {
                    loadFromAsset(assetString)
                }
            }
        }
    }

    /**
     * Performs the initial load of the LibrarySettings.
     *
     * If using [TealiumConfig.useRemoteLibrarySettings] then the library settings will be requested
     * from the [urlString] in the background and a previously cached version of the settings will
     * be loaded in the interim.
     *
     * Otherwise it checks if the [assetString] exists and loads from that.
     *
     * If all else fails, then use the default [LibrarySettings] or
     * [TealiumConfig.overrideDefaultLibrarySettings] if set.
     */
    private fun loadSettings(): LibrarySettings? {
        return when (config.useRemoteLibrarySettings) {
            true -> {
                val cachedSettings = loadFromCache(cachedSettingsFile)?.also {
                    Logger.dev(BuildConfig.TAG, "Loaded remote settings from cache.")
                }
                backgroundScope.launch {
                    fetchRemoteSettings(cachedSettings?.etag)
                }
                cachedSettings
            }

            false -> {
                loadFromAsset(assetString).also {
                    if (it != null) Logger.dev(BuildConfig.TAG, "Loaded local library settings.")
                    isAssetSettingsLoaded = true
                }
            }
        }
    }

    private fun loadFromCache(file: File): LibrarySettings? {
        return loader.loadFromFile(file)?.let {
            val json = JsonUtils.tryParse(it)

            if (json == null) {
                // cached file is invalid; delete
                removeFromCache()
                return null
            }

            LibrarySettings.fromJson(json)
        }
    }

    private fun loadFromAsset(fileName: String): LibrarySettings? {
        return loader.loadFromAsset(fileName)?.let {
            val json = JsonUtils.tryParse(it) ?: return null

            LibrarySettings.fromJson(json)
        }
    }

    private suspend fun fetchRemoteSettings(etag: String? = null) = coroutineScope {
        if (job?.isActive == false || job == null) {
            job = async {
                if (isActive) {
                    resourceRetriever.fetchWithEtag(etag)
                } else {
                    null
                }
            }
            val resource = job?.await()
            resource?.let { resource ->
                try {
                    // TODO: should read resource headers to determine the file type
                    val settings = when (urlString.endsWith(".html")) {
                        true -> {
                            val json = LibrarySettingsExtractor.extractHtmlLibrarySettings(resource)
                            json?.let {
                                LibrarySettings.fromMobilePublishSettings(it)
                            }
                        }

                        false -> {
                            if (resource.response?.let { JsonUtils.isValidJson(it) } == true) {
                                val json = JSONObject(resource.response)
                                resource.etag?.let {
                                    json.put(etagKey, it)
                                }
                                LibrarySettings.fromJson(json)
                            } else null
                        }
                    }

                    Logger.dev(BuildConfig.TAG, "Loaded remote library settings $settings.")

                    settings?.let {
                        writeToCache(LibrarySettings.toJson(settings).toString())
                        librarySettings = it
                    }
                } catch (ex: JSONException) {
                    Logger.qa(
                        BuildConfig.TAG,
                        "Failed to extract remote Library Settings from HTML."
                    )
                }
            }
        }
    }

    private fun writeToCache(string: String) {
        try {
            Logger.dev(BuildConfig.TAG, "Writing LibrarySettings to file.")
            cachedSettingsFile.writeText(string, Charsets.UTF_8)
        } catch (ex: Exception) {
            Logger.qa(BuildConfig.TAG, "Failed to write LibrarySettings to file.")
        }
    }

    private fun removeFromCache() {
        try {
            cachedSettingsFile.delete()
        } catch (ex: Exception) {
            Logger.qa(BuildConfig.TAG, "Failed to delete cached LibrarySettings file.")
        }
    }
}