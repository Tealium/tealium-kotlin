package com.tealium.core.persistence

import android.os.Build
import junit.framework.Assert.assertEquals
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class SerializationTests  {

    private lateinit var item: PersistentItem<*>
    private val key = "key"
    private val expiry: Expiry? = null
    private val timestamp: Long = getTimestamp()

    @Test
    fun testPersistentString() {
        val value = "string value"
        item = PersistentString(key, value, expiry, timestamp)

        assertEquals(key, item.key)
        assertEquals(value, item.serialize())
        assertEquals(timestamp, item.timestamp)
    }

    @Test
    fun testPersistentInt() {
        val value = 100
        item = PersistentInt(key, value, expiry, timestamp)

        assertEquals(key, item.key)
        assertEquals("100", item.serialize())
        assertEquals(value, PersistentInt.deserialize("100"))
        assertEquals(timestamp, item.timestamp)
    }

    @Test
    fun testPersistentLong() {
        val value = 100L
        item = PersistentLong(key, value, expiry, timestamp)

        assertEquals(key, item.key)
        assertEquals("100", item.serialize())
        assertEquals(value, PersistentLong.deserialize("100"))
        assertEquals(timestamp, item.timestamp)
    }


    @Test
    fun testPersistentDouble() {
        val value = 100.1
        item = PersistentDouble(key, value, expiry, timestamp)

        assertEquals(key, item.key)
        assertEquals("100.1", item.serialize())
        assertEquals(value, PersistentDouble.deserialize("100.1"))
        assertEquals(timestamp, item.timestamp)
    }

    @Test
    fun testPersistentBoolean() {
        item = PersistentBoolean(key, true, expiry, timestamp)

        assertEquals(key, item.key)
        assertEquals("1", item.serialize())
        assertEquals(true, PersistentBoolean.deserialize("1"))
        assertEquals(timestamp, item.timestamp)

        item = PersistentBoolean(key, false, expiry, timestamp)

        assertEquals(key, item.key)
        assertEquals("0", item.serialize())
        assertEquals(false, PersistentBoolean.deserialize("0"))
        assertEquals(timestamp, item.timestamp)
    }

    @Test
    fun testPersistentIntArray() {
        val value = arrayOf(10, 20, 30)
        item = PersistentIntArray(key, value, expiry, timestamp)

        assertEquals(key, item.key)
        assertEquals("[10,20,30]", item.serialize())
        assertArrayEquals(value, PersistentIntArray.deserialize("[10,20,30]"))
        assertEquals(timestamp, item.timestamp)
    }

    @Test
    fun testPersistentLongArray() {
        val value = arrayOf(10L, 20L, 30L)
        item = PersistentLongArray(key, value, expiry, timestamp)

        assertEquals(key, item.key)
        assertEquals("[10,20,30]", item.serialize())
        assertArrayEquals(value, PersistentLongArray.deserialize("[10,20,30]"))
        assertEquals(timestamp, item.timestamp)
    }


    @Test
    fun testPersistentDoubleArray() {
        val value = arrayOf(10.1, 20.2, 30.3)
        item = PersistentDoubleArray(key, value, expiry, timestamp)

        assertEquals(key, item.key)
        assertEquals("[10.1,20.2,30.3]", item.serialize())
        assertArrayEquals(value, PersistentDoubleArray.deserialize("[10.1,20.2,30.3]"))
        assertEquals(timestamp, item.timestamp)
    }

    @Test
    fun testPersistentBooleanArray() {
        val value = arrayOf(true, false, true)
        item = PersistentBooleanArray(key, value, expiry, timestamp)

        assertEquals(key, item.key)
        assertEquals("[true,false,true]", item.serialize())
        assertArrayEquals(value, PersistentBooleanArray.deserialize("[true,false,true]"))
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
        item = PersistentJsonObject(key, value, expiry, timestamp)

        assertEquals(key, item.key)
        assertEquals(jsonString, item.serialize())
        assertEquals(value.toString(), PersistentJsonObject.deserialize(jsonString).toString())
        assertEquals(timestamp, item.timestamp)
    }
}