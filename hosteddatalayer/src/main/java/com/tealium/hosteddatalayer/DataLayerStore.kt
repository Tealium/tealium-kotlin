package com.tealium.hosteddatalayer

import com.tealium.core.TealiumConfig
import com.tealium.core.persistence.KeyValueDao
import org.json.JSONObject
import java.io.File
import java.io.FilenameFilter
import java.util.concurrent.TimeUnit

interface DataLayerStorage : KeyValueDao<String, HostedDataLayerEntry>

data class HostedDataLayerEntry(val id: String,
                                val lastUpdated: Long,
                                val data: JSONObject) {

    companion object {
        const val KEY_ID = "key"
        const val KEY_LAST_UPDATED = "last_updated"
        const val KEY_DATA = "data"

        fun fromJson(json: JSONObject): HostedDataLayerEntry? {
            if (!json.has(KEY_ID) || !json.has(KEY_LAST_UPDATED) || !json.has(KEY_DATA)) {
                return null
            }
            return HostedDataLayerEntry(
                    json.getString(KEY_ID),
                    json.getLong(KEY_LAST_UPDATED),
                    json.getJSONObject(KEY_DATA)
            )
        }

        fun toJson(entry: HostedDataLayerEntry): JSONObject {
            val json = JSONObject()
            json.put(KEY_ID, entry.id)
            json.put(KEY_LAST_UPDATED, entry.lastUpdated)
            json.put(KEY_DATA, entry.data)
            return json
        }
    }

}

class DataLayerStore(config: TealiumConfig,
                     private val hdlDirectory: File = File(config.tealiumDirectory, hostDataLayerSubdirectory),
                     private val cache: MutableMap<String, HostedDataLayerEntry> = mutableMapOf()) : DataLayerStorage {

    private val maxCacheSize = config.hostedDataLayerMaxCacheSize ?: 50
    private val maxCacheTimeMinutes = config.hostedDataLayerMaxCacheTimeMinutes ?: TimeUnit.DAYS.toMinutes(7)

    init {
        hdlDirectory.mkdir()
    }

    private fun cache(key: String, entry: HostedDataLayerEntry?) {
        entry?.let {
            cache.put(key, it)
        }
    }

    override fun get(key: String): HostedDataLayerEntry? {
        return when (cache.containsKey(key)) {
            true -> cache[key]
            false -> {
                if (contains(key)) {
                    val file = File(hdlDirectory, "$key$jsonFileExtension")
                    val jsonText = file.readText(Charsets.UTF_8)
                    HostedDataLayerEntry.fromJson(JSONObject(jsonText))
                } else {
                    null
                }
            }
        }.also {
            cache(key, it)
        }
    }

    override fun getAll(): Map<String, HostedDataLayerEntry> {
        val entries = mutableMapOf<String, HostedDataLayerEntry>()
        hdlDirectory.list { _, name -> name.endsWith(jsonFileExtension) }?.forEach { fileName ->
            val key = fileName.removeSuffix(jsonFileExtension)
            val entry = get(key)
            entry?.let {
                entries.put(key, it)
                cache(key, it)
            }
        }
        return entries
    }

    override fun insert(item: HostedDataLayerEntry) {
        upsert(item)
    }

    override fun update(item: HostedDataLayerEntry) {
        upsert(item)
    }

    override fun delete(key: String) {
        val file = File(hdlDirectory, "$key$jsonFileExtension")
        if (file.exists()) {
            file.delete()
        }
    }

    override fun upsert(item: HostedDataLayerEntry) {
        val file = File(hdlDirectory, "${item.id}$jsonFileExtension")
        file.writeText(HostedDataLayerEntry.toJson(item).toString(), Charsets.UTF_8)
        cache(item.id, item)
    }

    override fun clear() {
        keys().forEach { key ->
            delete(key)
        }
    }

    override fun keys(): List<String> {
        return hdlDirectory.list { _, name -> name.endsWith(jsonFileExtension) }
                ?.filterNotNull()
                ?.map {
                    it.removeSuffix(jsonFileExtension)
                } ?: emptyList()
    }

    override fun count(): Int {
        return hdlDirectory.list { _, name -> name.endsWith(jsonFileExtension) }?.size ?: 0
    }

    override fun contains(key: String): Boolean {
        return cache.containsKey(key) || File(hdlDirectory, "$key.json").exists()
    }

    override fun purgeExpired() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        const val jsonFileExtension = ".json"
        const val hostDataLayerSubdirectory = "hdl"
    }
}