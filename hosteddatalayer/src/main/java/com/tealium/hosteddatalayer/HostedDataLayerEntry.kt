package com.tealium.hosteddatalayer

import com.tealium.core.Logger
import org.json.JSONException
import org.json.JSONObject
import java.io.File

data class HostedDataLayerEntry(val id: String,
                                val lastUpdated: Long,
                                val data: JSONObject) {

    companion object {
        fun fromFile(file: File): HostedDataLayerEntry? {
            if (file.exists()) {
                try {
                    val json = JSONObject(file.readText(Charsets.UTF_8))
                    return HostedDataLayerEntry(
                            file.nameWithoutExtension,
                            file.lastModified(),
                            json)
                } catch (ex: JSONException) {
                    Logger.dev(BuildConfig.TAG, "Failed to read json from file.")
                }
            }
            return null
        }

        fun toFile(dir: File, dataLayer: HostedDataLayerEntry): File {
            return File(dir, "${dataLayer.id}${DataLayerStore.JSON_FILE_EXTENSION}").also {
                it.setLastModified(dataLayer.lastUpdated)
            }
        }
    }
}