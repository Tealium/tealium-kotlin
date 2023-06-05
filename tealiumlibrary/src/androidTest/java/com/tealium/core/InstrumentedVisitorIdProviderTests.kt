package com.tealium.core

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.persistence.DataLayer
import com.tealium.core.persistence.DatabaseHelper
import com.tealium.core.persistence.DefaultVisitorStorage
import com.tealium.core.persistence.PersistentStorage
import com.tealium.core.persistence.VisitorStorage
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InstrumentedVisitorIdProviderTests {
    @MockK
    private lateinit var onVisitorIdUpdated: (String) -> Unit

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var dataLayer: DataLayer
    private lateinit var visitorStorage: VisitorStorage

    private val visitorIdKey = "identity_key"

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = TealiumConfig(context as Application, "test", "test", Environment.DEV)
        dbHelper = DatabaseHelper(config, null) // in-memory
        dbHelper.onCreate(dbHelper.db)
        dbHelper.onUpgrade(dbHelper.db,1, DatabaseHelper.DATABASE_VERSION)

        every { onVisitorIdUpdated(any()) } just Runs

        dataLayer = PersistentStorage(dbHelper, "visitors", eventRouter = mockk(relaxed = true))
        visitorStorage = DefaultVisitorStorage(dbHelper)
    }

    @Test
    fun initial_VisitorId_GetsGenerated() {
        val visitorIdProvider =
            VisitorIdProvider(null, visitorIdKey, visitorStorage, dataLayer, onVisitorIdUpdated)

        assertNotNull(visitorIdProvider.currentVisitorId)
        assertTrue(visitorIdProvider.currentVisitorId.isNotBlank())
        verify(exactly = 1) {
            onVisitorIdUpdated(any())
        }
    }

    @Test
    fun onDataUpdated_VisitorId_GetsLinkedToIdentity() {
        val visitorIdProvider =
            VisitorIdProvider(null, visitorIdKey, visitorStorage, dataLayer, onVisitorIdUpdated)
        val originalVisitorId = visitorIdProvider.currentVisitorId

        visitorIdProvider.onDataUpdated(visitorIdKey, "new_identity")
        val newVisitorId = visitorIdProvider.currentVisitorId

        assertNotNull(newVisitorId)
        assertEquals(originalVisitorId, newVisitorId)
        verify(exactly = 1) {
            onVisitorIdUpdated(any())
        }
    }

    @Test
    fun onDataUpdated_VisitorId_GetsSetFromKnownIdentity() {
        val visitorIdProvider =
            VisitorIdProvider(null, visitorIdKey, visitorStorage, dataLayer, onVisitorIdUpdated)
        val originalId = visitorIdProvider.currentVisitorId

        // Identify original user
        visitorIdProvider.onDataUpdated(visitorIdKey, "new_identity")
        val knownId1 = visitorIdProvider.currentVisitorId

        // Switch identity
        visitorIdProvider.onDataUpdated(visitorIdKey, "another_identity")
        val knownId2 = visitorIdProvider.currentVisitorId

        // Switch Identity Back
        visitorIdProvider.onDataUpdated(visitorIdKey, "new_identity")
        val knownId1Reverted =visitorIdProvider .currentVisitorId

        // Switch back again
        visitorIdProvider.onDataUpdated(visitorIdKey, "another_identity")
        val knownId2Reverted = visitorIdProvider.currentVisitorId

        assertEquals(originalId, knownId1)
        // Switch
        assertNotEquals(knownId1, knownId2)
        // Switch back
        assertEquals(knownId1, knownId1Reverted)
        // Switch back again
        assertEquals(knownId2, knownId2Reverted)
        verify(exactly = 4) {
            onVisitorIdUpdated(any())
        }
    }

}