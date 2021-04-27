package com.tealium.core.dispatcher

import TealiumCollectorConstants.TEALIUM_ACCOUNT
import TealiumCollectorConstants.TEALIUM_ENVIRONMENT
import TealiumCollectorConstants.TEALIUM_PROFILE
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

            assertEquals("account", it.shared[TEALIUM_ACCOUNT])
            assertEquals("profile", it.shared[TEALIUM_PROFILE])
            assertEquals("dev", it.shared[TEALIUM_ENVIRONMENT])

            batch.events.forEachIndexed { index, e ->
                // no compression on shared objects at this time
                assertEquals("shared_value_1", e["shared_key_1"])
                assertEquals("shared_value_2", e["shared_key_2"])
                assertEquals("unique_value_$index", e["shared_key"])
                assertEquals("unique_value_$index", e["unique_key_$index"])

                assertNull(e[TEALIUM_ACCOUNT])
                assertNull(e[TEALIUM_PROFILE])
                assertNull(e[TEALIUM_ENVIRONMENT])
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

            data[TEALIUM_ACCOUNT] = "account"
            data[TEALIUM_PROFILE] = "profile"
            data[TEALIUM_ENVIRONMENT] = "dev"

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