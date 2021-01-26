package com.tealium.autotracking.internal

import android.content.SharedPreferences
import com.tealium.autotracking.BuildConfig
import com.tealium.autotracking.autoTrackingBlacklistFilename
import com.tealium.autotracking.autoTrackingBlacklistUrl
import com.tealium.core.JsonLoader
import com.tealium.core.Loader
import com.tealium.core.Logger
import com.tealium.core.TealiumConfig
import com.tealium.core.network.Connectivity
import com.tealium.core.network.ConnectivityRetriever
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONException
import java.net.MalformedURLException
import java.net.URL
import java.util.*

class ActivityBlacklist(
        private val config: TealiumConfig,
        private val loader: Loader = JsonLoader.getInstance(config.application),
        private val sharedPreferences: SharedPreferences = config.application.getSharedPreferences(getSharedPreferencesName(config), 0),
        private val connectivity: Connectivity = ConnectivityRetriever.getInstance(config.application)
) {

    @Volatile
    private var blacklist: Set<String> = setOf()

    private val blacklistAssetFilename: String? = config.autoTrackingBlacklistFilename
    private val blacklistUrl: String? = config.autoTrackingBlacklistUrl
    private var hasFetched = false

    init {
        blacklistAssetFilename?.let { assetName ->
            loadFromAsset(assetName)
        } ?: blacklistUrl?.let { urlString ->
            loadFromCache()
            updateFromRemoteUrl(urlString)
        }
    }

    private fun updateFromRemoteUrl(urlString: String) {
        if (hasFetched || sharedPreferences.getLong(KEY_CACHED_BLACKLIST_NEXT_REFRESH, -1) >= System.currentTimeMillis()) {
            Logger.dev(BuildConfig.TAG, "Blacklist refresh time not exceeded; will not update.")
            return
        }

        if (!connectivity.isConnected()) return

        hasFetched = true
        CoroutineScope(Dispatchers.IO).launch {
            loadFromUrl(urlString)
        }
    }

    private fun loadFromAsset(assetName: String) {
        loader.loadFromAsset(assetName)?.let { assetData ->
            try {
                blacklist = JSONArray(assetData).toStringSet()
                Logger.dev(BuildConfig.TAG, "Loaded AutoTracking Blacklist from asset: $blacklist")
            } catch (jex: JSONException) {
                Logger.qa(BuildConfig.TAG, "Autotracking blacklist asset failed to load.")
            }
        }
    }

    private fun loadFromUrl(urlString: String) {
        try {
            Logger.dev(BuildConfig.TAG, "Updating blacklist from $urlString")
            loader.loadFromUrl(URL(urlString))?.let { urlData ->
                if (urlData is JSONArray) {
                    updateCache(urlData.toStringSet())
                }
            }
        } catch (muex: MalformedURLException) {
            Logger.qa(BuildConfig.TAG, "Malformed URL provided: ${muex.message}")
        }
    }

    private fun loadFromCache() {
        sharedPreferences.getStringSet(KEY_CACHED_BLACKLIST, null)?.filterNotNull()?.let {
            blacklist = it.toMutableSet()
            Logger.dev(BuildConfig.TAG, "Loaded AutoTracking Blacklist from cache: $blacklist")
        }
    }

    private fun updateCache(newBlacklist: Set<String>) {
        blacklist = newBlacklist
        sharedPreferences.edit()
                .putStringSet(KEY_CACHED_BLACKLIST, newBlacklist)
                .putLong(KEY_CACHED_BLACKLIST_NEXT_REFRESH, getNextRefresh())
                .apply()
        Logger.dev(BuildConfig.TAG, "AutoTracking Blacklist updated: $blacklist")
    }

    fun isBlacklisted(name: String): Boolean {
        blacklistUrl?.let {
            updateFromRemoteUrl(it)
        }
        return blacklist.any { name.contains(it, true) }
    }

    companion object {
        private const val KEY_CACHED_BLACKLIST = "cached_blacklist"
        private const val KEY_CACHED_BLACKLIST_NEXT_REFRESH = "cached_blacklist_next_refresh"
        private const val CACHED_BLACKLIST_TTL: Long = 24 * 60 * 60 * 1000

        @JvmStatic
        internal fun getNextRefresh(): Long {
            return System.currentTimeMillis() + CACHED_BLACKLIST_TTL
        }

        @JvmStatic
        internal fun getSharedPreferencesName(config: TealiumConfig): String {
            return "tealium.autotracking.blacklist." + Integer.toHexString((config.accountName + config.profileName + config.environment.environment).hashCode())
        }
    }

    private fun JSONArray.toStringSet(): Set<String> {
        val set: MutableSet<String> = mutableSetOf()

        for (i in 0 until this.length()) {
            try {
                this.optString(i, null)?.let {
                    set.add(it.toLowerCase(Locale.ROOT))
                }
            } catch (jex: JSONException) {
                Logger.dev(BuildConfig.TAG, "Error reading blacklist entry from JSONArray: ${jex.message}")
            }
        }

        return set.toSet()
    }
}