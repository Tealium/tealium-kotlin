package com.tealium.visitorservice

import com.tealium.visitorservice.TestUtils.Companion.loadJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CurrentVisitTest {

    @Test
    fun visitorProfileDecodeDatesSuccess() {
        val json = loadJson("valid_profile.json")
        val currentVisit = VisitorProfile.fromJson(json).currentVisit
        assertEquals(1585244808000, currentVisit?.dates?.getValue("11"))
        assertEquals(1585244808000, currentVisit?.dates?.getValue("10"))
    }

    @Test
    fun visitorProfileDecodeFlagsSuccess() {
        val json = loadJson("valid_profile.json")
        val currentVisit = VisitorProfile.fromJson(json).currentVisit
        assertTrue(currentVisit?.booleans?.getValue("14")!!)
        assertTrue(currentVisit?.booleans?.getValue("5122")!!)
    }

    @Test
    fun visitorProfileDecodeFlagListsSuccess() {
        val json = loadJson("valid_profile.json")
        val currentVisit = VisitorProfile.fromJson(json).currentVisit
        val set = currentVisit?.arraysOfBooleans?.get("5124")
        assertTrue(set?.first()!!)
    }

    @Test
    fun visitorProfileDecodeMetricsSuccess() {
        val json = loadJson("valid_profile.json")
        val currentVisit = VisitorProfile.fromJson(json).currentVisit
        assertEquals(4, currentVisit?.numbers?.count())
        assertEquals(0.toDouble(), currentVisit?.numbers?.getValue("12")!!, 0.0)
        assertEquals(1.toDouble(), currentVisit?.numbers?.getValue("7")!!, 0.0)
        assertEquals(0.toDouble(), currentVisit?.numbers?.getValue("80")!!, 0.0)
        assertEquals(5.toDouble(), currentVisit?.numbers?.getValue("5128")!!, 0.0)
    }

    @Test
    fun visitorProfileDecodeMetricListsSuccess() {
        val json = loadJson("valid_profile.json")
        val currentVisit = VisitorProfile.fromJson(json).currentVisit
        val set = currentVisit?.arraysOfNumbers?.get("5130")
        assertEquals(5.0, set?.first()!!, 0.0)
    }

    @Test
    fun visitorProfileDecodeMetricSetsSuccess() {
        val json = loadJson("valid_profile.json")
        val currentVisit = VisitorProfile.fromJson(json).currentVisit
        val set = currentVisit?.tallies?.get("5136")
        assertEquals(1.0, set?.get("blue_shoes")!!, 0.0)
    }

    @Test
    fun visitorProfileDecodePropertiesSuccess() {
        val json = loadJson("valid_profile.json")
        val currentVisit = VisitorProfile.fromJson(json).currentVisit
        assertEquals("Android", currentVisit?.strings?.getValue("45"))
        assertEquals("Android", currentVisit?.strings?.getValue("46"))
        assertEquals("mobile application", currentVisit?.strings?.getValue("47"))
        assertEquals("blue_shoes", currentVisit?.strings?.getValue("5135"))
    }

    @Test
    fun visitorProfileDecodePropertyListsSuccess() {
        val json = loadJson("valid_profile.json")
        val currentVisit = VisitorProfile.fromJson(json).currentVisit
        val propertyList = currentVisit?.arraysOfStrings?.get("8482")
        propertyList?.forEachIndexed { index, value ->
            assertEquals("category ${index + 1}", value)
        }
    }

    @Test
    fun visitorProfileDecodePropertySetsSuccess() {
        val json = loadJson("valid_profile.json")
        val currentVisit = VisitorProfile.fromJson(json).currentVisit
        var set = currentVisit?.setsOfStrings?.get("50")
        assertEquals("Android", set?.first())

        set = currentVisit?.setsOfStrings?.get("51")
        assertEquals("Android", set?.first())

        set = currentVisit?.setsOfStrings?.get("52")
        assertEquals("mobile application", set?.first())
        assertEquals("test", set?.last())
    }
}