package com.tealium.hosteddatalayer

import com.tealium.core.Logger
import com.tealium.core.TealiumConfig
import com.tealium.core.persistence.KeyValueDao
import java.io.File
import java.util.concurrent.TimeUnit

interface DataLayerStorage : KeyValueDao<String, HostedDataLayerEntry>

class DataLayerStore(config: TealiumConfig,
                     private val hdlDirectory: File = File(config.tealiumDirectory, HOSTED_DATALAYER_SUBDIRECTORY),
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
        val entry = when (cache.containsKey(key)) {
            true -> cache[key]
            false -> {
                if (contains(key)) {
                    val file = File(hdlDirectory, "$key$JSON_FILE_EXTENSION")
                    HostedDataLayerEntry.fromFile(file)
                } else {
                    null
                }
            }
        }
        return entry?.let {
            when (isExpired(it.lastUpdated)) {
                true -> {
                    delete(it.id)
                    null
                }
                else -> {
                    cache(it.id, it)
                    it
                }
            }
        }
    }

    override fun getAll(): Map<String, HostedDataLayerEntry> {
        val entries = mutableMapOf<String, HostedDataLayerEntry>()
        hdlDirectory.listFiles { _, name -> name.endsWith(JSON_FILE_EXTENSION) }?.forEach { fileName ->
            val key = fileName.nameWithoutExtension
            val entry = get(key)
            entry?.let {
                entries[key] = it
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
        cache.remove(key)
        val file = File(hdlDirectory, "$key$JSON_FILE_EXTENSION")
        if (file.exists()) {
            file.delete()
        }
    }

    override fun upsert(item: HostedDataLayerEntry) {
        while (!hasSpace()) {
            Logger.dev(BuildConfig.TAG, "Max cache reached. Removing oldest.")
            removeOldestDataLayer()
        }

        HostedDataLayerEntry.toFile(hdlDirectory, item)
        cache(item.id, item)
    }

    override fun clear() {
        keys().forEach { key ->
            delete(key)
        }
    }

    override fun keys(): List<String> {
        return hdlDirectory.listFiles { _, name -> name.endsWith(JSON_FILE_EXTENSION) }
                ?.filterNotNull()
                ?.map {
                    it.nameWithoutExtension
                } ?: emptyList()
    }

    override fun count(): Int {
        return hdlDirectory.list { _, name -> name.endsWith(JSON_FILE_EXTENSION) }?.size ?: 0
    }

    override fun contains(key: String): Boolean {
        return cache.containsKey(key) || File(hdlDirectory, "$key.json").exists()
    }

    override fun purgeExpired() {
        filesSortedByAge().filter { file ->
            isExpired(file.lastModified())
        }.forEach { file ->
            delete(file.nameWithoutExtension)
        }
    }

    private fun isExpired(timestamp: Long): Boolean {
        return (timestamp <
                System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(maxCacheTimeMinutes))
    }

    private fun hasSpace(incoming: Int = 1): Boolean {
        return count() + incoming.coerceAtLeast(1) <= maxCacheSize
    }

    private fun removeOldestDataLayer() {
        val oldest = filesSortedByAge().first()
        delete(oldest.nameWithoutExtension)
    }

    private fun filesSortedByAge(): Array<File> {
        return hdlDirectory.listFiles()?.also { file ->
            file.sortBy { it.lastModified() }
        } ?: emptyArray()
    }

    companion object {
        const val JSON_FILE_EXTENSION = ".json"
        const val HOSTED_DATALAYER_SUBDIRECTORY = "hdl"
    }
}