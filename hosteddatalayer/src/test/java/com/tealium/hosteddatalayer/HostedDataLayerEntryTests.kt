package com.tealium.hosteddatalayer

import io.mockk.MockKAnnotations
import io.mockk.spyk
import junit.framework.Assert.*
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
class HostedDataLayerEntryTests {

    private lateinit var mockDirectory: File

    private val invalidFile = "invalid"
    private val validFile = "valid"

    private val lastUpdated = System.currentTimeMillis()
    private val data = JSONObject("{}")
    private val validDataLayer = HostedDataLayerEntry(validFile,
            lastUpdated,
            data)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        // mock directories, valid file
        mockDirectory = spyk(File("hdl_test"))
        mockDirectory.mkdir()
        createTestFile(mockDirectory, validDataLayer)
    }

    @After
    fun tearDown() {
        // Delete all saved files.
        mockDirectory.list()?.forEach {
            File(mockDirectory, it).delete()
        }
    }

    @Test
    fun fromFile_returnsEntryWhenExists() {
        val file = File(mockDirectory, "$validFile.json")
        val result = HostedDataLayerEntry.fromFile(file)

        assertDataLayerEquals(validDataLayer, result!!)
    }

    @Test
    fun fromFile_returnsNullWhenNotExists() {
        val file = File(mockDirectory, "$invalidFile.json")
        val result = HostedDataLayerEntry.fromFile(file)

        assertNull(result)
    }

    @Test
    fun toFile_returnsValidFile() {
        val result = HostedDataLayerEntry.toFile(mockDirectory, validDataLayer)

        assertEquals(validFile, result.nameWithoutExtension)
        assertEquals("${validFile}.json", result.name)
        assertTrue(validDataLayer.lastUpdated - result.lastModified() < 1000)
        assertEquals("{}", result.readText(Charsets.UTF_8))
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