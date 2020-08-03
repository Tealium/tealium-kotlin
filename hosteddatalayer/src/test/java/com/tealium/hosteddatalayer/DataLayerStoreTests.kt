package com.tealium.hosteddatalayer

import com.tealium.core.TealiumConfig
import com.tealium.core.network.Connectivity
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.spyk
import io.mockk.verify
import junit.framework.Assert
import junit.framework.Assert.*
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.lang.Exception
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
class DataLayerStoreTests {

    @MockK
    lateinit var mockConfig: TealiumConfig

    @MockK
    lateinit var mockConnectivity: Connectivity

    private lateinit var mockDirectory: File
    private lateinit var cache: MutableMap<String, HostedDataLayerEntry>
    private lateinit var options: MutableMap<String, Any>

    private lateinit var store: DataLayerStore

    private val invalidFile = "invalid"
    private val validFile = "valid"
    private val secondValidFile = "second"
    private val defaultCacheSize = 5
    private val defaultCacheTime = 5L

    private val lastUpdated = System.currentTimeMillis()
    private val data = JSONObject("{}")
    private val validDataLayer = HostedDataLayerEntry(validFile,
            lastUpdated,
            data)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        options = mutableMapOf()
        cache = mutableMapOf()

        // reasonable defaults
        every { mockConfig.options } returns options
        every { mockConfig.hostedDataLayerMaxCacheSize } returns defaultCacheSize
        every { mockConfig.hostedDataLayerMaxCacheTimeMinutes } returns defaultCacheTime

        every { mockConnectivity.isConnected() } returns true
        every { mockConnectivity.isConnectedWifi() } returns true


        // mock directories, valid file
        mockDirectory = spyk(File("hdl_test"))
        mockDirectory.mkdir()
        createTestFile(mockDirectory, validDataLayer)
        createTestFile(mockDirectory, validDataLayer.copy(id = secondValidFile))

