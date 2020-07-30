package com.tealium.core.persistence

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.runner.AndroidJUnit4
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import junit.framework.Assert.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Exception
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class KeyValueStorageTests {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var storage: SqlDataLayer

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = TealiumConfig(context as Application, "test", "test", Environment.DEV)
        dbHelper = DatabaseHelper(config, null) // in-memory

        storage = PersistentStorage(dbHelper, "datalayer")
    }

    @Test
    fun testPutString() {
        storage.putString("string_key", "string_value")

        assertTrue(storage.contains("string_key"))
        assertTrue(storage.keys().contains("string_key"))
        assertEquals("string_value", storage.getString("string_key"))
    }

    @Test
    fun testPutInteger() {
        storage.putInt("int_key", 10)

        assertTrue(storage.contains("int_key"))
        assertTrue(storage.keys().contains("int_key"))
        assertEquals(10, storage.getInt("int_key"))
    }

    @Test
    fun testPutDouble() {
        storage.putDouble("double_key", 10.01)

        assertTrue(storage.contains("double_key"))
        assertTrue(storage.keys().contains("double_key"))
        assertEquals(10.01, storage.getDouble("double_key"))
    }

    @Test
    fun testPutLong() {
        storage.putLong("long_key", 10L)

        assertTrue(storage.contains("long_key"))
        assertTrue(storage.keys().contains("long_key"))
        assertEquals(10L, storage.getLong("long_key"))
    }

    @Test
    fun testPutBoolean() {
        storage.putBoolean("boolean_key_true", true)
        storage.putBoolean("boolean_key_false", false)

        assertTrue(storage.contains("boolean_key_true"))
        assertTrue(storage.contains("boolean_key_false"))
        assertTrue(storage.keys().contains("boolean_key_true"))
        assertTrue(storage.keys().contains("boolean_key_true"))
        assertEquals(true, storage.getBoolean("boolean_key_true"))
        assertEquals(false, storage.getBoolean("boolean_key_false"))
    }

    @Test
    fun testPutStringArray() {
        storage.putStringArray("string_array_key", arrayOf("string_1", "string_2"))

        assertTrue(storage.contains("string_array_key"))
        assertTrue(storage.keys().contains("string_array_key"))
        assertArrayEquals(arrayOf("string_1", "string_2"), storage.getStringArray("string_array_key"))
    }

    @Test
    fun testPutIntArray() {
        storage.putIntArray("int_array_key", arrayOf(1,2,3))

        assertTrue(storage.contains("int_array_key"))
        assertTrue(storage.keys().contains("int_array_key"))
        assertArrayEquals(arrayOf(1,2,3), storage.getIntArray("int_array_key"))
    }

    @Test
    fun testPutLongArray() {
        storage.putLongArray("long_array_key", arrayOf(1L,2L,3L))

        assertTrue(storage.contains("long_array_key"))
        assertTrue(storage.keys().contains("long_array_key"))
        assertArrayEquals(arrayOf(1L,2L,3L), storage.getLongArray("long_array_key"))
    }

    @Test
    fun testPutDoubleArray() {
        storage.putDoubleArray("double_array_key", arrayOf(1.1,2.2,3.3))

        assertTrue(storage.contains("double_array_key"))
        assertTrue(storage.keys().contains("double_array_key"))
        assertArrayEquals(arrayOf(1.1,2.2,3.3), storage.getDoubleArray("double_array_key"))
    }

    @Test
    fun testPutBooleanArray() {
        storage.putBooleanArray("boolean_array_key", arrayOf(true,false,true))

        assertTrue(storage.contains("boolean_array_key"))
        assertTrue(storage.keys().contains("boolean_array_key"))
        assertArrayEquals(arrayOf(true,false,true), storage.getBooleanArray("boolean_array_key"))
    }

    @Test
    fun testPutJsonObject() {
        val emptyJsonObject = JSONObject()
        val filledJsonObject = JSONObject("{\"key\":\"value\",\"nested\":{\"nested_key\":\"nested_value\"}}")

        storage.putJsonObject("empty_json_object", emptyJsonObject)
        storage.putJsonObject("filled_json_object", filledJsonObject)

        assertTrue(storage.contains("empty_json_object"))
        assertTrue(storage.contains("filled_json_object"))
        assertTrue(storage.keys().contains("empty_json_object"))
        assertTrue(storage.keys().contains("filled_json_object"))
        assertEquals(emptyJsonObject.toString(), storage.getJsonObject("empty_json_object").toString())
        assertEquals(filledJsonObject.toString(), storage.getJsonObject("filled_json_object").toString())
    }

    @Test
    fun testContainsKey() {
        storage.putString("string", "String")
        storage.putInt("int", 1)
        storage.putDouble("double", 2.0)
        storage.putLong("long", 2L)
        storage.putBoolean("boolean", true)
        storage.putJsonObject("json", JSONObject())
        storage.putStringArray("string_array", arrayOf("String"))
        storage.putIntArray("int_array", arrayOf(1))
        storage.putDoubleArray("double_array", arrayOf(2.0))
        storage.putLongArray("long_array", arrayOf(2L))
        storage.putBooleanArray("boolean_array", arrayOf(true))

        assertTrue(storage.contains("string"))
        assertTrue(storage.contains("int"))
        assertTrue(storage.contains("double"))
        assertTrue(storage.contains("long"))
        assertTrue(storage.contains("boolean"))
        assertTrue(storage.contains("json"))
        assertTrue(storage.contains("string_array"))
        assertTrue(storage.contains("int_array"))
        assertTrue(storage.contains("double_array"))
        assertTrue(storage.contains("long_array"))
        assertTrue(storage.contains("boolean_array"))

        assertTrue(!storage.contains("missing_key"))
    }

    @Test
    fun testKeysList() {
        storage.putString("string", "String")
        storage.putInt("int", 1)
        storage.putDouble("double", 2.0)
        storage.putLong("long", 2L)
        storage.putBoolean("boolean", true)
        storage.putJsonObject("json", JSONObject())
        storage.putStringArray("string_array", arrayOf("String"))
        storage.putIntArray("int_array", arrayOf(1))
        storage.putDoubleArray("double_array", arrayOf(2.0))
        storage.putLongArray("long_array", arrayOf(2L))
        storage.putBooleanArray("boolean_array", arrayOf(true))

        assertEquals(11, storage.keys().count())

        assertTrue(storage.keys().containsAll(listOf(
                "string", "int", "double", "long", "boolean", "json",
                "string_array", "int_array", "double_array", "long_array", "boolean_array"
        )))
    }

    @Test
    fun testKeysCount() {
        storage.putString("string", "String")
        storage.putInt("int", 1)
        storage.putDouble("double", 2.0)
        storage.putLong("long", 2L)
        storage.putBoolean("boolean", true)
        storage.putJsonObject("json", JSONObject())
        storage.putStringArray("string_array", arrayOf("String"))
        storage.putIntArray("int_array", arrayOf(1))
        storage.putDoubleArray("double_array", arrayOf(2.0))
        storage.putLongArray("long_array", arrayOf(2L))
        storage.putBooleanArray("boolean_array", arrayOf(true))

        assertEquals(11, storage.keys().count())
        assertEquals(11, storage.count())
    }

    @Test
    fun testGetAll() {
        storage.putString("string", "String")
        storage.putInt("int", 1)
        storage.putDouble("double", 2.0)
        storage.putLong("long", 2L)
        storage.putBoolean("boolean", true)
        storage.putJsonObject("json", JSONObject())
        storage.putStringArray("string_array", arrayOf("String"))
        storage.putIntArray("int_array", arrayOf(1))
        storage.putDoubleArray("double_array", arrayOf(2.0))
        storage.putLongArray("long_array", arrayOf(2L))
        storage.putBooleanArray("boolean_array", arrayOf(true))

        val map = storage.getAll()
        assertEquals("String", map["string"])
        assertEquals(1, map["int"])
        assertEquals(2.0, map["double"])
        assertEquals(2L, map["long"])
        assertEquals(true, map["boolean"])
        assertEquals(JSONObject().toString(), (map["json"] as JSONObject).toString())
        assertArrayEquals(arrayOf("String"), map["string_array"] as Array<*>)
        assertArrayEquals(arrayOf(1), map["int_array"] as Array<*>)
        assertArrayEquals(arrayOf(2.0), map["double_array"] as Array<*>)
        assertArrayEquals(arrayOf(2L), map["long_array"] as Array<*>)
        assertArrayEquals(arrayOf(true), map["boolean_array"] as Array<*>)
    }

    @Test
    fun testGetAny() {
        storage.putString("string", "String")
        storage.putInt("int", 1)
        storage.putDouble("double", 2.0)
        storage.putLong("long", 2L)
        storage.putBoolean("boolean", true)
        storage.putJsonObject("json", JSONObject())
        storage.putStringArray("string_array", arrayOf("String"))
        storage.putIntArray("int_array", arrayOf(1))
        storage.putDoubleArray("double_array", arrayOf(2.0))
        storage.putLongArray("long_array", arrayOf(2L))
        storage.putBooleanArray("boolean_array", arrayOf(true))

        assertEquals("String", storage.get("string"))
        assertEquals(1, storage.get("int"))
        assertEquals(2.0, storage.get("double"))
        assertEquals(2L, storage.get("long"))
        assertEquals(true, storage.get("boolean"))
        assertEquals(JSONObject().toString(), (storage.get("json") as JSONObject).toString())
        assertArrayEquals(arrayOf("String"), storage.get("string_array") as Array<*>)
        assertArrayEquals(arrayOf(1), storage.get("int_array") as Array<*>)
        assertArrayEquals(arrayOf(2.0), storage.get("double_array") as Array<*>)
        assertArrayEquals(arrayOf(2L), storage.get("long_array") as Array<*>)
        assertArrayEquals(arrayOf(true), storage.get("boolean_array") as Array<*>)
    }

    @Test
    fun testOverwrites() {
        storage.putString("string", "string 1")
        storage.putString("string", "string 2")
        assertEquals("string 2", storage.getString("string"))

        // change to Int
        storage.putInt("string", 1)
        assertEquals(1, storage.getInt("string"))

        // change to Double
        storage.putDouble("string", 1.0)
        assertEquals(1.0, storage.getDouble("string"))

        // change to Double
        storage.putBooleanArray("string", arrayOf(true, false, true))
        assertArrayEquals(arrayOf(true, false, true), storage.getBooleanArray("string"))
    }

    @Test
    fun testRemovals() {
        storage.putString("string", "string 1")
        assertEquals("string 1", storage.getString("string"))

        storage.remove("string")
        assertEquals(null, storage.getString("string"))

        try {
            storage.remove("missing")
        } catch (ignored: Exception) {
            Assert.fail()
        }
    }

    @Test
    fun testNulls() {
        // keys that dont exist
        assertNull(storage.getString("string"))

        // keys that do exist but the type is wrong.
        storage.putString("string", "string value")
        assertNotNull(storage.getString("string")) // correct
        assertNull(storage.getInt("string"))
        assertNull(storage.getDouble("string"))
        assertNull(storage.getLong("string"))
        assertNull(storage.getBoolean("string"))
        assertNull(storage.getJsonObject("string"))
        assertNull(storage.getStringArray("string"))
        assertNull(storage.getIntArray("string"))
        assertNull(storage.getDoubleArray("string"))
        assertNull(storage.getLongArray("string"))
        assertNull(storage.getBooleanArray("string"))
    }

    @Test
    fun testExpiryDates() = runBlocking {
        val timestamp = System.currentTimeMillis() / 1000
        storage.putBoolean("non_expired_boolean", true, Expiry.afterEpochTime(timestamp + 100))
        storage.putBoolean("expired_boolean_1", true, Expiry.afterEpochTime(timestamp - 100))
        storage.putBoolean("expired_boolean_2", true, Expiry.afterEpochTime(timestamp - 100))
        storage.putBoolean("session_boolean", true, Expiry.SESSION)
        storage.putBoolean("forever_boolean", true, Expiry.FOREVER)

        // storage shouldn't retrieve expired items
        assertNull(storage.getBoolean("expired_boolean_1"))
        assertNull(storage.getBoolean("expired_boolean_2"))

        val map = storage.getAll()
        assertTrue(map["non_expired_boolean"] as Boolean)
        assertNull(map["expired_boolean_1"])
        assertNull(map["expired_boolean_2"])
        assertTrue(map["session_boolean"] as Boolean)
        assertTrue(map["forever_boolean"] as Boolean)

        // counts should all be equal.
        assertEquals(map.count(), storage.count())
        assertEquals(storage.keys().count(), storage.count())

        // overwriting an UNEXPIRED item SHOULD NOT update the expiry time
        storage.putBoolean("non_expired_boolean", false)
        assertEquals(Expiry.afterEpochTime(timestamp + 100), storage.getExpiry("non_expired_boolean"))
        // overwriting an EXPIRED item SHOULD update the expiry time
        storage.putBoolean("expired_boolean_1", false) // no explicit expiry set, should save the default
        assertEquals(Expiry.SESSION, storage.getExpiry("expired_boolean_1"))
        // overwriting an EXPIRED item SHOULD update the expiry time
        storage.putBoolean("expired_boolean_2", false, Expiry.FOREVER) // explicit expiry set
        assertEquals(Expiry.FOREVER, storage.getExpiry("expired_boolean_2"))

        storage.putBoolean("session_boolean", false)
        assertEquals(Expiry.SESSION, storage.getExpiry("session_boolean"))
        storage.putBoolean("forever_boolean", false)
        assertEquals(Expiry.FOREVER, storage.getExpiry("forever_boolean"))
    }

    @Test
    fun testSessionExpiry() = runBlocking {
        storage.putBoolean("session_item_1", true, Expiry.SESSION)
        storage.putString("session_item_2", "string", Expiry.SESSION)
        storage.putDouble("forever_item", Double.MAX_VALUE, Expiry.FOREVER)
        storage.putLong("timed_item", Long.MIN_VALUE, Expiry.afterTimeUnit(10, TimeUnit.MINUTES))

        var allItems = storage.getAll()
        assertTrue(allItems.containsKey("session_item_1"))
        assertTrue(allItems.containsKey("session_item_2"))
        assertTrue(allItems.containsKey("forever_item"))
        assertTrue(allItems.containsKey("timed_item"))

        (storage as PersistentStorage).onNewSession(123L)

        allItems = storage.getAll()
        assertFalse(allItems.containsKey("session_item_1"))
        assertFalse(allItems.containsKey("session_item_2"))
        assertTrue(allItems.containsKey("forever_item"))
        assertTrue(allItems.containsKey("timed_item"))
    }

    @After
    fun tearDown() {
        // clear all.
        dbHelper.writableDatabase.delete("datalayer", null, null)
    }
}