package com.tealium.core.dispatcher

import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.GenericDispatch
import com.tealium.dispatcher.TealiumEvent
import com.tealium.dispatcher.TealiumView
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GenericDispatchTests {

    private lateinit var view: TealiumView
    private lateinit var event: TealiumEvent

    private lateinit var genericView: GenericDispatch
    private lateinit var genericEvent: GenericDispatch

    @Before
    fun setUp() {
        view = TealiumView("view_name")
        event = TealiumEvent("event_name")

        genericView = GenericDispatch(view)
        genericEvent = GenericDispatch(event)
    }

    @After
    fun tearDown() {

    }

    @Test
    fun create_IdAndTimestampAreEqualButNotSame() {
        assertEquals(view.id, genericView.id)
        assertEquals(event.id, genericEvent.id)
        assertEquals(view.timestamp, genericView.timestamp)
        assertEquals(event.timestamp, genericEvent.timestamp)

        genericView.timestamp = 1L
        genericEvent.timestamp = 1L
        assertNotEquals(view.timestamp, genericView.timestamp)
        assertNotEquals(event.timestamp, genericEvent.timestamp)
        assertNotSame(view.timestamp, genericView.timestamp)
        assertNotSame(event.timestamp, genericEvent.timestamp)
    }

    @Test
    fun create_PayloadMapIsNotTheSame() {
        assertNotSame(view.payload(), genericView.payload())
        assertNotSame(event.payload(), genericEvent.payload())
    }

    @Test
    fun create_PayloadUpdate_NewDataDoesNotAffectOriginal() {
        view.addAll(mapOf("new_data_1" to "new_value_1"))
        genericView.addAll(mapOf("new_data_2" to "new_value_2"))

        assertTrue(view.payload().containsKey("new_data_1"))
        assertFalse(genericView.payload().containsKey("new_data_1"))

        assertFalse(view.payload().containsKey("new_data_2"))
        assertTrue(genericView.payload().containsKey("new_data_2"))
    }

    @Test
    fun create_PayloadUpdate_ExistingDataDoesNotAffectOriginal() {
        view.addAll(mapOf(Dispatch.Keys.TEALIUM_EVENT to "new_value_1"))
        assertTrue(view.payload()[Dispatch.Keys.TEALIUM_EVENT] == "new_value_1")
        assertTrue(genericView.payload()[Dispatch.Keys.TEALIUM_EVENT] == "view_name")

        genericView.addAll(mapOf(Dispatch.Keys.TEALIUM_EVENT to "new_value_2"))
        assertTrue(view.payload()[Dispatch.Keys.TEALIUM_EVENT] == "new_value_1")
        assertTrue(genericView.payload()[Dispatch.Keys.TEALIUM_EVENT] == "new_value_2")
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun create_PayloadUpdate_ReferenceObjectChangesDoNotAffectOriginal() {
        val view = TealiumView("view_name", mapOf(
                "map_data" to mutableMapOf("key" to "value"),
                "list_data" to mutableListOf("string1", "string2"),
                "array_data" to arrayOf("string1", "string2")
        ))
        val genericView = GenericDispatch(view)

        (view.payload()["map_data"] as MutableMap<String, Any>)["key"] = "new value"
        assertEquals("value", (genericView.payload()["map_data"] as Map<String, Any>)["key"])

        (view.payload()["list_data"] as MutableList<String>)[0] = "new value"
        assertEquals("string1", (genericView.payload()["list_data"] as MutableList<String>)[0])

        (view.payload()["array_data"] as Array<String>)[0] = "new value"
        assertEquals("string1", (genericView.payload()["array_data"] as Array<String>)[0])
    }

    @Test
    fun create_PayloadUpdate_ReferenceObjectReplacementsDoNotAffectOriginal() {
        val map = mutableMapOf("key" to "value")
        val list = mutableListOf("string1", "string2")
        val array = arrayOf("string1", "string2")

        val view = TealiumView("view_name", mapOf(
                "map_data" to map,
                "list_data" to list,
                "array_data" to array
        ))
        val genericView = GenericDispatch(view)

        genericView.addAll(mapOf("map_data" to mapOf("new_key" to "new value")))
        assertNotSame(genericView.payload()["map_data"], view.payload()["map_data"])

        genericView.addAll(mapOf("list_data" to listOf("string3")))
        assertNotSame(genericView.payload()["list_data"], view.payload()["list_data"])

        genericView.addAll(mapOf("array_data" to arrayOf("string3")))
        assertNotSame(genericView.payload()["array_data"], view.payload()["array_data"])
    }
}