        store = spyk(DataLayerStore(mockConfig, mockDirectory, cache))
    }

    @After
    fun tearDown() {
        // Delete all saved files.
        mockDirectory.list()?.forEach {
            File(mockDirectory, it).delete()
        }
    }

    @Test
    fun dao_get_returnsValidEntryWhenFileExists() {
        val result = store.get(validFile)
        assertNotNull(result)
        assertDataLayerEquals(validDataLayer, result!!)
    }

    @Test
    fun dao_get_returnsNullWhenFileDoesntExist() {
        val result = store.get(invalidFile)
        assertNull(result)

        verify(exactly = 1) {
            store.contains(invalidFile)
        }
    }

    @Test
    fun dao_get_readsFromCacheWhenAvailable() {
        cache[validFile] = validDataLayer

        val result = store.get(validFile)
        assertDataLayerEquals(validDataLayer, result!!)

        verify(exactly = 0) {
            store.contains(validFile)
        }
    }

    @Test
    fun dao_get_addsToCacheWhenReadFromDisk() {
        val result = store.get(validFile)

        assertTrue(cache.containsKey(validFile))
        assertEquals(result, cache[validFile])
    }

    @Test
    fun dao_get_doesNotReturnExpired_WhenNotCached() {
        store.clear()
        val unexpiredDataLayer = validDataLayer.copy(id = "unexpired")
        val expiredDataLayer = unexpiredDataLayer.copy(id = "expired",
                lastUpdated = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(defaultCacheTime) * 2)
        createTestFile(mockDirectory, unexpiredDataLayer)
        createTestFile(mockDirectory, expiredDataLayer)

        assertNotNull(store.get("unexpired"))
        assertNull(store.get("expired"))
    }

    @Test
    fun dao_get_doesNotReturnExpired_WhenCached() {
        store.clear()
        val unexpiredDataLayer = validDataLayer.copy(id = "unexpired")
        val expiredDataLayer = unexpiredDataLayer.copy(id = "expired",
                lastUpdated = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(defaultCacheTime) * 2)
        store.insert(unexpiredDataLayer)
        store.insert(expiredDataLayer)

        assertNotNull(store.get("unexpired"))
        assertNull(store.get("expired"))
    }

    @Test
    fun dao_getAll_returnsAllItems() {
        val result = store.getAll()

        assertEquals(2, result.size)
        assertTrue(result.containsKey(validFile))
        assertTrue(result.containsKey(secondValidFile))

        // cache gets loaded
        assertTrue(cache.containsKey(validFile))
        assertTrue(cache.containsKey(secondValidFile))

        assertDataLayerEquals(validDataLayer, result[validFile]!!)
    }

    @Test
    fun dao_getAll_addsToCacheWhenReadFromDisk() {
        val result = store.getAll()

        assertTrue(cache.containsKey(validFile))
        assertEquals(result[validFile], cache[validFile])
    }

    @Test
    fun dao_upsert_savesToDiskAndUpdatesCache() {
        val id = "new_entry"
        val newEntry = validDataLayer.copy(id = id)
        store.upsert(newEntry)

        val result = store.get(id)
        assertNotNull(result)
        assertDataLayerEquals(newEntry, result!!)

        assertEquals(result, cache[id])
        assertTrue(File(mockDirectory, "$id.json").exists())
    }

    @Test
    fun dao_upsert_overwritesExistingFile() {
        val id = "new_entry"
        val newUpdate = lastUpdated + 100
        val newData = JSONObject("{\"key\":\"value\"}")
        val newEntry = HostedDataLayerEntry(id,
                newUpdate,
                newData)
        store.upsert(newEntry)

        val result = store.get(id)
        assertNotNull(result)
        assertDataLayerEquals(newEntry, result!!)
    }

    @Test
    fun dao_delete_removesExistingFileAndCache() {
        cache[validFile] = validDataLayer
        assertTrue(File(mockDirectory, "$validFile.json").exists())

        store.delete(validFile)

        assertFalse(File(mockDirectory, "$validFile.json").exists())
        assertFalse(cache.containsKey(validFile))
    }

    @Test
    fun dao_delete_doesNotFailOnInvalidFile() {
        try {
            store.delete(invalidFile)
        } catch (ex: Exception) {
            Assert.fail()
        }
    }

    @Test
    fun dao_clear_removesAllFiles() {
        assertEquals(2, store.count())
        store.clear()
        assertEquals(0, store.count())
    }

    @Test
    fun dao_keys_returnsAllKeys() {
        val keys = store.keys()

        assertEquals(2, keys.size)
        assertTrue(keys.contains(validFile))
        assertTrue(keys.contains(secondValidFile))
        assertFalse(keys.contains(invalidFile))
    }

    @Test
    fun dao_count_isCorrect() {
        assertEquals(2, store.count())
    }

    @Test
    fun dao_count_updatesAfterInsert() {
        assertEquals(2, store.count())
        store.insert(validDataLayer.copy("some_other_Id"))
        assertEquals(3, store.count())
    }

    @Test
    fun dao_contains_trueWhenFileExists() {
        assertTrue(store.contains(validFile))
    }

    @Test
    fun dao_contains_falseWhenFileDoesntExist() {
        assertFalse(store.contains(invalidFile))
    }

    @Test
    fun dao_contains_readsFromCacheFirst() {
        // add to cache, but delete underlying file (should still return true)
        cache[validFile] = validDataLayer
        File(mockDirectory, "$validFile.json").delete()

        store.contains(validFile)
    }

    @Test
    fun dao_purge_removesAllExpired() {
        store.clear()
        val unexpiredDataLayer = validDataLayer.copy(id = "unexpired")
        val expiredDataLayer = unexpiredDataLayer.copy(id = "expired",
                lastUpdated = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(defaultCacheTime) * 2)
        createTestFile(mockDirectory, unexpiredDataLayer)
        createTestFile(mockDirectory, expiredDataLayer)

        assertEquals(2, store.count())

        store.purgeExpired()
        assertEquals(1, store.count())
        assertTrue(store.contains("unexpired"))
        assertFalse(store.contains("expired"))
    }

    @Test
    fun size_doesNotExceedMax_AndRemovesOldest(){
        store.clear()
        assertEquals(0, store.count())

        repeat (defaultCacheSize + 5) {
            val entry = HostedDataLayerEntry("entry$it",
                    System.currentTimeMillis() - it * 10,
                        JSONObject("{}")
                    )
            store.insert(entry)
        }

        assertEquals(defaultCacheSize, store.count())

        // should contain the newest
        assertTrue(store.contains("entry9"))
        assertTrue(store.contains("entry8"))
        assertTrue(store.contains("entry7"))
        assertTrue(store.contains("entry6"))
        assertTrue(store.contains("entry5"))
        // should have dropped the oldest
        assertFalse(store.contains("entry4"))
        assertFalse(store.contains("entry3"))
        assertFalse(store.contains("entry2"))
        assertFalse(store.contains("entry1"))
        assertFalse(store.contains("entry0"))
    }


    /**
     * Utility to check equality of Data Layers.
     * [HostedDataLayerEntry.lastUpdated] is taken from the [File.lastModified] and may only have
     * and accuracy to the nearest second, so is considered equal if it's within a second of the
     * other.
     */
    private fun assertDataLayerEquals(entry1: HostedDataLayerEntry, entry2: HostedDataLayerEntry) {
        assertEquals(entry1.id, entry2.id)
        // to the nearest second, as File api may only support second accuracy
        assertTrue(abs(entry1.lastUpdated - entry2.lastUpdated) < 1000)
        assertEquals(entry1.data.toString(), entry2.data.toString())
    }

    /**
     * Utility to add files directly, bypassing the DataLayerStore cache
     */
    private fun createTestFile(dir: File, entry: HostedDataLayerEntry) {
        val file = HostedDataLayerEntry.toFile(dir, entry)
        file.writeText(entry.data.toString(), Charsets.UTF_8)
        file.setLastModified(entry.lastUpdated)
    }
}