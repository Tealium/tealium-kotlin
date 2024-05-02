package com.tealium.momentsapi

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EngineResponseTest {
    private val validEngineResponseString = """
{
  "audiences": [
    "audience_1",
    "audience_2",
    "audience_3"
  ],
  "badges": [
    "13"
  ],
  "properties": {
    "45": "Android",
    "46": "Android",
    "47": "mobile application",
    "5135": "blue_shoes",
    "24"=true,
    "23"=1714149813216,
    "22"=44.0, 
    "21"=3
  }
}
"""

    @Test
    fun engineResponseDecodeAudiencesSuccess() {
        val json = JSONObject(validEngineResponseString)
        val engineResponse = com.tealium.momentsapi.EngineResponse.fromJson(json)

        assertEquals(engineResponse.audiences?.get(0), "audience_1")
        assertEquals(engineResponse.audiences?.get(1), "audience_2")
        assertEquals(engineResponse.audiences?.get(2), "audience_3")
    }

    @Test
    fun engineResponseDecodeNullAudience() {
        val json = JSONObject(validEngineResponseString)
        val engineResponse = com.tealium.momentsapi.EngineResponse.fromJson(json)

        engineResponse.audiences?.contains("audience_4")?.let { assertFalse(it) }
    }

    @Test
    fun engineResponseDecodeBadgesSuccess() {
        val json = JSONObject(validEngineResponseString)
        val engineResponse = com.tealium.momentsapi.EngineResponse.fromJson(json)

        engineResponse.badges?.contains("13")?.let { assertTrue(it) }
    }

    @Test
    fun engineResponseDecodeNullBadge() {
        val json = JSONObject(validEngineResponseString)
        val engineResponse = com.tealium.momentsapi.EngineResponse.fromJson(json)

        engineResponse.badges?.contains("33")?.let { assertFalse(it) }
    }

    @Test
    fun engineResponseDecodePropertiesSuccess() {
        val json = JSONObject(validEngineResponseString)
        val engineResponse = com.tealium.momentsapi.EngineResponse.fromJson(json)

        assertEquals(engineResponse.attributes?.getValue("45"), "Android")
        assertEquals(engineResponse.attributes?.getValue("46"), "Android")
        assertEquals(engineResponse.attributes?.getValue("47"), "mobile application")
        assertEquals(engineResponse.attributes?.getValue("5135"), "blue_shoes")
    }

    @Test
    fun engineResponseDecodeNullProperty() {
        val json = JSONObject(validEngineResponseString)
        val engineResponse = com.tealium.momentsapi.EngineResponse.fromJson(json)

        assertNull(engineResponse.attributes?.get("333"))
    }

    @Test
    fun engineResponseDecodeStringFromAttributes() {
        val json = JSONObject(validEngineResponseString)
        val engineResponse = com.tealium.momentsapi.EngineResponse.fromJson(json)
        val strings = engineResponse.strings

        strings?.isNotEmpty()?.let { assertTrue(it) }
    }

    @Test(expected = Exception::class)
    fun testFromJson_InvalidJsonValue() {
        val jsonString = """
    {
      "properties": {"key1": null},
      "badges": ["badge1", "badge2"]
    }
  """
        val json = JSONObject(jsonString)
        com.tealium.momentsapi.EngineResponse.fromJson(json)
    }

    @Test
    fun testFromJson_ValidTimestamps() {
        val jsonString = """
      {
        "properties": {"dateKey": 1651516800000}
      }
    """
        val json = JSONObject(jsonString)
        val engineResponse = com.tealium.momentsapi.EngineResponse.fromJson(json)
        val dates = engineResponse.dates

        assertNotNull(dates)
        assertEquals(1, dates!!.size)
        assertEquals("dateKey", dates.keys.first())
        assertEquals(1651516800000, dates.values.first())
    }

    @Test
    fun testFromJson_IntAddedToNumbers() {
        val num = 1651516800
        val jsonString = """
      {
        "properties": {"dateKey": $num}
      }
    """
        val json = JSONObject(jsonString)
        val engineResponse = com.tealium.momentsapi.EngineResponse.fromJson(json)
        val numbers = engineResponse.numbers

        assertNull(engineResponse.dates)
        assertNotNull(numbers)
        assertEquals(1, numbers!!.size)
        assertEquals("dateKey", numbers.keys.first())
        assertEquals(num, numbers.values.first().toInt())
    }
}