package com.tealium.core

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class JsonUtilsTest {

    @Test
    fun jsonForFlatKeysStringValues() {
        val payload = mapOf(
            "key1" to "value1",
            "key2" to "value2"
        )
        val result = JsonUtils.jsonFor(payload)
        assertEquals("value1", result.optString("key1"))
        assertEquals("value2", result.optString("key2"))
    }

    @Test
    fun jsonForFlatKeysIntValues() {
        val payload = mapOf(
            "key1" to 1,
            "key2" to 2
        )
        val result = JsonUtils.jsonFor(payload)
        assertEquals(1, result.optInt("key1"))
        assertEquals(2, result.optInt("key2"))
    }

    @Test
    fun jsonForFlatKeysDoubleValues() {
        val payload = mapOf(
            "key1" to 1.2,
            "key2" to 3.4
        )
        val result = JsonUtils.jsonFor(payload)
        assertEquals(1.2, result.optDouble("key1"), 0.1)
        assertEquals(3.4, result.optDouble("key2"), 0.1)
    }

    @Test
    fun jsonForArraysOfMaps() {
        val payload = mapOf(
            "array" to arrayOf(
                mapOf("mapKey1" to "mapValue1"),
                mapOf("mapKey2" to "mapValue2")
            )
        )
        val result = JsonUtils.jsonFor(payload)
        val jsonArray = result.getJSONArray("array")
        assertNotNull(jsonArray)
        assertTrue(jsonArray.get(0) is JSONObject)
        assertTrue(jsonArray.get(1) is JSONObject)

        val jsonObj1 = jsonArray.getJSONObject(0)
        val jsonObj2 = jsonArray.getJSONObject(1)
        assertEquals("mapValue1", jsonObj1["mapKey1"])
        assertEquals("mapValue2", jsonObj2["mapKey2"])
    }

    @Test
    fun jsonForListsOfMaps() {
        val payload = mapOf(
            "list" to listOf(
                mapOf("mapKey1" to "mapValue1"),
                mapOf("mapKey2" to "mapValue2")
            )
        )
        val result = JsonUtils.jsonFor(payload)
        val jsonArray = result.getJSONArray("list")
        assertNotNull(jsonArray)
        assertTrue(jsonArray.get(0) is JSONObject)
        assertTrue(jsonArray.get(1) is JSONObject)

        val jsonObj1 = jsonArray.getJSONObject(0)
        val jsonObj2 = jsonArray.getJSONObject(1)
        assertEquals("mapValue1", jsonObj1["mapKey1"])
        assertEquals("mapValue2", jsonObj2["mapKey2"])
    }

    @Test
    fun jsonForSetsOfMaps() {
        val payload = mapOf(
            "set" to setOf(
                mapOf("mapKey1" to "mapValue1"),
                mapOf("mapKey2" to "mapValue2")
            )
        )
        val result = JsonUtils.jsonFor(payload)
        val jsonArray = result.getJSONArray("set")
        assertNotNull(jsonArray)
        assertTrue(jsonArray.get(0) is JSONObject)
        assertTrue(jsonArray.get(1) is JSONObject)

        val jsonObj1 = jsonArray.getJSONObject(0)
        val jsonObj2 = jsonArray.getJSONObject(1)
        assertEquals("mapValue1", jsonObj1["mapKey1"])
        assertEquals("mapValue2", jsonObj2["mapKey2"])
    }

    @Test
    fun jsonForArraysOfMapsOfArrays() {
        val payload = mapOf(
            "array" to arrayOf(
                mapOf("mapKey1" to arrayOf("1", "2", "3")),
                mapOf("mapKey2" to listOf(1, 2, 3))
            )
        )
        val result = JsonUtils.jsonFor(payload)
        val jsonArray = result.getJSONArray("array")
        assertNotNull(jsonArray)
        assertTrue(jsonArray.get(0) is JSONObject)
        assertTrue(jsonArray.get(1) is JSONObject)

        val jsonObj1 = jsonArray.getJSONObject(0)
        assertTrue(jsonObj1.get("mapKey1") is JSONArray)
        val jsonArray1 = jsonObj1.getJSONArray("mapKey1")
        assertEquals("1", jsonArray1.get(0))
        assertEquals("2", jsonArray1.get(1))
        assertEquals("3", jsonArray1.get(2))

        val jsonObj2 = jsonArray.getJSONObject(1)
        assertTrue(jsonArray.get(1) is JSONObject)
        val jsonArray2 = jsonObj2.getJSONArray("mapKey2")
        assertEquals(1, jsonArray2.get(0))
        assertEquals(2, jsonArray2.get(1))
        assertEquals(3, jsonArray2.get(2))
    }

    @Test
    fun tryParseReturnsValidJsonObject() {
        val jsonLibrarySettings = "{\n" +
                "  \"collect_dispatcher\": false,\n" +
                "  \"tag_management_dispatcher\": true,\n" +
                "  \"batching\": {\n" +
                "    \"batch_size\": 10,\n" +
                "    \"max_queue_size\": 999,\n" +
                "    \"expiration\": \"1d\"\n" +
                "  },\n" +
                "  \"battery_saver\": true,\n" +
                "  \"wifi_only\": false,\n" +
                "  \"refresh_interval\": \"15m\",\n" +
                "  \"log_level\": \"dev\",\n" +
                "  \"disable_library\": false\n" +
                "}"

        val jsonObject = JsonUtils.tryParse(jsonLibrarySettings)
        assertNotNull(jsonObject)
        assertEquals(false, jsonObject!!.getBoolean("collect_dispatcher"))
    }

    @Test
    fun tryParseReturnsNullOnInvalidJsonObjectButDoesNotThrow() {
        val malformedJsonLibrarySettings = "{\n" +
                "  \"collect_dispatcher\": false,\n" +
                "  \"tag_management_dispatcher\": true,\n" +
                "  \"batching\": {\n" +
                "    \"batch_size\": 10,\n" +
                "    \"max_queue_size\": 999,\n" +
                "    \"expiration\": \"1d\"\n" +
                "  },\n" +
                "  \"battery_saver\": true,\n" +
                "  \"wifi_only\": false,\n" +
                "  \"refresh_interval\": \"15m\",\n" +
                "  \"log_level\": \"dev\",\n" +
                "  \"disable_library\": false\n" // missing closing brace

        val jsonObject = JsonUtils.tryParse(malformedJsonLibrarySettings)
        assertNull(jsonObject)
    }
}