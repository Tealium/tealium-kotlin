package com.tealium.core.persistence

import com.tealium.core.persistence.DefaultVisitorStorage.Companion.KEY_CURRENT_IDENTITY
import com.tealium.core.persistence.DefaultVisitorStorage.Companion.KEY_VISITOR_ID_RECONCILED
import com.tealium.dispatcher.Dispatch
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.*

class VisitorStorageTests {

    @RelaxedMockK
    internal lateinit var storage: PersistentStorageDao

    private lateinit var visitorStorage: VisitorStorage

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        visitorStorage = DefaultVisitorStorage(
            storage
        )
    }

    @Test
    fun currentVisitorId_ReturnsSavedVisitorId() {
        every { storage.get(Dispatch.Keys.TEALIUM_VISITOR_ID) } returns PersistentItem(
            key = Dispatch.Keys.TEALIUM_VISITOR_ID,
            value = "saved_visitor_id",
            type = Serialization.STRING
        )

        assertEquals("saved_visitor_id", visitorStorage.currentVisitorId)
    }

    @Test
    fun currentVisitorId_ReturnsNull_WhenNoVisitorIdSaved() {
        every { storage.get(Dispatch.Keys.TEALIUM_VISITOR_ID) } returns null

        assertNull(visitorStorage.currentVisitorId)
    }

    @Test
    fun currentIdentity_ReturnsSavedIdentity() {
        every { storage.get(KEY_CURRENT_IDENTITY) } returns PersistentItem(
            key = KEY_CURRENT_IDENTITY,
            value = "saved_identity",
            type = Serialization.STRING
        )

        assertEquals("saved_identity", visitorStorage.currentIdentity)
    }

    @Test
    fun currentIdentity_ReturnsNull_WhenNoIdentitySaved() {
        every { storage.get(KEY_CURRENT_IDENTITY) } returns null

        assertNull(visitorStorage.currentIdentity)
    }

    @Test
    fun isVisitorIdReconciled_DefaultsToFalse_AndPersistsTrue_WhenSet() {
        every { storage.get(KEY_VISITOR_ID_RECONCILED) } returns null
        assertFalse(visitorStorage.isVisitorIdReconciled)

        visitorStorage.isVisitorIdReconciled = true

        verify {
            storage.upsert(match {
                it.key == KEY_VISITOR_ID_RECONCILED && it.value == "true"
            })
        }
        every { storage.get(KEY_VISITOR_ID_RECONCILED) } returns PersistentItem(
            key = KEY_VISITOR_ID_RECONCILED,
            value = "true",
            type = Serialization.STRING
        )
        assertTrue(visitorStorage.isVisitorIdReconciled)
    }

    @Test
    fun getVisitorId_ReturnsSavedVisitorId() {
        val identity = "identity123"
        every { storage.get(identity) } returns PersistentItem(
            key = Dispatch.Keys.TEALIUM_VISITOR_ID,
            value = "saved_visitor_id",
            type = Serialization.STRING
        )

        assertEquals("saved_visitor_id", visitorStorage.getVisitorId(identity))
    }

    @Test
    fun getVisitorId_ReturnsNull_IfNotPresent() {
        val identity = "identity123"
        every { storage.get(any()) } returns null

        assertNull(visitorStorage.getVisitorId(identity + "fail"))
    }
}