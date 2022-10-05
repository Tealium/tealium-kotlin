package com.tealium.core

import com.tealium.core.persistence.DataLayer
import com.tealium.core.persistence.VisitorStorage
import com.tealium.core.persistence.sha256
import com.tealium.dispatcher.Dispatch
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VisitorIdProviderTests {

    @MockK
    internal lateinit var dataLayer: DataLayer

    @MockK
    internal lateinit var config: TealiumConfig

    @MockK
    lateinit var onVisitorIdUpdated: (String) -> Unit

    private val defaultVisitorId = "visitor123"
    private val defaultIdentity = "identity123"
    private val visitorIdKey = "known_visitor_id"
    private val dataLayerVisitorId = "data_layer_id"
    private val existingVisitorId = "existing_visitor_id"

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { dataLayer.getString(visitorIdKey) } returns null
        // assume first launch
        every { dataLayer.getString(Dispatch.Keys.TEALIUM_VISITOR_ID) } returns null
        every { dataLayer.putString(Dispatch.Keys.TEALIUM_VISITOR_ID, any(), any()) } just Runs
        every { onVisitorIdUpdated(any()) } just Runs

        mockkObject(Logger)
        every { Logger.dev(any(), any()) } just Runs
        every { Logger.qa(any(), any()) } just Runs
        every { Logger.prod(any(), any()) } just Runs
    }

    @Test
    fun init_MigratesKnownVisitorId_FromDataLayer_When_NoVisitorIdSaved() {
        val visitorStorage = MockVisitorStorage()
        every { dataLayer.getString(Dispatch.Keys.TEALIUM_VISITOR_ID) } returns dataLayerVisitorId

        val visitorIdProvider = createDefaultVisitorIdProvider(visitorStorage = visitorStorage)

        assertEquals(dataLayerVisitorId, visitorIdProvider.currentVisitorId)
        assertEquals(dataLayerVisitorId, visitorStorage.currentVisitorId)
    }

    @Test
    fun init_MigratesKnownVisitorId_FromExistingId_When_NoVisitorIdSaved() {
        val visitorStorage = MockVisitorStorage()
        every { dataLayer.getString(Dispatch.Keys.TEALIUM_VISITOR_ID) } returns null

        val visitorIdProvider = createDefaultVisitorIdProvider(visitorStorage = visitorStorage)

        assertEquals(existingVisitorId, visitorIdProvider.currentVisitorId)
        assertEquals(existingVisitorId, visitorStorage.currentVisitorId)
    }

    @Test
    fun init_MigratesKnownVisitorId_FromConfigExistingId_When_NoVisitorIdSaved() {
        val visitorStorage = MockVisitorStorage()
        every { config.visitorIdentityKey } returns null
        every { dataLayer.getString(Dispatch.Keys.TEALIUM_VISITOR_ID) } returns null
        every { config.existingVisitorId } returns "config_existing_visitor_id"

        val visitorIdProvider =
            VisitorIdProvider(config, visitorStorage, dataLayer, onVisitorIdUpdated)

        assertEquals("config_existing_visitor_id", visitorIdProvider.currentVisitorId)
        assertEquals("config_existing_visitor_id", visitorStorage.currentVisitorId)
    }

    @Test
    fun init_GeneratesNewId_When_NoVisitorIds() {
        every { config.visitorIdentityKey } returns null
        val visitorStorage = MockVisitorStorage()
        every { dataLayer.getString(Dispatch.Keys.TEALIUM_VISITOR_ID) } returns null
        every { config.existingVisitorId } returns null

        val visitorIdProvider =
            VisitorIdProvider(config, visitorStorage, dataLayer, onVisitorIdUpdated)

        assertTrue(visitorIdProvider.currentVisitorId.isNotBlank())
        assertTrue(visitorStorage.currentVisitorId!!.isNotBlank())
    }

    @Test
    fun init_IdentityIsSet_When_PopulatedInDataLayer_AndLinkedWhenCurrentIdentity_IsNull() {
        val visitorStorage = MockVisitorStorage(
            initialVisitorId = defaultVisitorId,
            initialIdentity = null
        )
        every { dataLayer.getString(visitorIdKey) } returns "newIdentity"
        val visitorIdProvider = createDefaultVisitorIdProvider(visitorStorage = visitorStorage)

        assertEquals(defaultVisitorId, visitorIdProvider.currentVisitorId)
        assertEquals(defaultVisitorId, visitorStorage.currentVisitorId)
        assertEquals("newIdentity".sha256(), visitorStorage.currentIdentity)
        assertEquals(defaultVisitorId, visitorStorage.getVisitorId("newIdentity".sha256()))
        verify(exactly = 0) {
            onVisitorIdUpdated wasNot Called
        }
    }


    @Test
    fun init_IdentityIsSet_When_PopulatedInDataLayer_AndReset_When_CurrentIdentity_IsNotNull() {
        val visitorStorage = MockVisitorStorage(
            initialVisitorId = defaultVisitorId,
            initialIdentity = defaultIdentity
        )
        every { dataLayer.getString(visitorIdKey) } returns "newIdentity"
        val visitorIdProvider = createDefaultVisitorIdProvider(visitorStorage = visitorStorage)

        assertNotEquals(defaultVisitorId, visitorIdProvider.currentVisitorId)
        assertNotEquals(defaultVisitorId, visitorStorage.currentVisitorId)
        assertEquals("newIdentity".sha256(), visitorStorage.currentIdentity)
        assertNotNull(visitorStorage.getVisitorId("newIdentity".sha256()))
        assertNotEquals(defaultVisitorId, visitorStorage.getVisitorId("newIdentity".sha256()))
        verify(exactly = 1) {
            onVisitorIdUpdated(any())
        }
    }

    @Test
    fun init_visitorIdKey_TakenFromConfig() {
        val visitorStorage = MockVisitorStorage(
            initialVisitorId = defaultVisitorId,
            initialIdentity = defaultIdentity
        )
        every { dataLayer.getString("different_key") } returns null
        every { config.visitorIdentityKey } returns "different_key"
        every { config.existingVisitorId } returns null
        val visitorIdProvider =
            VisitorIdProvider(config, visitorStorage, dataLayer, onVisitorIdUpdated)

        visitorIdProvider.onDataUpdated(visitorIdKey, "newIdentity")

        assertEquals(defaultIdentity.sha256(), visitorStorage.currentIdentity)
        assertEquals(defaultVisitorId, visitorStorage.currentVisitorId)
        verify(exactly = 0) {
            onVisitorIdUpdated wasNot Called
        }
    }

    @Test
    fun identity_IsNotUpdated_When_IncorrectDataKey() {
        val visitorStorage = MockVisitorStorage(
            initialVisitorId = defaultVisitorId,
            initialIdentity = defaultIdentity
        )
        val visitorIdProvider = createDefaultVisitorIdProvider(visitorStorage = visitorStorage)

        visitorIdProvider.onDataUpdated("incorrect_key", "newIdentity")

        assertEquals(defaultIdentity.sha256(), visitorStorage.currentIdentity)
        assertEquals(defaultVisitorId, visitorStorage.currentVisitorId)
        verify(exactly = 0) {
            onVisitorIdUpdated wasNot Called
        }
    }

    @Test
    fun identity_IsNotUpdated_When_BlankOrEmptyIdentity() {
        val visitorStorage = MockVisitorStorage(
            initialVisitorId = defaultVisitorId,
            initialIdentity = defaultIdentity
        )
        val visitorIdProvider = createDefaultVisitorIdProvider(visitorStorage = visitorStorage)

        visitorIdProvider.onDataUpdated(visitorIdKey, "")
        visitorIdProvider.onDataUpdated(visitorIdKey, "   ")

        assertEquals(defaultIdentity.sha256(), visitorStorage.currentIdentity)
        assertEquals(defaultVisitorId, visitorStorage.currentVisitorId)
        verify(exactly = 0) {
            onVisitorIdUpdated wasNot Called
        }
    }

    @Test
    fun identity_IsUpdated_When_CorrectDataKey_AndResetsVisitorId_WhenIdentityChanged() {
        val visitorStorage = MockVisitorStorage(
            initialVisitorId = defaultVisitorId,
            initialIdentity = defaultIdentity
        )
        val visitorIdProvider = createDefaultVisitorIdProvider(visitorStorage = visitorStorage)

        visitorIdProvider.onDataUpdated(visitorIdKey, "newIdentity")

        assertEquals("newIdentity".sha256(), visitorStorage.currentIdentity)
        assertNotEquals(defaultVisitorId, visitorStorage.currentVisitorId)
        verify {
            onVisitorIdUpdated(neq(defaultVisitorId))
        }
    }

    @Test
    fun identity_IsSet_When_CorrectDataKey_AndLinksVisitorId_WhenIdentityUnknown() {
        val visitorStorage = MockVisitorStorage(
            initialVisitorId = defaultVisitorId,
            initialIdentity = null
        )
        val visitorIdProvider = createDefaultVisitorIdProvider(visitorStorage = visitorStorage)

        visitorIdProvider.onDataUpdated(visitorIdKey, "newIdentity")

        assertEquals("newIdentity".sha256(), visitorStorage.currentIdentity)
        assertEquals(defaultVisitorId, visitorStorage.getVisitorId("newIdentity".sha256()))
        assertEquals(defaultVisitorId, visitorIdProvider.currentVisitorId)
    }

    @Test
    fun identity_IsSet_When_CorrectDataKey_AndLinksVisitorId_WhenIdentitySeenBefore() {
        val visitorStorage = MockVisitorStorage(
            initialVisitorId = defaultVisitorId,
            initialIdentity = null,
            initialVisitorMap = mapOf("newIdentity" to "linked_visitor_id")
        )
        val visitorIdProvider = createDefaultVisitorIdProvider(visitorStorage = visitorStorage)

        visitorIdProvider.onDataUpdated(visitorIdKey, "newIdentity")

        assertEquals("newIdentity".sha256(), visitorStorage.currentIdentity)
        assertEquals("linked_visitor_id", visitorIdProvider.currentVisitorId)
        assertEquals("linked_visitor_id", visitorStorage.currentVisitorId)
        assertEquals("linked_visitor_id", visitorStorage.getVisitorId("newIdentity".sha256()))
    }

    @Test
    fun resetVisitorId_ResetsCurrentVisitorId() {
        val visitorStorage = MockVisitorStorage(
            initialVisitorId = defaultVisitorId
        )
        val visitorIdProvider = createDefaultVisitorIdProvider(visitorStorage = visitorStorage)
        assertEquals(defaultVisitorId, visitorIdProvider.currentVisitorId)

        visitorIdProvider.resetVisitorId()

        assertNotEquals(defaultVisitorId, visitorIdProvider.currentVisitorId)

        verify {
            onVisitorIdUpdated(any())
        }
    }

    @Test
    fun clearStoredVisitorIds_ResetsCurrentVisitorId() {
        val visitorStorage = MockVisitorStorage(
            initialVisitorId = defaultVisitorId,
            initialIdentity = defaultIdentity,
            initialVisitorMap = mapOf(defaultIdentity to defaultVisitorId)
        )
        val visitorIdProvider = createDefaultVisitorIdProvider(visitorStorage = visitorStorage)
        assertEquals(defaultVisitorId, visitorIdProvider.currentVisitorId)

        visitorIdProvider.clearStoredVisitorIds()

        assertNotNull(visitorStorage.currentVisitorId)
        assertNotEquals(defaultVisitorId, visitorIdProvider.currentVisitorId)
        assertNotEquals(defaultVisitorId, visitorStorage.currentVisitorId)
    }

    @Test
    fun clearStoredVisitorIds_ResetsCurrentVisitorId_ButStoresIdentityFromDataLayer() {
        val visitorStorage = MockVisitorStorage(
            initialVisitorId = defaultVisitorId,
            initialIdentity = defaultIdentity,
            initialVisitorMap = mapOf(defaultIdentity to defaultVisitorId)
        )
        every { dataLayer.getString(visitorIdKey) } returns defaultIdentity
        val visitorIdProvider = createDefaultVisitorIdProvider(visitorStorage = visitorStorage)
        assertEquals(defaultVisitorId, visitorIdProvider.currentVisitorId)

        visitorIdProvider.clearStoredVisitorIds()

        assertNotNull(visitorStorage.currentVisitorId)
        assertNotEquals(defaultVisitorId, visitorIdProvider.currentVisitorId)
        assertNotEquals(defaultVisitorId, visitorStorage.currentVisitorId)
        assertEquals(defaultIdentity.sha256(), visitorStorage.currentIdentity)
        assertNotNull(visitorStorage.getVisitorId(defaultIdentity.sha256()))
        assertNotEquals(defaultVisitorId, visitorStorage.getVisitorId(defaultIdentity.sha256()))
    }

    private fun createDefaultVisitorIdProvider(
        dataLayer: DataLayer? = null,
        visitorStorage: VisitorStorage,
        onVisitorIdUpdated: ((String) -> Unit)? = null
    ): VisitorIdProvider {
        return createCustomVisitorIdProvider(
            visitorIdKey,
            existingVisitorId,
            dataLayer,
            visitorStorage,
            onVisitorIdUpdated
        )
    }

    private fun createCustomVisitorIdProvider(
        visitorIdKey: String? = null,
        existingVisitorId: String? = null,
        dataLayer: DataLayer? = null,
        visitorStorage: VisitorStorage,
        onVisitorIdUpdated: ((String) -> Unit)? = null
    ): VisitorIdProvider {
        return VisitorIdProvider(
            visitorIdKey = visitorIdKey,
            existingVisitorId = existingVisitorId,
            dataLayer = dataLayer ?: this.dataLayer,
            visitorStorage = visitorStorage,
            onVisitorIdUpdated = onVisitorIdUpdated ?: this.onVisitorIdUpdated
        )
    }

    /**
     * Mock storage that just gets and sets to memory
     */
    private class MockVisitorStorage(
        initialVisitorId: String? = null,
        initialIdentity: String? = null,
        initialVisitorMap: Map<String, String> = mapOf()
    ) : VisitorStorage {

        override var currentVisitorId: String? = initialVisitorId
            set(value) {
                field = value
            }
        override var currentIdentity: String? = initialIdentity?.sha256()
            set(value) {
                field = value
            }
        private val _visitorMap = initialVisitorMap.mapKeys { (k, _) -> k.sha256() }.toMutableMap()

        override fun getVisitorId(identity: String): String? {
            return _visitorMap[identity]
        }

        override fun saveVisitorId(identity: String, visitorId: String) {
            _visitorMap[identity] = visitorId
        }

        override fun clear() {
            _visitorMap.clear()
            currentVisitorId = null
            currentIdentity = null
        }
    }
}