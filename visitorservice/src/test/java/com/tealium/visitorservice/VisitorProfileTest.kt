package com.tealium.visitorservice

import com.tealium.visitorservice.TestUtils.Companion.loadJson
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.BufferedReader

@RunWith(RobolectricTestRunner::class)
class VisitorProfileTest {

    @Test
    fun visitorProfileDecodeAudiencesSuccess() {
        val json = loadJson("valid_profile.json")
        val visitorProfile = VisitorProfile.fromJson(json)
        assertEquals(3, visitorProfile.audiences?.count())
        assertEquals(visitorProfile.audiences?.get("audience_1"), "red_shoes")
        assertEquals(visitorProfile.audiences?.get("audience_2"), "test_users")
        assertEquals(visitorProfile.audiences?.get("audience_3"), "likes_cats")
    }

    @Test
    fun visitorProfileDecodeNullAudience() {
        val json = loadJson("valid_profile.json")
        val visitorProfile = VisitorProfile.fromJson(json)
        assertNull(visitorProfile.audiences?.get("audience_123"))
    }

    @Test
    fun visitorProfileDecodeBadgesSuccess() {
        val json = loadJson("valid_profile.json")
        val visitorProfile = VisitorProfile.fromJson(json)
        assertTrue(visitorProfile.badges?.get("32")!!)
    }

    @Test
    fun visitorProfileDecodeNullBadge() {
        val json = loadJson("valid_profile.json")
        val visitorProfile = VisitorProfile.fromJson(json)
        assertNull(visitorProfile.badges?.get("99"))
    }

    @Test
    fun visitorProfileDecodeDatesSuccess() {
        val json = loadJson("valid_profile.json")
        val visitorProfile = VisitorProfile.fromJson(json)
        assertEquals(1585244808000, visitorProfile.dates?.getValue("23"))
    }

    @Test
    fun visitorProfileDecodeNullDate() {
        val json = loadJson("valid_profile.json")
        val visitorProfile = VisitorProfile.fromJson(json)
        assertNull(visitorProfile.dates?.get("999"))
    }

    @Test
    fun visitorProfileDecodeFlagsSuccess() {
        val json = loadJson("valid_profile.json")
        val visitorProfile = VisitorProfile.fromJson(json)
        assertFalse(visitorProfile.booleans?.getValue("13")!!)
        assertFalse(visitorProfile.booleans?.getValue("5122")!!)
    }

    @Test
    fun visitorProfileDecodeNullFlag() {
        val json = loadJson("valid_profile.json")
        val visitorProfile = VisitorProfile.fromJson(json)
        assertNull(visitorProfile.booleans?.get("999"))
    }

    @Test
    fun visitorProfileDecodeFlagListsSuccess() {
        val json = loadJson("valid_profile.json")
        val visitorProfile = VisitorProfile.fromJson(json)
        val array = visitorProfile.arraysOfBooleans?.get("5126")
        assertEquals(1, array?.count())
        assertTrue(array?.first()!!)
    }

    @Test
    fun visitorProfileDecodeNullFlagList() {
        val json = loadJson("valid_profile.json")
        val visitorProfile = VisitorProfile.fromJson(json)
        assertNull(visitorProfile.arraysOfBooleans?.get("9999"))
    }

    @Test
    fun visitorProfileDecodeMetricsSuccess() {
        val json = loadJson("valid_profile.json")
        val visitorProfile = VisitorProfile.fromJson(json)
        assertEquals(5, visitorProfile.numbers?.count())
        assertTrue(visitorProfile.numbers?.containsKey("22")!!)
        assertTrue(visitorProfile.numbers?.containsKey("15")!!)
        assertTrue(visitorProfile.numbers?.containsKey("28")!!)
        assertTrue(visitorProfile.numbers?.containsKey("29")!!)
        assertTrue(visitorProfile.numbers?.containsKey("21")!!)
        visitorProfile.numbers?.forEach { (_, value) ->
            assertEquals(1.toDouble(), value, 0.0)
        }
    }

    @Test
    fun visitorProfileDecodeNullMetric() {
        val json = loadJson("valid_profile.json")
        val visitorProfile = VisitorProfile.fromJson(json)
        assertNull(visitorProfile.numbers?.get("9999"))
    }

