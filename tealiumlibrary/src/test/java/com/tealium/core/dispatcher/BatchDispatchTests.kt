package com.tealium.core.dispatcher

import com.tealium.dispatcher.BatchDispatch
import com.tealium.dispatcher.BatchDispatch.Companion.KEY_EVENTS
import com.tealium.dispatcher.BatchDispatch.Companion.KEY_SHARED
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.TealiumEvent
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BatchDispatchTests {

    @Test
    fun testPayloadExtraction() {
        val batch = BatchDispatch.create(createMultipleDispatches(5))
        batch?.let {
            assertTrue(it.shared.isNotEmpty())
            assertEquals(5, it.events.count())

            assertEquals("account", it.shared[Dispatch.Keys.TEALIUM_ACCOUNT])
            assertEquals("profile", it.shared[Dispatch.Keys.TEALIUM_PROFILE])
            assertEquals("dev", it.shared[Dispatch.Keys.TEALIUM_ENVIRONMENT])

            batch.events.forEachIndexed { index, e ->
                // no compression on shared objects at this time
                assertEquals("shared_value_1", e["shared_key_1"])
                assertEquals("shared_value_2", e["shared_key_2"])
                assertEquals("unique_value_$index", e["shared_key"])
                assertEquals("unique_value_$index", e["unique_key_$index"])

                assertNull(e[Dispatch.Keys.TEALIUM_ACCOUNT])
                assertNull(e[Dispatch.Keys.TEALIUM_PROFILE])
                assertNull(e[Dispatch.Keys.TEALIUM_ENVIRONMENT])
            }
        } ?: Assert.fail()
    }

    @Test
    fun testPayloadContents() {
        val batch = BatchDispatch.create(createMultipleDispatches(5))
        batch?.let {
            assertTrue(it.payload().containsKey(KEY_SHARED))
            assertNotNull(it.payload()[KEY_SHARED])

            assertTrue(it.payload().containsKey(KEY_EVENTS))
            assertNotNull(it.payload()[KEY_EVENTS])
        } ?: Assert.fail()
    }

    @Test
    fun testCreation() {
        assertNotNull(BatchDispatch.create(createMultipleDispatches(10)))
        assertNull(BatchDispatch.create(createMultipleDispatches(0)))
    }

    fun createMultipleDispatches(count: Int): List<Dispatch> {
        val dispatches = mutableListOf<Dispatch>()
        repeat(count) {
            val data = mutableMapOf<String, Any>()

            data[Dispatch.Keys.TEALIUM_ACCOUNT] = "account"
            data[Dispatch.Keys.TEALIUM_PROFILE] = "profile"
            data[Dispatch.Keys.TEALIUM_ENVIRONMENT] = "dev"

            // shared keys
            data["shared_key_1"] = "shared_value_1"
            data["shared_key_2"] = "shared_value_2"

            // unique keys
            data["shared_key"] = "unique_value_$it" // shared key but unique values.
            data["unique_key_$it"] = "unique_value_$it" // unique keys, unique values.

            dispatches.add(TealiumEvent("event_$it", data))
        }

        return dispatches
    }
}