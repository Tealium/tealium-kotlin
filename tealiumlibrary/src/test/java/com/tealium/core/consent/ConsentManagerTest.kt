package com.tealium.core.consent

import android.app.Application
import android.content.SharedPreferences
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.consent.ConsentManagerConstants.KEY_CATEGORIES
import com.tealium.core.consent.ConsentManagerConstants.KEY_LAST_STATUS_UPDATE
import com.tealium.core.consent.ConsentManagerConstants.KEY_STATUS
import com.tealium.core.messaging.EventRouter
import com.tealium.core.network.HttpClient
import com.tealium.core.settings.LibrarySettings
import com.tealium.dispatcher.AuditEvent
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.TealiumEvent
import com.tealium.dispatcher.TealiumView
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class ConsentManagerTest {

    @MockK
    lateinit var context: Application

    @MockK
    lateinit var mockFile: File

    @RelaxedMockK
    lateinit var sharedPreferences: SharedPreferences

    @RelaxedMockK
    lateinit var editor: SharedPreferences.Editor

    @MockK
    lateinit var mockTealiumContext: TealiumContext

    @RelaxedMockK
    lateinit var mockHttpClient: HttpClient

    @RelaxedMockK
    lateinit var eventRouter: EventRouter

    @RelaxedMockK
    lateinit var mockPolicy: ConsentManagementPolicy

    lateinit var consentManager: ConsentManager
    lateinit var config: TealiumConfig

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { context.filesDir } returns mockFile
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.apply() } just Runs

        every { sharedPreferences.getString(KEY_STATUS, "unknown") } returns "unknown"
        every { sharedPreferences.getStringSet(KEY_CATEGORIES, null) } returns null
        every { editor.putString(KEY_STATUS, "unknown") } returns editor
        every { editor.remove(any()) } returns editor

        config = TealiumConfig(context, "test", "profile12345", Environment.QA)
        every { mockTealiumContext.config } returns config
        every { mockTealiumContext.visitorId } returns "visitor1234567890"
        every { mockTealiumContext.httpClient } returns mockHttpClient
        every { mockTealiumContext.track(any()) } just Runs
        config.consentExpiry = ConsentExpiry(1, TimeUnit.MINUTES)
        consentManager = ConsentManager(mockTealiumContext, eventRouter, mockk())
    }

    @Test
    fun consentManagerModuleDisabledByDefault() {
        assertFalse(consentManager.enabled)
    }

    @Test
    fun consentManagerConfigEnabledChecksConsentPolicy() {
        assertFalse(config.consentManagerEnabled!!)
        config.consentManagerPolicy = ConsentPolicy.CCPA
        assertTrue(config.consentManagerEnabled!!)

        // Backwards compatability
        config.consentManagerEnabled = true
        assertTrue(config.consentManagerEnabled!!)
        config.consentManagerEnabled = false
        assertFalse(config.consentManagerEnabled!!)
    }

    @Test
    fun consentManagerStatusDefaultsUnknown() {
        assertEquals(ConsentStatus.UNKNOWN, consentManager.userConsentStatus)
    }

    @Test
    fun consentManagerStatusDefaultNullConsentCategories() {
        assertNull(consentManager.userConsentCategories)
    }

    @Test
    fun consentManagerShouldQueueDefaultsToFalse() {
        assertFalse(consentManager.shouldQueue(mockk(relaxed = true)))
    }

    @Test
    fun consentManagerShouldDropDefaultsToFalse() {
        assertFalse(consentManager.shouldDrop(mockk(relaxed = true)))
    }

    @Test
    fun consentManagerNotConsentedNullConsentCategories() {
        every { editor.putString(KEY_STATUS, "notConsented") } returns editor
        every { sharedPreferences.getString(any(), any()) } returns "notConsented"
        every { editor.putStringSet(KEY_CATEGORIES, null) } returns editor
        every { editor.remove(any()) } returns editor
        consentManager.userConsentStatus = ConsentStatus.NOT_CONSENTED
        assertEquals(ConsentStatus.NOT_CONSENTED, consentManager.userConsentStatus)
        assertNull(consentManager.userConsentCategories)
    }

    @Test
    fun consentManagerSetSingleCategorySetsConsented() {
        every { editor.putString(KEY_STATUS, "consented") } returns editor
        every { sharedPreferences.getString(any(), any()) } returns "consented"
        every { sharedPreferences.getStringSet(KEY_CATEGORIES, null) } returns setOf("engagement")
        every { editor.putStringSet(KEY_CATEGORIES, setOf("engagement")) } returns editor
        consentManager.userConsentCategories = setOf(ConsentCategory.ENGAGEMENT)
        assertEquals(ConsentStatus.CONSENTED, consentManager.userConsentStatus)
        assertTrue(consentManager.userConsentCategories?.contains(ConsentCategory.ENGAGEMENT)!!)
    }

    @Test
    fun consentManagerSetCategoryNullSetsConsentStatusNotConsented() {
        every { editor.putString(KEY_STATUS, "notConsented") } returns editor
        every { sharedPreferences.getString(any(), any()) } returns "notConsented"
        every { editor.putStringSet(KEY_CATEGORIES, null) } returns editor
        every { editor.remove(any()) } returns editor
        consentManager.userConsentCategories = null
        assertEquals(ConsentStatus.NOT_CONSENTED, consentManager.userConsentStatus)
        assertNull(consentManager.userConsentCategories)
    }

    @Test
    fun consentManagerSetCategoryNullSetsConsentStatusUnknown() {
        every { editor.putString(KEY_STATUS, "unknown") } returns editor
        every { sharedPreferences.getString(any(), any()) } returns "unknown"
        every { editor.putStringSet(KEY_CATEGORIES, null) } returns editor
        every { editor.remove(any()) } returns editor
        consentManager.userConsentStatus = ConsentStatus.UNKNOWN
        assertEquals(ConsentStatus.UNKNOWN, consentManager.userConsentStatus)
        assertNull(consentManager.userConsentCategories)
    }

    @Test
    fun setUserConsentStatusConsentedSetsAllCategories() {
        every { editor.putString(KEY_STATUS, "consented") } returns editor
        every {
            editor.putStringSet(
                KEY_CATEGORIES,
                ConsentCategory.ALL.map { it.value }.toMutableSet()
            )
        } returns editor
        every { sharedPreferences.getString(any(), any()) } returns "consented"
        every {
            sharedPreferences.getStringSet(
                KEY_CATEGORIES,
                null
            )
        } returns ConsentCategory.ALL.map { it.value }.toMutableSet()
        consentManager.userConsentStatus = ConsentStatus.CONSENTED
        assertEquals(ConsentStatus.CONSENTED, consentManager.userConsentStatus)
        assertEquals(15, consentManager.userConsentCategories?.count())
    }

    @Test
    fun resetUserConsentPreferencesSuccess() {
        every { editor.putStringSet(KEY_CATEGORIES, null) } returns editor
        every { editor.remove(any()) } returns editor
        consentManager.reset()
        assertEquals(ConsentStatus.UNKNOWN, consentManager.userConsentStatus)
        assertNull(consentManager.userConsentCategories)
    }

    @Test
    fun userPreferencesUpdateListener_Called_WhenDifferent() {
        every { editor.putString(KEY_STATUS, "consented") } returns editor
        every { sharedPreferences.getString(any(), any()) } returns "consented"
        every { sharedPreferences.getStringSet(KEY_CATEGORIES, null) } returns setOf("crm")
        every { editor.putStringSet(KEY_CATEGORIES, setOf("engagement")) } returns editor
        consentManager =
            ConsentManager(mockTealiumContext, eventRouter, mockk(), ConsentPolicy.GDPR)
        consentManager.userConsentCategories = setOf(ConsentCategory.ENGAGEMENT)

        // no policy == no updates.
        verify(exactly = 1) {
            eventRouter.onUserConsentPreferencesUpdated(any(), any())
        }
    }

    @Test
    fun userPreferencesUpdateListener_NotCalled_WhenSame() {
        every { editor.putString(KEY_STATUS, "consented") } returns editor
        every { sharedPreferences.getString(any(), any()) } returns "consented"
        every { sharedPreferences.getStringSet(KEY_CATEGORIES, null) } returns setOf("engagement")
        every { editor.putStringSet(KEY_CATEGORIES, setOf("engagement")) } returns editor
        consentManager =
            ConsentManager(mockTealiumContext, eventRouter, mockk(), ConsentPolicy.GDPR)
        consentManager.userConsentCategories = setOf(ConsentCategory.ENGAGEMENT)

        // no policy == no updates.
        verify(exactly = 0) {
            eventRouter.onUserConsentPreferencesUpdated(any(), any())
        }
    }

    @Test
    fun userPreferencesUpdateListener_NotCalled() {
        every { editor.putString(KEY_STATUS, "consented") } returns editor
        every { sharedPreferences.getString(any(), any()) } returns "consented"
        every { sharedPreferences.getStringSet(KEY_CATEGORIES, null) } returns setOf("engagement")
        every { editor.putStringSet(KEY_CATEGORIES, setOf("engagement")) } returns editor
        consentManager.userConsentCategories = setOf(ConsentCategory.ENGAGEMENT)

        // no policy == no updates.
        verify(exactly = 0) {
            eventRouter.onUserConsentPreferencesUpdated(any(), any())
        }
    }

    @Test
    fun consentManagerStatusUnknown_DoesNotCollect() = runBlocking {
        every { editor.remove(any()) } returns editor
        consentManager.userConsentStatus = ConsentStatus.UNKNOWN

        val data = consentManager.collect()

        assertEquals(1, data.count())
        assertTrue(data.containsKey(Dispatch.Keys.CONSENT_LAST_UPDATED))
    }

    @Test
    fun consentManagerStatusPartiallyConsented_DoesCollect() = runBlocking {
        val mockSettings: LibrarySettings = mockk()
        every { editor.putString(KEY_STATUS, "consented") } returns editor
        every { sharedPreferences.getString(any(), any()) } returns "consented"
        every { sharedPreferences.getStringSet(KEY_CATEGORIES, null) } returns setOf("affiliates")
        every { editor.putStringSet(KEY_CATEGORIES, setOf("affiliates")) } returns editor
        consentManager =
            ConsentManager(mockTealiumContext, eventRouter, mockSettings, ConsentPolicy.GDPR)
        consentManager.userConsentCategories = setOf(ConsentCategory.AFFILIATES)

        assertEquals(ConsentStatus.CONSENTED, consentManager.userConsentStatus)
        val data = consentManager.collect()
        assertFalse(data.isEmpty())
        val expected = setOf(ConsentCategory.AFFILIATES)
            .map { it.value }
        assertEquals(expected, data["consent_categories"])
    }

    @Test
    fun consentLoggingEnabledSendsAuditEventWhenStatusChanges() {
        config.consentManagerLoggingEnabled = true
        consentManager =
            ConsentManager(mockTealiumContext, eventRouter, LibrarySettings(), ConsentPolicy.GDPR)
        assertEquals(ConsentStatus.UNKNOWN, consentManager.userConsentStatus)

        consentManager.userConsentStatus = ConsentStatus.CONSENTED

        verify {
            mockTealiumContext.track(match { it is AuditEvent })
        }
    }

    @Test
    fun consentLoggingSendsConsentedEventWhenPolicyIsGdprAndStatusConsentedWithAllCategories() {
        config.consentManagerLoggingEnabled = true
        consentManager =
            ConsentManager(mockTealiumContext, eventRouter, LibrarySettings(), ConsentPolicy.GDPR)
        assertEquals(ConsentStatus.UNKNOWN, consentManager.userConsentStatus)

        consentManager.userConsentStatus = ConsentStatus.CONSENTED

        verify {
            mockTealiumContext.track(match {
                it[Dispatch.Keys.TEALIUM_EVENT] == ConsentManagerConstants.GRANT_FULL_CONSENT
            })
        }
    }

    @Test
    fun consentLoggingSendsPartialConsentEventWhenPolicyIsGdprAndStatusConsentedWithSomeCategories() {
        config.consentManagerLoggingEnabled = true
        consentManager =
            ConsentManager(mockTealiumContext, eventRouter, LibrarySettings(), ConsentPolicy.GDPR)
        assertEquals(ConsentStatus.UNKNOWN, consentManager.userConsentStatus)

        consentManager.userConsentCategories = setOf(ConsentCategory.ANALYTICS)

        verify {
            mockTealiumContext.track(match {
                it[Dispatch.Keys.TEALIUM_EVENT] == ConsentManagerConstants.GRANT_PARTIAL_CONSENT
            })
        }
    }

    @Test
    fun consentLoggingSendsDeclineConsentEventWhenPolicyIsGdprAndStatusNotConsented() {
        config.consentManagerLoggingEnabled = true
        consentManager =
            ConsentManager(mockTealiumContext, eventRouter, LibrarySettings(), ConsentPolicy.GDPR)
        assertEquals(ConsentStatus.UNKNOWN, consentManager.userConsentStatus)

        consentManager.userConsentStatus = ConsentStatus.NOT_CONSENTED

        verify {
            mockTealiumContext.track(match {
                it[Dispatch.Keys.TEALIUM_EVENT] == ConsentManagerConstants.DECLINE_CONSENT
            })
        }
    }

    @Test
    fun consentCategoriesKeyEnabled_VerifyPayloadOverrideSuccess() = runBlocking {
        config.overrideConsentCategoriesKey = "custom_consent_categories_key"
        val mockSettings: LibrarySettings = mockk()

        every { editor.putString(KEY_STATUS, "consented") } returns editor
        every { sharedPreferences.getString(any(), any()) } returns "consented"

        consentManager =
            ConsentManager(mockTealiumContext, eventRouter, mockSettings, ConsentPolicy.GDPR)
        consentManager.userConsentStatus = ConsentStatus.CONSENTED

        val payload = consentManager.collect()

        assertTrue(payload.containsKey("custom_consent_categories_key"))
    }

    @Test
    fun consentCategoriesKeyDisabled_VerifyPayloadSuccess() = runBlocking {
        val mockSettings: LibrarySettings = mockk()

        every { editor.putString(KEY_STATUS, "consented") } returns editor
        every { sharedPreferences.getString(any(), any()) } returns "consented"

        consentManager =
            ConsentManager(mockTealiumContext, eventRouter, mockSettings, ConsentPolicy.GDPR)
        consentManager.userConsentStatus = ConsentStatus.CONSENTED

        val payload = consentManager.collect()

        assertTrue(!payload.containsKey("custom_consent_categories_key"))
        assertTrue(payload.containsKey("consent_categories"))
    }

    @Test
    fun consentManagerLastSetDefinedUponConsentStatusChangeConsented() {
        every { editor.putLong(KEY_LAST_STATUS_UPDATE, 1234) } returns editor
        every { sharedPreferences.getLong(any(), any()) } returns 1234

        consentManager.userConsentStatus = ConsentStatus.CONSENTED

        verify(exactly = 1) {
            editor.putLong(KEY_LAST_STATUS_UPDATE, any())
        }
    }

    @Test
    fun consentManagerLastSetDefinedUponConsentStatusChangeNotConsented() {
        every { editor.putLong(KEY_LAST_STATUS_UPDATE, 1234) } returns editor
        every { sharedPreferences.getLong(any(), any()) } returns 1234

        consentManager.userConsentStatus = ConsentStatus.NOT_CONSENTED

        verify(exactly = 1) {
            editor.putLong(KEY_LAST_STATUS_UPDATE, any())
        }
    }

    @Test
    fun consentManagerLastSetAddedToPayload() = runBlocking {
        every { editor.putLong(KEY_LAST_STATUS_UPDATE, 1234) } returns editor
        every { sharedPreferences.getLong(any(), any()) } returns 1234

        consentManager.userConsentStatus = ConsentStatus.CONSENTED
        var payload = consentManager.collect()
        assertTrue(payload.containsKey(Dispatch.Keys.CONSENT_LAST_UPDATED))
        assertEquals(1234L, payload[Dispatch.Keys.CONSENT_LAST_UPDATED])

        consentManager.userConsentStatus = ConsentStatus.NOT_CONSENTED
        payload = consentManager.collect()
        assertTrue(payload.containsKey(Dispatch.Keys.CONSENT_LAST_UPDATED))
        assertEquals(1234L, payload[Dispatch.Keys.CONSENT_LAST_UPDATED])

        consentManager.userConsentStatus = ConsentStatus.UNKNOWN
        payload = consentManager.collect()
        assertTrue(payload.containsKey(Dispatch.Keys.CONSENT_LAST_UPDATED))
        assertEquals(1234L, payload[Dispatch.Keys.CONSENT_LAST_UPDATED])
    }

    @Test
    fun defaultConsentExpiryCCPA() {
        var localConfig = TealiumConfig(context, "test", "profile", Environment.QA)
        localConfig.consentManagerPolicy = ConsentPolicy.CCPA
        every { mockTealiumContext.config } returns localConfig
        val localConsentManager = ConsentManager(mockTealiumContext, eventRouter, mockk())
        assertEquals(395, localConsentManager.expiry.time)
        assertEquals(TimeUnit.DAYS, localConsentManager.expiry.unit)
    }

    @Test
    fun defaultConsentExpiryGDPR() {
        var localConfig = TealiumConfig(context, "test", "profile", Environment.QA)
        localConfig.consentManagerPolicy = ConsentPolicy.GDPR
        every { mockTealiumContext.config } returns localConfig
        val localConsentManager = ConsentManager(mockTealiumContext, eventRouter, mockk())
        assertEquals(365, localConsentManager.expiry.time)
        assertEquals(TimeUnit.DAYS, localConsentManager.expiry.unit)
    }

    @Test
    fun defaultConsentExpiryCustom() {
        var localConfig = TealiumConfig(context, "test", "profile", Environment.QA)
        every { mockPolicy.defaultConsentExpiry } returns ConsentExpiry(10, TimeUnit.MINUTES)
        ConsentPolicy.CUSTOM.setCustomPolicy(mockPolicy)
        localConfig.consentManagerPolicy = ConsentPolicy.CUSTOM
        every { mockTealiumContext.config } returns localConfig
        val localConsentManager = ConsentManager(mockTealiumContext, eventRouter, mockk())
        assertEquals(10, localConsentManager.expiry.time)
        assertEquals(TimeUnit.MINUTES, localConsentManager.expiry.unit)
    }

    @Test
    fun customConsentExpiry() {
        var localConfig = TealiumConfig(context, "test", "profile", Environment.QA)
        localConfig.consentManagerPolicy = ConsentPolicy.GDPR
        localConfig.consentExpiry = ConsentExpiry(90, TimeUnit.MINUTES)
        every { mockTealiumContext.config } returns localConfig
        val localConsentManager = ConsentManager(mockTealiumContext, eventRouter, mockk())
        assertEquals(90, localConsentManager.expiry.time)
        assertEquals(TimeUnit.MINUTES, localConsentManager.expiry.unit)
    }

    @Test
    fun consentManagerLastSetNotDefinedUponWhenStatusUnknown() {
        consentManager.userConsentStatus = ConsentStatus.UNKNOWN

        // run once on init
        verify(exactly = 1) {
            sharedPreferences.getLong(any(), any())
        }
    }

    @Test
    fun expireConsentUpdatesConsentStatus() {
        val mockTime = System.currentTimeMillis() - 60001
        every { editor.putLong(KEY_LAST_STATUS_UPDATE, mockTime) } returns editor
        every { sharedPreferences.getLong(any(), any()) } returns mockTime
        every { sharedPreferences.getString(KEY_STATUS, any()) } returns "consented"

        consentManager.shouldQueue(TealiumView("track"))

        verify(exactly = 1) {
            editor.putString(KEY_STATUS, any())
        }
    }

    @Test
    fun expireConsentDoesntUpdateConsentStatus() {
        every { editor.putLong(KEY_LAST_STATUS_UPDATE, 0) } returns editor
        every { sharedPreferences.getLong(any(), any()) } returns 0

        consentManager.shouldQueue(TealiumView("track"))

        verify(exactly = 0) {
            editor.putString(KEY_STATUS, any())
        }
    }

    @Test
    fun expireConsentCallsListener() {
        val mockTime = System.currentTimeMillis() - 60001
        every { editor.putLong(KEY_LAST_STATUS_UPDATE, mockTime) } returns editor
        every { sharedPreferences.getLong(any(), any()) } returns mockTime
        every { sharedPreferences.getString(KEY_STATUS, any()) } returns "consented"
        consentManager =
            ConsentManager(mockTealiumContext, eventRouter, mockk(), ConsentPolicy.GDPR)

        verify(exactly = 1) {
            eventRouter.onUserConsentPreferencesUpdated(any(), any())
        }
    }

    @Test
    fun expireConsentDoesntCallListener() {
        every { editor.putLong(KEY_LAST_STATUS_UPDATE, 0) } returns editor
        every { sharedPreferences.getLong(any(), any()) } returns 0
        consentManager =
            ConsentManager(mockTealiumContext, eventRouter, mockk(), ConsentPolicy.GDPR)

        verify(exactly = 0) {
            eventRouter.onUserConsentPreferencesUpdated(any(), any())
        }
    }

    @Test
    fun consentManager_DelegatesToCustomConsentPolicy() = runBlocking {
        ConsentPolicy.CUSTOM.setCustomPolicy(mockPolicy)
        every { sharedPreferences.getString(KEY_STATUS, any()) } returns "consented"
        consentManager =
            ConsentManager(mockTealiumContext, eventRouter, mockk(), ConsentPolicy.CUSTOM)

        every { mockPolicy.shouldQueue() } returns true
        every { mockPolicy.shouldDrop() } returns true
        every { mockPolicy.policyStatusInfo() } returns mapOf("my_policy_name" to "policy")

        assertTrue(consentManager.shouldQueue(mockk(relaxed = true)))
        assertTrue(consentManager.shouldDrop(mockk()))
        val policyInfo = consentManager.collect()
        assertTrue(policyInfo["my_policy_name"] == "policy")
    }

    @Test
    fun shouldQueue_Adds_PolicyStatus_To_Dispatch() {
        ConsentPolicy.CUSTOM.setCustomPolicy(mockPolicy)
        every { sharedPreferences.getString(KEY_STATUS, any()) } returns "consented"
        consentManager = ConsentManager(mockTealiumContext, eventRouter, mockk(), ConsentPolicy.CUSTOM)

        every { mockPolicy.shouldQueue() } returns true
        every { mockPolicy.shouldDrop() } returns true
        every { mockPolicy.policyStatusInfo() } returns mapOf("my_policy_name" to "policy")

        val dispatch = TealiumEvent("event")
        consentManager.shouldQueue(dispatch)

        assertEquals("policy", dispatch.payload()["my_policy_name"])
    }

    @Test
    fun isConsentGrantedEvent_Returns_True_When_Tealium_Event_Is_One_Of_Consent_Logging_Names() {
        listOf(
            ConsentManagerConstants.GRANT_FULL_CONSENT,
            ConsentManagerConstants.GRANT_PARTIAL_CONSENT,
            ConsentManagerConstants.DECLINE_CONSENT
        ).forEach { consentEventName ->
            assertTrue(ConsentManager.isConsentGrantedEvent(TealiumEvent(consentEventName)))
        }
    }

    @Test
    fun isConsentGrantedEvent_Returns_False_When_Tealium_Event_Key_Missing() {
        val dispatch = TealiumEvent("test")
        dispatch.remove(Dispatch.Keys.TEALIUM_EVENT)
        assertFalse(ConsentManager.isConsentGrantedEvent(dispatch))
    }

    @Test
    fun isConsentGrantedEvent_Returns_False_When_Tealium_Event_Key_Not_A_Consent_Event() {
        val dispatch = TealiumEvent("test")
        assertFalse(ConsentManager.isConsentGrantedEvent(dispatch))
    }
}
