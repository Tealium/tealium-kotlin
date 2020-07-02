package com.tealium.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JsonUtilsTest {

    @Test
    fun jsonForFlatKeysStringValues() {
        val payload = mapOf(
                "key1" to "value1",
                "key2" to "value2")
        val result = JsonUtils.jsonFor(payload)
        assertEquals("value1", result.optString("key1"))
        assertEquals("value2", result.optString("key2"))
    }

    @Test
    fun jsonForFlatKeysIntValues() {
        val payload = mapOf(
                "key1" to 1,
                "key2" to 2)
        val result = JsonUtils.jsonFor(payload)
        assertEquals(1, result.optInt("key1"))
        assertEquals(2, result.optInt("key2"))
    }

    @Test
    fun jsonForFlatKeysDoubleValues() {
        val payload = mapOf(
                "key1" to 1.2,
                "key2" to 3.4)
        val result = JsonUtils.jsonFor(payload)
        assertEquals(1.2, result.optDouble("key1"), 0.1)
        assertEquals(3.4, result.optDouble("key2"), 0.1)
    }
}