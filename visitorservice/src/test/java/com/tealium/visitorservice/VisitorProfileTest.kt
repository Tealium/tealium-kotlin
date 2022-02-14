package com.tealium.visitorservice

import com.tealium.visitorservice.TestUtils.Companion.loadJson
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.BufferedReader

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21, 29])
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
        assertEquals(6, visitorProfile.numbers?.count())
        assertEquals(1.0, visitorProfile.numbers?.get("22")!!, 0.0)
        assertEquals(1.0, visitorProfile.numbers?.get("15")!!, 0.0)
        assertEquals(1.0, visitorProfile.numbers?.get("28")!!, 0.0)
        assertEquals(1.0, visitorProfile.numbers?.get("29")!!, 0.0)
        assertEquals(1.0, visitorProfile.numbers?.get("21")!!, 0.0)
        assertEquals(1585244808000.0, visitorProfile.numbers?.get("23")!!, 0.0)
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
        assertEquals(2, array?.count())
        assertEquals(5.toDouble(), array?.first()!!, 0.0)
        assertEquals(1585244808000.toDouble(), array?.last()!!, 0.0)
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
        assertEquals(1585244808000.0, set1?.get("long_yellow_shoes")!!, 0.0)

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

    @Test
    fun toDouble_AcceptsInts() {
        val double: Double? = VisitorProfile.toDouble(100)

        assertEquals(100.0, double!!, 0.5)
    }

    @Test
    fun toDouble_AcceptsDoubles() {
        val double = VisitorProfile.toDouble(100.5)

        assertEquals(100.5, double!!, 0.05)
    }

    @Test
    fun toDouble_AcceptsLongs() {
        val double = VisitorProfile.toDouble(100L)

        assertEquals(100.0, double!!, 0.0)
    }

    @Test
    fun toDouble_ReturnsNull() {
        val double = VisitorProfile.toDouble("not-a-number")

        assertNull(double)
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