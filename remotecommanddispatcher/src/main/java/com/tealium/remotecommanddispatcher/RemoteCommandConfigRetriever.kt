package com.tealium.remotecommanddispatcher

import android.webkit.URLUtil
import com.tealium.core.*
import com.tealium.core.network.HttpClient
import com.tealium.core.network.NetworkClient
import com.tealium.core.network.ResourceRetriever
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.lang.Exception

interface RemoteCommandConfigRetriever {
    val remoteCommandConfig: RemoteCommandConfig
}

class AssetRemoteCommandConfigRetriever(
    private val config: TealiumConfig,
    private val filename: String,
    private val loader: Loader = JsonLoader.getInstance(config.application)
) : RemoteCommandConfigRetriever {

    private var _remoteCommandConfig: RemoteCommandConfig = loadConfig()
    override val remoteCommandConfig: RemoteCommandConfig
        get() = _remoteCommandConfig

    private fun loadConfig(): RemoteCommandConfig {
        return loadFromAsset(filename)?.also {
            Logger.dev(BuildConfig.TAG, "Loaded local remote command settings.")
        } ?: RemoteCommandConfig()
    }

    private fun loadFromAsset(filename: String): RemoteCommandConfig? {
        return loadFromAsset(loader, filename)
    }

    companion object {
        fun loadFromAsset(loader: Loader, filename: String): RemoteCommandConfig? {
            val fullFilename = if (filename.endsWith(".json")) filename else "$filename.json"

            return loader.loadFromAsset(fullFilename)?.let {
                try {
                    val json = JSONObject(it)
                    RemoteCommandConfig.fromJson(json)
                } catch (ex: JSONException) {
                    Logger.qa(
                        BuildConfig.TAG,
                        "Error loading RemoteCommandsConfig JSON from asset: ${ex.message}"
                    )
                    null
                }
            }
        }
    }
}

class UrlRemoteCommandConfigRetriever(
    private val config: TealiumConfig,
    private val commandId: String,
    private val remoteUrl: String,
    private val client: NetworkClient = HttpClient(config),
    private val resourceRetriever: ResourceRetriever =
        ResourceRetriever(config, remoteUrl, client).also {
            config.remoteCommandConfigRefresh?.let { refresh ->
                it.refreshInterval = refresh.toInt()
            }
        },
    private val loader: Loader = JsonLoader.getInstance(config.application),
    private val backgroundScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val localFileName: String? = null
) : RemoteCommandConfigRetriever {

    private val tealiumDleUrl =
        "${Settings.DLE_PREFIX}/${config.accountName}/${config.profileName}/"

    private val cachedSettingsFile: File =
        File(config.tealiumDirectory.canonicalPath, "${getSettingsFilename()}.json")

    private var job: Deferred<String?>? = null

    private var _remoteCommandConfig: RemoteCommandConfig = loadConfig()
    override val remoteCommandConfig: RemoteCommandConfig
        get() = _remoteCommandConfig

    private fun loadConfig(): RemoteCommandConfig {
        val cachedSettings = loadFromCache(cachedSettingsFile)?.also {
            Logger.dev(BuildConfig.TAG, "Loaded remote command settings from cache")
        }
        backgroundScope.launch {
            fetchRemoteSettings()
        }
        return cachedSettings
            ?: AssetRemoteCommandConfigRetriever.loadFromAsset(
                loader, localFileName ?: "$commandId.json"
            ) ?: RemoteCommandConfig()
    }

    private fun loadFromCache(file: File): RemoteCommandConfig? {
        return loader.loadFromFile(file)?.let {
            try {
                val json = JSONObject(it)
                RemoteCommandConfig.fromJson(json)
            } catch (ex: JSONException) {
                Logger.qa(
                    BuildConfig.TAG,
                    "Error loading RemoteCommandsConfig JSON from cache: ${ex.message}"
                )
                null
            }
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
                Logger.dev(BuildConfig.TAG, "Loaded Remote Command config from remote URL")
                try {
                    val settings = RemoteCommandConfig.fromJson(JSONObject(string))
                    saveSettingsToCache(RemoteCommandConfig.toJson(settings).toString())
                    _remoteCommandConfig = settings
                } catch (ex: JSONException) {
                    Logger.dev(
                        BuildConfig.TAG,
                        "Failed to load remote command config from remote URL"
                    )
                }
            }
        }
    }

    private fun saveSettingsToCache(string: String) {
        try {
            Logger.dev(BuildConfig.TAG, "Saving Remote Command settings to file")
            cachedSettingsFile.writeText(string, Charsets.UTF_8)
        } catch (ex: Exception) {
            Logger.qa(BuildConfig.TAG, "Failed to save Remote Command settings to file")
        }
    }

    private fun getSettingsFilename(): String {
        // check if Tealium DLE URL
        if (remoteUrl.contains(tealiumDleUrl)) {
            return remoteUrl.substringAfter(tealiumDleUrl).substringBefore(".json")
        } else if (URLUtil.isValidUrl(remoteUrl)) {
            return commandId
        }

        return commandId
    }
}