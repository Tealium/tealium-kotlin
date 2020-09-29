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

interface RemoteCommandConfigRetrieverFactory {
    fun retrieveConfig(config: TealiumConfig, commandId: String, filename: String?,remoteUrl: String?) : RemoteCommandConfigRetriever
}

class RemoteCommandConfigRetriever(private val config: TealiumConfig,
                                   private val commandId: String,
                                   private val filename: String? = null,
                                   private val remoteUrl: String? = null,
                                   private val client: NetworkClient = HttpClient(config),
                                   private val loader: Loader = JsonLoader(config.application),
                                   private val backgroundScope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {


    private val tealiumDleUrl = "${Settings.DLE_PREFIX}/${config.accountName}/${config.profileName}/"

    private var resourceRetriever: ResourceRetriever? = null
    private var job: Deferred<String?>? = null

    private var isAssetConfigLoaded = false
    private var cachedSettingsFile: File = File(config.tealiumDirectory.canonicalPath, "${getSettingsFilename()}.json")

    var remoteCommandConfig: RemoteCommandConfig = loadSettings()

    init {
        remoteUrl?.let {
            resourceRetriever = ResourceRetriever(config, it, client).also {
                config.remoteCommandConfigRefresh?.let { refresh ->
                    it.refreshInterval = refresh.toInt()
                }
            }
        }
    }

    private fun loadSettings(): RemoteCommandConfig {
        return filename?.let {
            loadFromAsset(it)?.also {
                isAssetConfigLoaded = true
                Logger.dev(BuildConfig.TAG, "Loaded local remote command settings.")
            }
        } ?: remoteUrl?.let {
            val cachedSettings = loadFromCache(cachedSettingsFile)?.also {
                Logger.dev(BuildConfig.TAG, "Loaded remote command settings from cache")
            }
            backgroundScope.launch {
                fetchRemoteSettings()
            }
            cachedSettings
        } ?: RemoteCommandConfig()
    }

    private fun loadFromCache(file: File): RemoteCommandConfig? {
        return loader.loadFromFile(file)?.let {
            val json = JSONObject(it)
            RemoteCommandConfig.fromJson(json)
        }
    }

    private fun loadFromAsset(filename: String): RemoteCommandConfig? {
        return loader.loadFromAsset(filename)?.let {
            val json = JSONObject(it)
            RemoteCommandConfig.fromJson(json)
        }
    }

    private suspend fun fetchRemoteSettings() = coroutineScope {
        if (job?.isActive == false || job == null) {
            job = async {
                if (isActive) {
                    resourceRetriever?.fetch()
                } else {
                    null
                }
            }
            job?.await()?.let { string ->
                Logger.dev(BuildConfig.TAG, "Loaded Remote Command config from remote URL")
                try {
                    val settings = RemoteCommandConfig.fromJson(JSONObject(string))
                    saveSettingsToCache(RemoteCommandConfig.toJson(settings).toString())
                    remoteCommandConfig = settings
                } catch (ex: JSONException) {
                    Logger.dev(BuildConfig.TAG, "Failed to load remote command config from remote URL")
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
        remoteUrl?.let { url ->
            // check if Tealium DLE URL
            if (url.contains(tealiumDleUrl)) {
                return url.substringAfter(tealiumDleUrl).substringBefore(".json")
            } else if (URLUtil.isValidUrl(url)) {
                return commandId
            }
        }

        return commandId
    }
}