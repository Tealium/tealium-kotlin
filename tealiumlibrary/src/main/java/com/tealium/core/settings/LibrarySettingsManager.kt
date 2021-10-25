package com.tealium.core.settings

import com.tealium.core.*
import com.tealium.core.messaging.EventRouter
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
    private val backgroundScope: CoroutineScope
) {

    private val resourceRetriever: ResourceRetriever
    private var job: Deferred<String?>? = null

    // Asset
    private val assetString: String = "tealium-settings.json"
    private var isAssetSettingsLoaded = false

    // Remote
    private var cachedSettingsFile: File = File(config.tealiumDirectory.canonicalPath, assetString)
    private val urlString: String
        get() = config.overrideLibrarySettingsUrl
            ?: "https://tags.tiqcdn.com/utag/${config.accountName}/${config.profileName}/${config.environment.environment}/mobile.html"

    init {
        resourceRetriever = ResourceRetriever(config, urlString, networkClient)
    }

    var librarySettings: LibrarySettings by Delegates.observable(
        initialValue = loadSettings(),
        onChange = { _, _, new ->
            eventRouter.onLibrarySettingsUpdated(new)
        }
    )
    private val defaultInitialSettings: LibrarySettings
        get() = config.overrideDefaultLibrarySettings ?: LibrarySettings()


    suspend fun fetchLibrarySettings() = coroutineScope {
        when (config.useRemoteLibrarySettings) {
            true -> fetchRemoteSettings()
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
    private fun loadSettings(): LibrarySettings {
        return when (config.useRemoteLibrarySettings) {
            true -> {
                val cachedSettings = loadFromCache(cachedSettingsFile)?.also {
                    Logger.dev(BuildConfig.TAG, "Loaded remote settings from cache.")
                }
                backgroundScope.launch {
                    fetchRemoteSettings()
                }
                cachedSettings
            }
            false -> {
                loadFromAsset(assetString).also {
                    if (it != null) Logger.dev(BuildConfig.TAG, "Loaded local library settings.")
                    isAssetSettingsLoaded = true
                }
            }
        } ?: defaultInitialSettings
    }

    private fun loadFromCache(file: File): LibrarySettings? {
        return loader.loadFromFile(file)?.let {
            val json = JSONObject(it)
            LibrarySettings.fromJson(json)
        }
    }

    private fun loadFromAsset(fileName: String): LibrarySettings? {
        return loader.loadFromAsset(fileName)?.let {
            val json = JSONObject(it)
            LibrarySettings.fromJson(json)
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
            job?.await()?.let { string ->
                Logger.dev(BuildConfig.TAG, "Loaded remote library settings $librarySettings.")
                try {
                    // TODO: should read response headers to determine the file type
                    val settings = when (urlString.endsWith(".html")) {
                        true -> {
                            val json = LibrarySettingsExtractor.extractHtmlLibrarySettings(string)
                            json?.let {
                                LibrarySettings.fromMobilePublishSettings(it)
                            }
                        }
                        false -> {
                            val json = JSONObject(string)
                            LibrarySettings.fromJson(json)
                        }
                    }
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
}