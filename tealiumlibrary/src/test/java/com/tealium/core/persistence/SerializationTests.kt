package com.tealium.core.persistence

import android.os.Build
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class SerializationTests {

    private lateinit var item: PersistentItem
    private val key = "key"
    private val expiry: Expiry? = null
    private val timestamp: Long = getTimestamp()

    @Test
    fun testPersistentString() {
        val value = "string value"
        item = PersistentItem(key, value, expiry, timestamp, Serialization.STRING)

        assertEquals(key, item.key)
        assertEquals(value, Serdes.stringSerde().deserializer.deserialize(item.value))
        assertEquals(timestamp, item.timestamp)
    }

    @Test
    fun testPersistentInt() {
        val value = 100
        item = PersistentItem(
            key,
            Serdes.intSerde().serializer.serialize(value),
            expiry,
            timestamp,
            Serialization.INT
        )

        assertEquals(key, item.key)
        assertEquals("100", item.value)
        assertEquals(value, Serdes.intSerde().deserializer.deserialize("100"))
        assertEquals(timestamp, item.timestamp)
    }

    @Test
    fun testPersistentLong() {
        val value = 100L
        item = PersistentItem(
            key,
            Serdes.longSerde().serializer.serialize(value),
            expiry,
            timestamp,
            Serialization.LONG
        )

        assertEquals(key, item.key)
        assertEquals("100", item.value)
        assertEquals(value, Serdes.longSerde().deserializer.deserialize("100"))
        assertEquals(timestamp, item.timestamp)
    }

    @Test
    fun testPersistentDouble() {
        val value = 100.1
        item = PersistentItem(
            key,
            Serdes.doubleSerde().serializer.serialize(value),
            expiry,
            timestamp,
            Serialization.DOUBLE
        )

        assertEquals(key, item.key)
        assertEquals("100.1", item.value)
        assertEquals(value, Serdes.doubleSerde().deserializer.deserialize("100.1"), 0.0)
        assertEquals(timestamp, item.timestamp)
    }

    @Test
    fun testPersistentBoolean() {
        item = PersistentItem(
            key,
            Serdes.booleanSerde().serializer.serialize(true),
            expiry,
            timestamp,
            Serialization.BOOLEAN
        )

        assertEquals(key, item.key)
        assertEquals("1", item.value)
        assertEquals(true, Serdes.booleanSerde().deserializer.deserialize("1"))
        assertEquals(timestamp, item.timestamp)

        item = PersistentItem(
            key,
            Serdes.booleanSerde().serializer.serialize(false),
            expiry,
            timestamp,
            Serialization.BOOLEAN
        )

        assertEquals(key, item.key)
        assertEquals("0", item.value)
        assertEquals(false, Serdes.booleanSerde().deserializer.deserialize("0"))
        assertEquals(timestamp, item.timestamp)
    }

    @Test
    fun testPersistentIntArray() {
        val value = arrayOf(10, 20, 30)
        item = PersistentItem(
            key,
            Serdes.intArraySerde().serializer.serialize(value),
            expiry,
            timestamp,
            Serialization.INT_ARRAY
        )

        assertEquals(key, item.key)
        assertEquals("[10,20,30]", item.value)
        assertArrayEquals(value, Serdes.intArraySerde().deserializer.deserialize("[10,20,30]"))
        assertEquals(timestamp, item.timestamp)
    }

    @Test
    fun testPersistentLongArray() {
        val value = arrayOf(10L, 20L, 30L)
        item = PersistentItem(
            key,
            Serdes.longArraySerde().serializer.serialize(value),
            expiry,
            timestamp,
            Serialization.LONG_ARRAY
        )

        assertEquals(key, item.key)
        assertEquals("[10,20,30]", item.value)
        assertArrayEquals(value, Serdes.longArraySerde().deserializer.deserialize("[10,20,30]"))
        assertEquals(timestamp, item.timestamp)
    }


    @Test
    fun testPersistentDoubleArray() {
        val value = arrayOf(10.1, 20.2, 30.3)
        item = PersistentItem(
            key,
            Serdes.doubleArraySerde().serializer.serialize(value),
            expiry,
            timestamp,
            Serialization.DOUBLE_ARRAY
        )

        assertEquals(key, item.key)
        assertEquals("[10.1,20.2,30.3]", item.value)
        assertArrayEquals(
            value,
            Serdes.doubleArraySerde().deserializer.deserialize("[10.1,20.2,30.3]")
        )
        assertEquals(timestamp, item.timestamp)
    }

    @Test
    fun testPersistentBooleanArray() {
        val value = arrayOf(true, false, true)
        item = PersistentItem(
            key,
            Serdes.booleanArraySerde().serializer.serialize(value),
            expiry,
            timestamp,
            Serialization.BOOLEAN_ARRAY
        )

        assertEquals(key, item.key)
        assertEquals("[true,false,true]", item.value)
        assertArrayEquals(
            value,
            Serdes.booleanArraySerde().deserializer.deserialize("[true,false,true]")
        )
        assertEquals(timestamp, item.timestamp)
    }

    @Test
    fun testPersistentJsonObject() {
        val value = JSONObject()
        value.put("string", "value")
        value.put("integer", 10)
        value.put("double", 10.1)
        value.put("boolean", true)
        val jsonString = """
            {
                "string": "value",
                "integer": 10,
                "double": 10.1,
                "boolean": true
            }
        """.trimIndent().replace(" ", "").replace("\n", "")
        item = PersistentItem(
            key,
            Serdes.jsonObjectSerde().serializer.serialize(value),
            expiry,
            timestamp,
            Serialization.JSON_OBJECT
        )

        assertEquals(key, item.key)
        assertEquals(jsonString, item.value)
        assertEquals(
            value.toString(),
            Serdes.jsonObjectSerde().deserializer.deserialize(jsonString).toString()
        )
        assertEquals(timestamp, item.timestamp)
    }
}