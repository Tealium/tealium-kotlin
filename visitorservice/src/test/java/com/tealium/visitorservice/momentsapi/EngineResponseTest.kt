package com.tealium.visitorservice.momentsapi

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EngineResponseTest {
    val validEngineResponseString = """
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
    "5135": "blue_shoes"
  }
}
"""

    @Test
    fun engineResponseDecodeAudiencesSuccess() {
        val json = JSONObject(validEngineResponseString)
        val engineResponse = EngineResponse.fromJson(json)

        assertEquals(engineResponse.audiences?.get(0), "audience_1")
        assertEquals(engineResponse.audiences?.get(1), "audience_2")
        assertEquals(engineResponse.audiences?.get(2), "audience_3")
    }

    @Test
    fun engineResponseDecodeNullAudience() {
        val json = JSONObject(validEngineResponseString)
        val engineResponse = EngineResponse.fromJson(json)

        engineResponse.audiences?.contains("audience_4")?.let { assertFalse(it) }
    }

    @Test
    fun engineResponseDecodeBadgesSuccess() {
        val json = JSONObject(validEngineResponseString)
        val engineResponse = EngineResponse.fromJson(json)

        engineResponse.badges?.contains("13")?.let { assertTrue(it) }
    }

    @Test
    fun engineResponseDecodeNullBadge() {
        val json = JSONObject(validEngineResponseString)
        val engineResponse = EngineResponse.fromJson(json)

        engineResponse.badges?.contains("33")?.let { assertFalse(it) }
    }

    @Test
    fun engineResponseDecodePropertiesSuccess() {
        val json = JSONObject(validEngineResponseString)
        val engineResponse = EngineResponse.fromJson(json)

        assertEquals(engineResponse.properties?.getValue("45"), "Android")
        assertEquals(engineResponse.properties?.getValue("46"), "Android")
        assertEquals(engineResponse.properties?.getValue("47"), "mobile application")
        assertEquals(engineResponse.properties?.getValue("5135"), "blue_shoes")
    }

    @Test
    fun engineResponseDecodeNullProperty() {
        val json = JSONObject(validEngineResponseString)
        val engineResponse = EngineResponse.fromJson(json)

        assertNull(engineResponse.properties?.get("333"))
    }
}