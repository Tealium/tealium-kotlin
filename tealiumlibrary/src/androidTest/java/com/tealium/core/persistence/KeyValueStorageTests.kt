package com.tealium.core.persistence

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.Exception
import java.util.concurrent.TimeUnit

class KeyValueStorageTests {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var storage: DataLayer

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = TealiumConfig(context as Application, "test", "test", Environment.DEV)
        dbHelper = DatabaseHelper(config, null) // in-memory

        storage = PersistentStorage(dbHelper, "datalayer")
    }

    @Test
    fun putString_StoresValue() {
        storage.putString("string_key", "string_value")
        storage.putString("string_key_volatile", "string_value", Expiry.UNTIL_RESTART)

        assertTrue(storage.contains("string_key"))
        assertTrue(storage.keys().contains("string_key"))
        assertEquals("string_value", storage.getString("string_key"))
        assertTrue(storage.contains("string_key_volatile"))
        assertTrue(storage.keys().contains("string_key_volatile"))
        assertEquals("string_value", storage.getString("string_key_volatile"))
    }

    @Test
    fun putInt_StoresValue() {
        storage.putInt("int_key", 10)
        storage.putInt("int_key_volatile", 10, Expiry.UNTIL_RESTART)

        assertTrue(storage.contains("int_key"))
        assertTrue(storage.keys().contains("int_key"))
        assertEquals(10, storage.getInt("int_key"))
        assertTrue(storage.contains("int_key_volatile"))
        assertTrue(storage.keys().contains("int_key_volatile"))
        assertEquals(10, storage.getInt("int_key_volatile"))
    }

    @Test
    fun putDouble_StoresValue() {
        storage.putDouble("double_key", 10.01)
        storage.putDouble("double_key_volatile", 10.01, Expiry.UNTIL_RESTART)

        assertTrue(storage.contains("double_key"))
        assertTrue(storage.keys().contains("double_key"))
        assertEquals(10.01, storage.getDouble("double_key"))
        assertTrue(storage.contains("double_key_volatile"))
        assertTrue(storage.keys().contains("double_key_volatile"))
        assertEquals(10.01, storage.getDouble("double_key_volatile"))
    }

    @Test
    fun putLong_StoresValue() {
        storage.putLong("long_key", 10L)
        storage.putLong("long_key_volatile", 10L, Expiry.UNTIL_RESTART)

        assertTrue(storage.contains("long_key"))
        assertTrue(storage.keys().contains("long_key"))
        assertEquals(10L, storage.getLong("long_key"))
        assertTrue(storage.contains("long_key_volatile"))
        assertTrue(storage.keys().contains("long_key_volatile"))
        assertEquals(10L, storage.getLong("long_key_volatile"))
    }

    @Test
    fun putBoolean_StoresValue() {
        storage.putBoolean("boolean_key_true", true)
        storage.putBoolean("boolean_key_false", false)
        storage.putBoolean("boolean_key_true_volatile", true, Expiry.UNTIL_RESTART)
        storage.putBoolean("boolean_key_false_volatile", false, Expiry.UNTIL_RESTART)

        assertTrue(storage.contains("boolean_key_true"))
        assertTrue(storage.contains("boolean_key_false"))
        assertTrue(storage.keys().contains("boolean_key_true"))
        assertTrue(storage.keys().contains("boolean_key_true"))
        assertEquals(true, storage.getBoolean("boolean_key_true"))
        assertEquals(false, storage.getBoolean("boolean_key_false"))
        assertTrue(storage.contains("boolean_key_true_volatile"))
        assertTrue(storage.contains("boolean_key_false_volatile"))
        assertTrue(storage.keys().contains("boolean_key_true_volatile"))
        assertTrue(storage.keys().contains("boolean_key_true_volatile"))
        assertEquals(true, storage.getBoolean("boolean_key_true_volatile"))
        assertEquals(false, storage.getBoolean("boolean_key_false_volatile"))
    }

    @Test
    fun putStringArray_StoresValue() {
        storage.putStringArray("string_array_key", arrayOf("string_1", "string_2"))
        storage.putStringArray(
            "string_array_key_volatile",
            arrayOf("string_1", "string_2"),
            Expiry.UNTIL_RESTART
        )

        assertTrue(storage.contains("string_array_key"))
        assertTrue(storage.keys().contains("string_array_key"))
        assertArrayEquals(
            arrayOf("string_1", "string_2"),
            storage.getStringArray("string_array_key")
        )
        assertTrue(storage.contains("string_array_key_volatile"))
        assertTrue(storage.keys().contains("string_array_key_volatile"))
        assertArrayEquals(
            arrayOf("string_1", "string_2"),
            storage.getStringArray("string_array_key_volatile")
        )
    }

    @Test
    fun putIntArray_StoresValue() {
        storage.putIntArray("int_array_key", arrayOf(1, 2, 3))
        storage.putIntArray("int_array_key_volatile", arrayOf(1, 2, 3), Expiry.UNTIL_RESTART)

        assertTrue(storage.contains("int_array_key"))
        assertTrue(storage.keys().contains("int_array_key"))
        assertArrayEquals(arrayOf(1, 2, 3), storage.getIntArray("int_array_key"))
        assertTrue(storage.contains("int_array_key_volatile"))
        assertTrue(storage.keys().contains("int_array_key_volatile"))
        assertArrayEquals(arrayOf(1, 2, 3), storage.getIntArray("int_array_key_volatile"))
    }

    @Test
    fun putLongArray_StoresValue() {
        storage.putLongArray("long_array_key", arrayOf(1L, 2L, 3L))
        storage.putLongArray("long_array_key_volatile", arrayOf(1L, 2L, 3L), Expiry.UNTIL_RESTART)

        assertTrue(storage.contains("long_array_key"))
        assertTrue(storage.keys().contains("long_array_key"))
        assertArrayEquals(arrayOf(1L, 2L, 3L), storage.getLongArray("long_array_key"))
        assertTrue(storage.contains("long_array_key_volatile"))
        assertTrue(storage.keys().contains("long_array_key_volatile"))
        assertArrayEquals(arrayOf(1L, 2L, 3L), storage.getLongArray("long_array_key_volatile"))
    }

    @Test
    fun putDoubleArray_StoresValue() {
        storage.putDoubleArray("double_array_key", arrayOf(1.1, 2.2, 3.3))
        storage.putDoubleArray(
            "double_array_key_volatile",
            arrayOf(1.1, 2.2, 3.3),
            Expiry.UNTIL_RESTART
        )

        assertTrue(storage.contains("double_array_key"))
        assertTrue(storage.keys().contains("double_array_key"))
        assertArrayEquals(arrayOf(1.1, 2.2, 3.3), storage.getDoubleArray("double_array_key"))
        assertTrue(storage.contains("double_array_key_volatile"))
        assertTrue(storage.keys().contains("double_array_key_volatile"))
        assertArrayEquals(
            arrayOf(1.1, 2.2, 3.3),
            storage.getDoubleArray("double_array_key_volatile")
        )
    }

    @Test
    fun putBooleanArray_StoresValue() {
        storage.putBooleanArray("boolean_array_key", arrayOf(true, false, true))
        storage.putBooleanArray(
            "boolean_array_key_volatile",
            arrayOf(true, false, true),
            Expiry.UNTIL_RESTART
        )

        assertTrue(storage.contains("boolean_array_key"))
        assertTrue(storage.keys().contains("boolean_array_key"))
        assertArrayEquals(arrayOf(true, false, true), storage.getBooleanArray("boolean_array_key"))
        assertTrue(storage.contains("boolean_array_key_volatile"))
        assertTrue(storage.keys().contains("boolean_array_key_volatile"))
        assertArrayEquals(
            arrayOf(true, false, true),
            storage.getBooleanArray("boolean_array_key_volatile")
        )
    }

    @Test
    fun putJsonObject_StoresValue() {
        val emptyJsonObject = JSONObject()
        val filledJsonObject =
            JSONObject("{\"key\":\"value\",\"nested\":{\"nested_key\":\"nested_value\"}}")

        storage.putJsonObject("empty_json_object", emptyJsonObject)
        storage.putJsonObject("filled_json_object", filledJsonObject)
        storage.putJsonObject("empty_json_object_volatile", emptyJsonObject, Expiry.UNTIL_RESTART)
        storage.putJsonObject("filled_json_object_volatile", filledJsonObject, Expiry.UNTIL_RESTART)

        assertTrue(storage.contains("empty_json_object"))
        assertTrue(storage.contains("filled_json_object"))
        assertTrue(storage.keys().contains("empty_json_object"))
        assertTrue(storage.keys().contains("filled_json_object"))
        assertEquals(
            emptyJsonObject.toString(),
            storage.getJsonObject("empty_json_object").toString()
        )
        assertEquals(
            filledJsonObject.toString(),
            storage.getJsonObject("filled_json_object").toString()
        )
        assertTrue(storage.contains("empty_json_object_volatile"))
        assertTrue(storage.contains("filled_json_object_volatile"))
        assertTrue(storage.keys().contains("empty_json_object_volatile"))
        assertTrue(storage.keys().contains("filled_json_object_volatile"))
        assertEquals(
            emptyJsonObject.toString(),
            storage.getJsonObject("empty_json_object_volatile").toString()
        )
        assertEquals(
            filledJsonObject.toString(),
            storage.getJsonObject("filled_json_object_volatile").toString()
        )
    }

    @Test
    fun contains_ReturnsCorrectValue() {
        storage.putString("string", "String", Expiry.FOREVER)
        storage.putInt("int", 1, Expiry.UNTIL_RESTART)
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

        assertFalse(storage.contains("missing_key"))
    }

    @Test
    fun keys_ContainsAllKeys() {
        storage.putString("string", "String", Expiry.FOREVER)
        storage.putInt("int", 1, Expiry.UNTIL_RESTART)
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

        assertTrue(
            storage.keys().containsAll(
                listOf(
                    "string", "int", "double", "long", "boolean", "json",
                    "string_array", "int_array", "double_array", "long_array", "boolean_array"
                )
            )
        )
    }

    @Test
    fun keys_Count() {
        storage.putString("string", "String", Expiry.FOREVER)
        storage.putInt("int", 1, Expiry.UNTIL_RESTART)
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
    fun getAll_ReturnsAllData() {
        storage.putString("string", "String", Expiry.FOREVER)
        storage.putInt("int", 1, Expiry.UNTIL_RESTART)
        storage.putDouble("double", 2.0)
        storage.putLong("long", 2L)
        storage.putBoolean("boolean", true)
        storage.putJsonObject("json_obj", JSONObject())
        storage.putJsonArray("json_arr", JSONArray())
        storage.putStringArray("string_array", arrayOf("String"))
        storage.putIntArray("int_array", arrayOf(1))
        storage.putDoubleArray("double_array", arrayOf(2.0))
        storage.putLongArray("long_array", arrayOf(2L))
        storage.putBooleanArray("boolean_array", arrayOf(true))

        val map = storage.all()
        assertEquals("String", map["string"])
        assertEquals(1, map["int"])
        assertEquals(2.0, map["double"])
        assertEquals(2L, map["long"])
        assertEquals(true, map["boolean"])
        assertEquals(JSONObject().toString(), (map["json_obj"] as JSONObject).toString())
        assertEquals(JSONArray().toString(), (map["json_arr"] as JSONArray).toString())
        assertArrayEquals(arrayOf("String"), map["string_array"] as Array<*>)
        assertArrayEquals(arrayOf(1), map["int_array"] as Array<*>)
        assertArrayEquals(arrayOf(2.0), map["double_array"] as Array<*>)
        assertArrayEquals(arrayOf(2L), map["long_array"] as Array<*>)
        assertArrayEquals(arrayOf(true), map["boolean_array"] as Array<*>)
    }

    @Test
    fun get_ReturnsDataOfCorrectType() {
        storage.putString("string", "String", Expiry.FOREVER)
        storage.putInt("int", 1, Expiry.UNTIL_RESTART)
        storage.putDouble("double", 2.0)
        storage.putLong("long", 2L)
        storage.putBoolean("boolean", true)
        storage.putJsonObject("json_obj", JSONObject())
        storage.putJsonArray("json_arr", JSONArray())
        storage.putStringArray("string_array", arrayOf("String"))
        storage.putIntArray("int_array", arrayOf(1))
        storage.putDoubleArray("double_array", arrayOf(2.0))
        storage.putLongArray("long_array", arrayOf(2L))
        storage.putBooleanArray("boolean_array", arrayOf(true))

        assertEquals("String", storage.get("string"))
        assertTrue(storage.get("string") is String)
        assertEquals(1, storage.get("int"))
        assertTrue(storage.get("int") is Int)
        assertEquals(2.0, storage.get("double"))
        assertTrue(storage.get("double") is Double)
        assertEquals(2L, storage.get("long"))
        assertTrue(storage.get("long") is Long)
        assertEquals(true, storage.get("boolean"))
        assertTrue(storage.get("boolean") is Boolean)
        assertEquals(JSONObject().toString(), (storage.get("json_obj") as JSONObject).toString())
        assertTrue(storage.get("json_obj") is JSONObject)
        assertEquals(JSONArray().toString(), (storage.get("json_arr") as JSONArray).toString())
        assertTrue(storage.get("json_arr") is JSONArray)
        assertArrayEquals(arrayOf("String"), storage.get("string_array") as Array<*>)
        assertArrayEquals(arrayOf(1), storage.get("int_array") as Array<*>)
        assertArrayEquals(arrayOf(2.0), storage.get("double_array") as Array<*>)
        assertArrayEquals(arrayOf(2L), storage.get("long_array") as Array<*>)
        assertArrayEquals(arrayOf(true), storage.get("boolean_array") as Array<*>)
    }

    @Test
    fun put_OverwritesExistingValue() {
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
    fun remove_RemovesData() {
        storage.putString("string", "string 1")
        assertEquals("string 1", storage.getString("string"))

        storage.remove("string")
        assertNull(storage.getString("string"))

        try {
            storage.remove("missing")
        } catch (ignored: Exception) {
            Assert.fail()
        }
    }

    @Test
    fun get_ReturnsNull_ForIncorrectType() {
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
    fun get_ReturnsNull_ForIncorrectVolatileType() {
        // keys that dont exist
        assertNull(storage.getString("string"))

        // keys that do exist but the type is wrong.
        storage.putString("string", "string value", Expiry.UNTIL_RESTART)
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
    fun get_DoesNotReturnExpiredData() {
        val timestamp = System.currentTimeMillis() / 1000
        storage.putBoolean("non_expired_boolean", true, Expiry.afterEpochTime(timestamp + 100))
        storage.putBoolean("expired_boolean_1", true, Expiry.afterEpochTime(timestamp - 100))
        storage.putBoolean("expired_boolean_2", true, Expiry.afterEpochTime(timestamp - 100))
        storage.putBoolean("session_boolean", true, Expiry.SESSION)
        storage.putBoolean("forever_boolean", true, Expiry.FOREVER)

        // storage shouldn't retrieve expired items
        assertNull(storage.getBoolean("expired_boolean_1"))
        assertNull(storage.getBoolean("expired_boolean_2"))

        val map = storage.all()
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
        assertEquals(
            Expiry.afterEpochTime(timestamp + 100),
            storage.getExpiry("non_expired_boolean")
        )
        // overwriting an EXPIRED item SHOULD update the expiry time
        storage.putBoolean(
            "expired_boolean_1",
            false
        ) // no explicit expiry set, should save the default
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
    fun onNewSession_RemovesAllSessionData() = runBlocking {
        storage.putBoolean("session_item_1", true, Expiry.SESSION)
        storage.putString("session_item_2", "string", Expiry.SESSION)
        storage.putDouble("forever_item", Double.MAX_VALUE, Expiry.FOREVER)
        storage.putLong("timed_item", Long.MIN_VALUE, Expiry.afterTimeUnit(10, TimeUnit.MINUTES))
        storage.putLong("restart_item", Long.MIN_VALUE, Expiry.UNTIL_RESTART)

        var allItems = storage.all()
        assertTrue(allItems.containsKey("session_item_1"))
        assertTrue(allItems.containsKey("session_item_2"))
        assertTrue(allItems.containsKey("forever_item"))
        assertTrue(allItems.containsKey("timed_item"))

        (storage as PersistentStorage).onNewSession(123L)

        allItems = storage.all()
        assertFalse(allItems.containsKey("session_item_1"))
        assertFalse(allItems.containsKey("session_item_2"))
        assertTrue(allItems.containsKey("forever_item"))
        assertTrue(allItems.containsKey("timed_item"))
        assertTrue(allItems.containsKey("restart_item"))
    }

    @Test
    fun untilRestart_StoresAndRetrievesData() {
        storage.putString("until_restart_string", "string", Expiry.UNTIL_RESTART)
        storage.putInt("until_restart_int", 10, Expiry.UNTIL_RESTART)
        storage.putDouble("until_restart_double", 10.1, Expiry.UNTIL_RESTART)
        storage.putLong("until_restart_long", 100L, Expiry.UNTIL_RESTART)
        storage.putBoolean("until_restart_boolean", true, Expiry.UNTIL_RESTART)
        storage.putStringArray(
            "until_restart_string_array",
            arrayOf("string", "string_1"),
            Expiry.UNTIL_RESTART
        )
        storage.putIntArray("until_restart_int_array", arrayOf(10, 11, 12), Expiry.UNTIL_RESTART)
        storage.putDoubleArray(
            "until_restart_double_array",
            arrayOf(10.1, 11.1, 12.1),
            Expiry.UNTIL_RESTART
        )
        storage.putLongArray(
            "until_restart_long_array",
            arrayOf(100L, 200L, 300L),
            Expiry.UNTIL_RESTART
        )
        storage.putBooleanArray(
            "until_restart_boolean_array",
            arrayOf(true, false, true),
            Expiry.UNTIL_RESTART
        )
        val json_obj = JSONObject("{\"key\":\"value\"}")
        storage.putJsonObject("until_restart_json_obj", json_obj, Expiry.UNTIL_RESTART)

        val json_arr = JSONArray("[\"key\",\"value\"]")
        storage.putJsonArray("until_restart_json_arr", json_arr, Expiry.UNTIL_RESTART)

        assertEquals("string", storage.get("until_restart_string"))
        assertEquals(10, storage.get("until_restart_int"))
        assertEquals(10.1, storage.get("until_restart_double"))
        assertEquals(100L, storage.get("until_restart_long"))
        assertEquals(true, storage.get("until_restart_boolean"))
        assertArrayEquals(
            arrayOf("string", "string_1"),
            storage.getStringArray("until_restart_string_array")
        )
        assertArrayEquals(arrayOf(10, 11, 12), storage.getIntArray("until_restart_int_array"))
        assertArrayEquals(
            arrayOf(10.1, 11.1, 12.1),
            storage.getDoubleArray("until_restart_double_array")
        )
        assertArrayEquals(
            arrayOf(100L, 200L, 300L),
            storage.getLongArray("until_restart_long_array")
        )
        assertArrayEquals(
            arrayOf(true, false, true),
            storage.getBooleanArray("until_restart_boolean_array")
        )
        assertEquals(json_obj.toString(), (storage.get("until_restart_json_obj") as JSONObject).toString())
        assertEquals(json_arr.toString(), (storage.get("until_restart_json_arr") as JSONArray).toString())
    }

    @Test
    fun untilRestart_ErasedOnRestart() {
        // UntilRestart
        storage.putString("until_restart_string", "string", Expiry.UNTIL_RESTART)
        storage.putInt("until_restart_int", 10, Expiry.UNTIL_RESTART)
        // Session
        storage.putString("session_string", "string", Expiry.SESSION)
        storage.putInt("session_int", 10, Expiry.SESSION)

        assertEquals("string", storage.get("until_restart_string"))
        assertEquals(10, storage.get("until_restart_int"))
        assertEquals("string", storage.get("session_string"))
        assertEquals(10, storage.get("session_int"))

        storage = PersistentStorage(dbHelper, "datalayer")
        // Until Restart
        assertNull(storage.get("until_restart_string"))
        assertNull(storage.get("until_restart_int"))
        // Session
        assertEquals("string", storage.get("session_string"))
        assertEquals(10, storage.get("session_int"))
    }

    @Test
    fun untilRestart_LatestExpiryWins() {
        // UntilRestart
        storage.putString("string", "string", Expiry.SESSION)
        assertEquals("string", storage.get("string"))
        assertEquals(Expiry.SESSION, storage.getExpiry("string"))

        storage.putString("string", "string", Expiry.UNTIL_RESTART)
        assertEquals("string", storage.get("string"))
        assertEquals(Expiry.UNTIL_RESTART, storage.getExpiry("string"))

        storage = PersistentStorage(dbHelper, "datalayer")
        assertNull(storage.get("string"))
    }

    @Test
    fun untilRestart_LatestExpiryWins_OrderReversed() {
        storage.putString("string", "string", Expiry.UNTIL_RESTART)
        assertEquals("string", storage.get("string"))
        assertEquals(Expiry.UNTIL_RESTART, storage.getExpiry("string"))

        // UntilRestart
        storage.putString("string", "string", Expiry.SESSION)
        assertEquals("string", storage.get("string"))
        assertEquals(Expiry.SESSION, storage.getExpiry("string"))

        storage = PersistentStorage(dbHelper, "datalayer")
        assertEquals("string", storage.get("string"))
    }

    @Test
    fun untilRestart_Keys_ContainsMergedKeys() {
        storage.putString("restart", "string", Expiry.UNTIL_RESTART)
        storage.putString("session", "string", Expiry.SESSION)
        storage.putString("forever", "string", Expiry.FOREVER)

        val keys = storage.keys();
        assertTrue(keys.contains("restart"))
        assertTrue(keys.contains("session"))
        assertTrue(keys.contains("forever"))
    }

    @Test
    fun untilRestart_GetAll_ContainsMergedData() {
        storage.putString("restart", "string", Expiry.UNTIL_RESTART)
        storage.putString("session", "string", Expiry.SESSION)
        storage.putString("forever", "string", Expiry.FOREVER)

        val data = storage.all()
        assertEquals("string", data["restart"])
        assertEquals("string", data["session"])
        assertEquals("string", data["forever"])
    }

    @Test
    fun untilRestart_Remove_RemovesAllExpiries() {
        storage.putString("restart", "string", Expiry.UNTIL_RESTART)
        storage.putString("session", "string", Expiry.SESSION)
        storage.putString("forever", "string", Expiry.FOREVER)
        assertEquals("string", storage.getString("restart"))
        assertEquals("string", storage.getString("session"))
        assertEquals("string", storage.getString("forever"))

        storage.remove("restart")
        storage.remove("session")
        storage.remove("forever")

        assertNull(storage.getString("restart"))
        assertNull(storage.getString("session"))
        assertNull(storage.getString("forever"))
    }

    @Test
    fun untilRestart_Clear_RemovesAll() {
        storage.putString("restart", "string", Expiry.UNTIL_RESTART)
        storage.putString("session", "string", Expiry.SESSION)
        storage.putString("forever", "string", Expiry.FOREVER)

        storage.clear()

        assertNull(storage.getString("restart"))
        assertNull(storage.getString("session"))
        assertNull(storage.getString("forever"))
    }

    @After
    fun tearDown() {
        // clear all.
        dbHelper.writableDatabase.delete("datalayer", null, null)
    }
}