    @Test
    fun visitorProfileDecodeMetricListsSuccess() {
        val json = loadJson("valid_profile.json")
        val visitorProfile = VisitorProfile.fromJson(json)
        val array = visitorProfile.arraysOfNumbers?.get("5132")
        assertEquals(1, array?.count())
        assertEquals(5.toDouble(), array?.first()!!, 0.0)
    }

    @Test
    fun visitorProfileDecodeNullMetricLists() {
        val json = loadJson("valid_profile.json")
        val visitorProfile = VisitorProfile.fromJson(json)
        assertNull(visitorProfile.arraysOfNumbers?.get("9999"))
    }

    @Test
    fun visitorProfileDecodeMetricSetsSuccess() {
        val json = loadJson("valid_profile_metrics.json")
        val visitorProfile = VisitorProfile.fromJson(json)
        val set1 = visitorProfile?.tallies?.get("5136")
        assertEquals(1.0, set1?.get("blue_shoes")!!, 0.0)
        assertEquals(5.0, set1?.get("red_shoes")!!, 0.0)
        assertEquals(100.0, set1?.get("yellow_shoes")!!, 0.0)

        val set2 = visitorProfile.tallies?.get("1234")
        assertEquals(100.0, set2?.get("blue_shirts")!!, 0.0)
        assertEquals(-15.0, set2?.get("red_shirts")!!, 0.0)
        assertEquals(100.0, set2?.get("yellow_shirts")!!, 0.0)
    }

    @Test
    fun visitorProfileDecodeNullMetricSet() {
        val json = loadJson("valid_profile_metrics.json")
        val visitorProfile = VisitorProfile.fromJson(json)
        assertNull(visitorProfile?.tallies?.get("9999"))
    }

    @Test
    fun visitorProfileDecodePropertiesSuccess() {
        val json = loadJson("valid_profile.json")
        val visitorProfile = VisitorProfile.fromJson(json)
        assertEquals("kotlin-test", visitorProfile.strings?.getValue("profile"))
        assertEquals("tealiummobile", visitorProfile.strings?.getValue("account"))
    }

    @Test
    fun visitorProfileDecodeNullProperty() {
        val json = loadJson("valid_profile.json")
        val visitorProfile = VisitorProfile.fromJson(json)
        assertNull("tealiummobile", visitorProfile.strings?.get("9999"))
    }

    @Test
    fun visitorProfileDecodePropertyListsSuccess() {
        val json = loadJson("valid_profile.json")
        val visitorProfile = VisitorProfile.fromJson(json)
        val propertyList = visitorProfile.arraysOfStrings?.get("8483")
        propertyList?.forEachIndexed { index, value ->
            assertEquals("category ${index + 1}", value)
        }
    }

    @Test
    fun visitorProfileDecodeNullPropertyList() {
        val json = loadJson("valid_profile.json")
        val visitorProfile = VisitorProfile.fromJson(json)
        assertNull(visitorProfile.arraysOfStrings?.get("9999"))
    }

    @Test
    fun visitorProfileDecodePropertySetsSuccess() {
        val json = loadJson("valid_profile.json")
        val visitorProfile = VisitorProfile.fromJson(json)
        val set1 = visitorProfile.setsOfStrings?.get("49")
        assertEquals("Chrome", set1?.first()!!)
        val set2 = visitorProfile.setsOfStrings?.get("58")
        set2?.forEachIndexed { index, value ->
            assertEquals("${index + 1}", value)
        }
    }

    @Test
    fun visitorProfileDecodeNullPropertySet() {
        val json = loadJson("valid_profile.json")
        val visitorProfile = VisitorProfile.fromJson(json)
        assertNull(visitorProfile.setsOfStrings?.get("9999"))

    }
}

class TestUtils {
    companion object {
        fun loadJson(filename: String): JSONObject {
            val inputStream = this::class.java.classLoader?.getResourceAsStream(filename)
                    ?: throw NullPointerException("Test resource missing. Are you sure you included $filename in the resources directory?")
            val content = inputStream.bufferedReader().use(BufferedReader::readText)
            return JSONObject(content)
        }
    }
}