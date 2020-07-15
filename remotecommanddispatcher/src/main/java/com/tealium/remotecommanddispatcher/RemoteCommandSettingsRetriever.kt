package com.tealium.remotecommanddispatcher

import com.tealium.core.JsonLoader
import com.tealium.core.Loader
import com.tealium.core.Logger
import com.tealium.core.TealiumContext
import com.tealium.core.network.HttpClient
import com.tealium.core.network.NetworkClient
import org.json.JSONObject
import java.net.URL

class RemoteCommandSettingsRetriever(private val context: TealiumContext,
                                     private val client: NetworkClient = HttpClient(context.config),
                                     private var loader: Loader = JsonLoader(context.config.application)) {

    fun loadConfig(remoteCommand: RemoteCommand) {

    }

    fun fetchLocalSettings(remoteCommand: RemoteCommand): JSONObject? {
        remoteCommand.filename?.let { filename ->
            loader.loadFromAsset(filename)?.let {
                Logger.dev(BuildConfig.TAG, "Found remote command with file name: $filename.")
                JSONObject(it)
            }
        }
        return null
    }

    fun fetchRemoteSettings(remoteCommand: RemoteCommand) {
        remoteCommand.remoteUrl?.let { url ->
            loader.loadFromUrl(URL(url))?.let {
                Logger.dev(BuildConfig.TAG, "Fetched Remote Command Config: $it.")
                remoteCommand.settings = RemoteCommandSettings.fromJson(it as JSONObject)
            }
        }
    }

    fun saveSettings(remoteCommand: RemoteCommand) {

    }

    fun readSettings(remoteCommand: RemoteCommand) {

    }
}