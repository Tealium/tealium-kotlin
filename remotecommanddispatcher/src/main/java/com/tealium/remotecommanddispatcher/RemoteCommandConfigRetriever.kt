package com.tealium.remotecommanddispatcher

import com.tealium.core.*
import com.tealium.core.network.CooldownHelper
import com.tealium.core.network.HttpClient
import com.tealium.core.network.NetworkClient
import com.tealium.core.network.ResourceEntity
import com.tealium.core.network.ResourceRetriever
import com.tealium.core.network.ResponseStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.lang.Exception

interface RemoteCommandConfigRetriever {
    val remoteCommandConfig: RemoteCommandConfig?
    suspend fun refreshConfig()
}

class AssetRemoteCommandConfigRetriever(
    private val config: TealiumConfig,
    private val filename: String,
    private val loader: Loader = JsonLoader.getInstance(config.application)
) : RemoteCommandConfigRetriever {

    private var _remoteCommandConfig: RemoteCommandConfig? = loadConfig()
    override val remoteCommandConfig: RemoteCommandConfig?
        get() = _remoteCommandConfig

    private fun loadConfig(): RemoteCommandConfig? {
        return loadFromAsset(filename)?.also {
            Logger.dev(BuildConfig.TAG, "Loaded local remote command settings.")
        }
    }

    private fun loadFromAsset(filename: String): RemoteCommandConfig? {
        return loadFromAsset(loader, filename)
    }

    override suspend fun refreshConfig() {
        // nothing to do - asset is already loaded if valid.
    }

    companion object {
        fun loadFromAsset(loader: Loader, filename: String): RemoteCommandConfig? {
            return if (!filename.endsWith(".json", true)) {
                loader.loadFromAsset("$filename.json") ?: loader.loadFromAsset(filename)
            } else {
                loader.loadFromAsset(filename)
            }?.let {
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
    private val assetFileName: String? = null,
    private val cacheFile: File = getCacheFile(config, commandId),
    private val cooldownHelper: CooldownHelper? = config.remoteCommandConfigRefresh?.let { refreshInterval ->
        CooldownHelper(refreshInterval * 60 * 1000, 30_000L)
    }
) : RemoteCommandConfigRetriever {

    private var job: Deferred<ResourceEntity?>? = null

    private var _remoteCommandConfig: RemoteCommandConfig? = loadConfig()
    override val remoteCommandConfig: RemoteCommandConfig?
        get() = _remoteCommandConfig

    private fun loadConfig(): RemoteCommandConfig? {
        val cachedSettings = loadFromCache(cacheFile)?.also {
            Logger.dev(BuildConfig.TAG, "Loaded remote command settings from cache")
        }
        backgroundScope.launch {
            refreshConfig()
        }
        return cachedSettings
            ?: AssetRemoteCommandConfigRetriever.loadFromAsset(
                loader, assetFileName ?: "$commandId.json"
            )
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
        if (job == null || job?.isActive == false) {
            job = async {
                if (isActive) {
                    resourceRetriever.fetchWithEtag(remoteCommandConfig?.etag)
                } else {
                    null
                }
            }
            val resourceEntity = job?.await() ?: return@coroutineScope

            if (!(resourceEntity.status == ResponseStatus.Success
                        || (resourceEntity.status is ResponseStatus.Non200Response
                        && (resourceEntity.status as ResponseStatus.Non200Response).code < 400))
            ) {
                Logger.dev(
                    BuildConfig.TAG,
                    "No entity returned for remote command config from remote URL"
                )
                cooldownHelper?.updateStatus(CooldownHelper.CooldownStatus.Failure)

                return@coroutineScope
            }

            Logger.dev(BuildConfig.TAG, "Loaded Remote Command config from remote URL")
            try {
                cooldownHelper?.updateStatus(CooldownHelper.CooldownStatus.Success)

                RemoteCommandConfig.fromResourceEntity(resourceEntity)?.let { settings ->
                    saveSettingsToCache(RemoteCommandConfig.toJson(settings).toString())
                    _remoteCommandConfig = settings
                }
            } catch (ex: JSONException) {
                Logger.dev(
                    BuildConfig.TAG,
                    "Failed to parse remote command config from remote URL"
                )
            }
        }
    }

    private fun saveSettingsToCache(string: String) {
        try {
            Logger.dev(BuildConfig.TAG, "Saving Remote Command settings to file")
            cacheFile.writeText(string, Charsets.UTF_8)
        } catch (ex: Exception) {
            Logger.qa(BuildConfig.TAG, "Failed to save Remote Command settings to file")
        }
    }

    override suspend fun refreshConfig() {
        val lastFetch = resourceRetriever.lastFetchTimestamp
        if (_remoteCommandConfig == null
            && lastFetch != null
            && cooldownHelper?.isInCooldown(lastFetch) != false
        ) {
            Logger.dev(
                BuildConfig.TAG,
                "Not refreshing RemoteCommandConfig for $commandId; still in error cooldown period"
            )
            return
        }

        fetchRemoteSettings()
    }

    companion object {
        fun getCacheFile(config: TealiumConfig, commandId: String): File {
            return File(config.tealiumDirectory.canonicalPath, "$commandId.json")
        }
    }
}