package com.tealium.autotracking.internal

import android.content.SharedPreferences
import com.tealium.autotracking.BuildConfig
import com.tealium.autotracking.autoTrackingBlocklistFilename
import com.tealium.autotracking.autoTrackingBlocklistUrl
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

class ActivityBlocklist(
        private val config: TealiumConfig,
        private val loader: Loader = JsonLoader.getInstance(config.application),
        private val sharedPreferences: SharedPreferences = config.application.getSharedPreferences(getSharedPreferencesName(config), 0),
        private val connectivity: Connectivity = ConnectivityRetriever.getInstance(config.application)
) {

    @Volatile
    private var blocklist: Set<String> = setOf()

    private val blocklistAssetFilename: String? = config.autoTrackingBlocklistFilename
    private val blocklistUrl: String? = config.autoTrackingBlocklistUrl
    private var hasFetched = false

    init {
        blocklistAssetFilename?.let { assetName ->
            loadFromAsset(assetName)
        } ?: blocklistUrl?.let { urlString ->
            loadFromCache()
            updateFromRemoteUrl(urlString)
        }
    }

    private fun updateFromRemoteUrl(urlString: String) {
        if (hasFetched || sharedPreferences.getLong(KEY_CACHED_BLOCKLIST_NEXT_REFRESH, -1) >= System.currentTimeMillis()) {
            Logger.dev(BuildConfig.TAG, "Blocklist refresh time not exceeded; will not update.")
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
                blocklist = JSONArray(assetData).toStringSet()
                Logger.dev(BuildConfig.TAG, "Loaded AutoTracking Blocklist from asset: $blocklist")
            } catch (jex: JSONException) {
                Logger.qa(BuildConfig.TAG, "Autotracking blocklist asset failed to load.")
            }
        }
    }

    private fun loadFromUrl(urlString: String) {
        try {
            Logger.dev(BuildConfig.TAG, "Updating blocklist from $urlString")
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
        sharedPreferences.getStringSet(KEY_CACHED_BLOCKLIST, null)?.filterNotNull()?.let {
            blocklist = it.toMutableSet()
            Logger.dev(BuildConfig.TAG, "Loaded AutoTracking Blocklist from cache: $blocklist")
        }
    }

    private fun updateCache(newBlocklist: Set<String>) {
        blocklist = newBlocklist
        sharedPreferences.edit()
                .putStringSet(KEY_CACHED_BLOCKLIST, newBlocklist)
                .putLong(KEY_CACHED_BLOCKLIST_NEXT_REFRESH, getNextRefresh())
                .apply()
        Logger.dev(BuildConfig.TAG, "AutoTracking Blocklist updated: $blocklist")
    }

    fun isBlocklisted(name: String): Boolean {
        blocklistUrl?.let {
            updateFromRemoteUrl(it)
        }
        return blocklist.any { name.contains(it, true) }
    }

    companion object {
        private const val KEY_CACHED_BLOCKLIST = "cached_blocklist"
        private const val KEY_CACHED_BLOCKLIST_NEXT_REFRESH = "cached_blocklist_next_refresh"
        private const val CACHED_BLOCKLIST_TTL: Long = 24 * 60 * 60 * 1000

        @JvmStatic
        internal fun getNextRefresh(): Long {
            return System.currentTimeMillis() + CACHED_BLOCKLIST_TTL
        }

        @JvmStatic
        internal fun getSharedPreferencesName(config: TealiumConfig): String {
            return "tealium.autotracking.blocklist." + Integer.toHexString((config.accountName + config.profileName + config.environment.environment).hashCode())
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
                Logger.dev(BuildConfig.TAG, "Error reading blocklist entry from JSONArray: ${jex.message}")
            }
        }

        return set.toSet()
    }
}