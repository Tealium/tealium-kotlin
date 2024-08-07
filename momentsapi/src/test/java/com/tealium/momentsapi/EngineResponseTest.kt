package com.tealium.momentsapi

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EngineResponseTest {
    private val validEngineResponseString = """
{
   "audiences":[
      "audience_1",
      "audience_2",
      "audience_3"
   ],
   "badges":[
      "13",
      "24"
   ],
   "properties":{
      "45":"Android",
      "46":"mobile application",
      "5135":"blue_shoes"
   },
   "metrics":{
      "22":21.0
   },
   "dates":{
      "23":1714426723413
   },
   "flags":{
      "27":true
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

        engineResponse.audiences?.let {
            assertFalse(it.contains("audience_4"))
        }
    }

    @Test
    fun engineResponseDecodeBadgesSuccess() {
        val json = JSONObject(validEngineResponseString)
        val engineResponse = EngineResponse.fromJson(json)

        assertEquals(engineResponse.badges?.get(0), "13")
    }

    @Test
    fun engineResponseDecodeNullBadge() {
        val json = JSONObject(validEngineResponseString)
        val engineResponse = EngineResponse.fromJson(json)

        engineResponse.badges?.let {
            assertFalse(it.contains("33"))
        }
    }

    @Test
    fun engineResponseDecodePropertiesSuccess() {
        val json = JSONObject(validEngineResponseString)
        val engineResponse = EngineResponse.fromJson(json)

        assertEquals(engineResponse.strings?.getValue("45"), "Android")
        assertEquals(engineResponse.strings?.getValue("46"), "mobile application")
        assertEquals(engineResponse.strings?.getValue("5135"), "blue_shoes")
    }

    @Test
    fun engineResponseDecodeNullProperty() {
        val json = JSONObject(validEngineResponseString)
        val engineResponse = EngineResponse.fromJson(json)

        assertNull(engineResponse.strings?.get("333"))
    }

    @Test
    fun engineResponseDecodeDatesSuccess() {
        val json = JSONObject(validEngineResponseString)
        val engineResponse = EngineResponse.fromJson(json)

        assertEquals(engineResponse.dates?.getValue("23"), 1714426723413)
    }

    @Test
    fun engineResponseDecodeNullDate() {
        val json = JSONObject(validEngineResponseString)
        val engineResponse = EngineResponse.fromJson(json)

        assertNull(engineResponse.dates?.get("32"))
    }

    @Test
    fun engineResponseDecodeBooleanSuccess() {
        val json = JSONObject(validEngineResponseString)
        val engineResponse = EngineResponse.fromJson(json)

        assertEquals(engineResponse.booleans?.getValue("27"), true)
    }

    @Test
    fun engineResponseDecodeNullBoolean() {
        val json = JSONObject(validEngineResponseString)
        val engineResponse = EngineResponse.fromJson(json)

        assertNull(engineResponse.dates?.get("72"))
    }

    @Test
    fun engineResponseDecodeNumbersSuccess() {
        val json = JSONObject(validEngineResponseString)
        val engineResponse = EngineResponse.fromJson(json)

        assertEquals(engineResponse.numbers?.getValue("22"), 21.0)
    }

    @Test
    fun engineResponseDecodeNullNumbers() {
        val json = JSONObject(validEngineResponseString)
        val engineResponse = EngineResponse.fromJson(json)

        assertNull(engineResponse.numbers?.get("32"))
    }


}