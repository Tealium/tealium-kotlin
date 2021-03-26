package com.tealium.core.consent

import com.tealium.core.consent.ConsentManagerConstants.CONSENT_CATEGORIES
import com.tealium.core.consent.ConsentManagerConstants.CONSENT_DO_NOT_SELL
import com.tealium.core.consent.ConsentManagerConstants.CONSENT_POLICY
import com.tealium.core.consent.ConsentManagerConstants.CONSENT_STATUS
import com.tealium.core.consent.ConsentManagerConstants.DECLINE_CONSENT
import com.tealium.core.consent.ConsentManagerConstants.GRANT_FULL_CONSENT
import com.tealium.core.consent.ConsentManagerConstants.GRANT_PARTIAL_CONSENT
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class UserConsentPreferencesTest {

    @MockK
    lateinit var preferences: UserConsentPreferences

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun testStringToEnumConversions_CorrectStatusIsReturned() {
        assertEquals(ConsentStatus.CONSENTED, ConsentStatus.consentStatus("consented"))
        assertEquals(ConsentStatus.NOT_CONSENTED, ConsentStatus.consentStatus("notConsented"))

        assertEquals(ConsentStatus.UNKNOWN, ConsentStatus.consentStatus("unknown"))
        assertEquals(ConsentStatus.UNKNOWN, ConsentStatus.consentStatus("invalid"))
    }

    @Test
    fun testStringToEnumConversions_CorrectCategoriesAreReturned() {
        for (category in ConsentCategory.values()) {
            assertEquals(category, ConsentCategory.consentCategory(category.value))
        }
    }

    @Test
    fun testStringToEnumConversions_SetContainsOnlyValidCategoriesWhenValidStringsProvided() {
        val setOfValidCategories = setOf(ConsentCategory.SOCIAL.value,
                ConsentCategory.CDP.value,
                ConsentCategory.EMAIL.value,
                ConsentCategory.MONITORING.value)
        val validConsentCategories = ConsentCategory.consentCategories(setOfValidCategories)

        assertTrue(validConsentCategories.contains(ConsentCategory.SOCIAL))
        assertTrue(validConsentCategories.contains(ConsentCategory.CDP))
        assertTrue(validConsentCategories.contains(ConsentCategory.EMAIL))
        assertTrue(validConsentCategories.contains(ConsentCategory.MONITORING))
        assertEquals(4, validConsentCategories.count())
    }

    @Test
    fun testStringToEnumConversions_SetContainsOnlyValidCategoriesWhenInvalidStringsProvided() {
        val setOfValidCategories = setOf(ConsentCategory.SOCIAL.value,
                ConsentCategory.CDP.value,
                "invalidCategory",
                ConsentCategory.EMAIL.value,
                "invalidCategory2",
                ConsentCategory.MONITORING.value,
                "invalidCategory3")
        val validConsentCategories = ConsentCategory.consentCategories(setOfValidCategories)

        assertTrue(validConsentCategories.contains(ConsentCategory.SOCIAL))
        assertTrue(validConsentCategories.contains(ConsentCategory.CDP))
        assertTrue(validConsentCategories.contains(ConsentCategory.EMAIL))
        assertTrue(validConsentCategories.contains(ConsentCategory.MONITORING))
        assertEquals(4, validConsentCategories.count())
    }

    @Test
    fun testConsentManagementPolicy_Gdpr_Defaults() {
        val gdprPolicy = ConsentPolicy.GDPR.create(preferences)
        assertTrue(gdprPolicy.consentLoggingEnabled)
        assertTrue(gdprPolicy.cookieUpdateRequired)
        assertEquals("update_consent_cookie", gdprPolicy.cookieUpdateEventName)
        assertEquals(ConsentPolicy.GDPR.value, gdprPolicy.name)
    }

    @Test
    fun testConsentManagementPolicy_Gdpr_WhenStatusUnknown() {
        every { preferences.consentStatus } returns ConsentStatus.UNKNOWN
        every { preferences.consentCategories } returns null

        val gdprPolicy = ConsentPolicy.GDPR.create(preferences)
        assertTrue(gdprPolicy.shouldQueue())
        assertFalse(gdprPolicy.shouldDrop())
        assertEquals(DECLINE_CONSENT, gdprPolicy.consentLoggingEventName)

        val policyInfo = gdprPolicy.policyStatusInfo()
        assertEquals(ConsentPolicy.GDPR.value, policyInfo[CONSENT_POLICY])
        assertFalse(policyInfo.keys.contains(CONSENT_CATEGORIES))
    }

    @Test
    fun testConsentManagementPolicy_Gdpr_WhenStatusFullyConsented() {
        every { preferences.consentStatus } returns ConsentStatus.CONSENTED
        every { preferences.consentCategories } returns ConsentCategory.ALL

        val gdprPolicy = ConsentPolicy.GDPR.create(preferences)
        assertFalse(gdprPolicy.shouldQueue())
        assertFalse(gdprPolicy.shouldDrop())
        assertEquals(GRANT_FULL_CONSENT, gdprPolicy.consentLoggingEventName)

        val policyInfo = gdprPolicy.policyStatusInfo()
        assertEquals(ConsentPolicy.GDPR.value, policyInfo[CONSENT_POLICY])
        assertEquals(ConsentStatus.CONSENTED, policyInfo[CONSENT_STATUS])
        assertTrue(policyInfo.keys.contains(CONSENT_CATEGORIES))
    }

    @Test
    fun testConsentManagementPolicy_Gdpr_WhenStatusPartiallyConsented() {
        every { preferences.consentStatus } returns ConsentStatus.CONSENTED
        every { preferences.consentCategories } returns null

        val gdprPolicy = ConsentPolicy.GDPR.create(preferences)
        assertFalse(gdprPolicy.shouldQueue())
        assertFalse(gdprPolicy.shouldDrop())
        assertEquals(GRANT_PARTIAL_CONSENT, gdprPolicy.consentLoggingEventName)

        var policyInfo = gdprPolicy.policyStatusInfo()
        assertEquals(ConsentPolicy.GDPR.value, policyInfo[CONSENT_POLICY])
        assertEquals(ConsentStatus.CONSENTED, policyInfo[CONSENT_STATUS])
        assertFalse(policyInfo.keys.contains(CONSENT_CATEGORIES))

        every { preferences.consentCategories } returns setOf(ConsentCategory.AFFILIATES, ConsentCategory.MONITORING)
        policyInfo = gdprPolicy.policyStatusInfo()
        assertEquals(ConsentPolicy.GDPR.value, policyInfo[CONSENT_POLICY])
        assertEquals(ConsentStatus.CONSENTED, policyInfo[CONSENT_STATUS])
        assertTrue(policyInfo.keys.contains(CONSENT_CATEGORIES))
    }

    @Test
    fun testConsentManagementPolicy_Gdpr_WhenStatusNotConsented() {
        every { preferences.consentStatus } returns ConsentStatus.NOT_CONSENTED
        every { preferences.consentCategories } returns null

        val gdprPolicy = ConsentPolicy.GDPR.create(preferences)
        assertTrue(gdprPolicy.shouldDrop())
        assertEquals(DECLINE_CONSENT, gdprPolicy.consentLoggingEventName)

        val policyInfo = gdprPolicy.policyStatusInfo()
        assertEquals(ConsentPolicy.GDPR.value, policyInfo[CONSENT_POLICY])
        assertEquals(ConsentStatus.NOT_CONSENTED, policyInfo[CONSENT_STATUS])
        assertFalse(policyInfo.keys.contains(CONSENT_CATEGORIES))
    }

    @Test
    fun testConsentManagementPolicy_Ccpa_Defaults() {
        val ccpaPolicy = ConsentPolicy.CCPA.create(preferences)
        assertFalse(ccpaPolicy.consentLoggingEnabled)
        assertTrue(ccpaPolicy.cookieUpdateRequired)
        assertEquals("set_dns_state", ccpaPolicy.cookieUpdateEventName)
        assertEquals(ConsentPolicy.CCPA.value, ccpaPolicy.name)
    }

    @Test
    fun testConsentManagementPolicy_Ccpa_WhenStatusUnknown() {
        every { preferences.consentStatus } returns ConsentStatus.UNKNOWN
        every { preferences.consentCategories } returns null

        val ccpaPolicy = ConsentPolicy.CCPA.create(preferences)
        assertFalse(ccpaPolicy.shouldQueue())
        assertFalse(ccpaPolicy.shouldDrop())
        assertEquals(GRANT_PARTIAL_CONSENT, ccpaPolicy.consentLoggingEventName)

        val policyInfo = ccpaPolicy.policyStatusInfo()
        assertEquals(ConsentPolicy.CCPA.value, policyInfo[CONSENT_POLICY])
        assertEquals(false, policyInfo[CONSENT_DO_NOT_SELL])
        assertFalse(policyInfo.keys.contains(CONSENT_STATUS))
        assertFalse(policyInfo.keys.contains(CONSENT_CATEGORIES))
    }

    @Test
    fun testConsentManagementPolicy_Ccpa_WhenStatusFullyConsented() {
        every { preferences.consentStatus } returns ConsentStatus.CONSENTED
        every { preferences.consentCategories } returns ConsentCategory.ALL

        val ccpaPolicy = ConsentPolicy.CCPA.create(preferences)
        assertFalse(ccpaPolicy.shouldQueue())
        assertFalse(ccpaPolicy.shouldDrop())
        assertEquals(GRANT_FULL_CONSENT, ccpaPolicy.consentLoggingEventName)

        val policyInfo = ccpaPolicy.policyStatusInfo()
        assertEquals(ConsentPolicy.CCPA.value, policyInfo[CONSENT_POLICY])
        assertTrue(policyInfo[CONSENT_DO_NOT_SELL] as Boolean)
        assertFalse(policyInfo.keys.contains(CONSENT_STATUS))
        assertFalse(policyInfo.keys.contains(CONSENT_CATEGORIES))
    }

    @Test
    fun testConsentManagementPolicy_Ccpa_WhenStatusPartiallyConsented() {
        every { preferences.consentStatus } returns ConsentStatus.CONSENTED
        every { preferences.consentCategories } returns null

        val ccpaPolicy = ConsentPolicy.CCPA.create(preferences)
        assertFalse(ccpaPolicy.shouldQueue())
        assertFalse(ccpaPolicy.shouldDrop())
        assertEquals(GRANT_FULL_CONSENT, ccpaPolicy.consentLoggingEventName)

        val policyInfo = ccpaPolicy.policyStatusInfo()
        assertEquals(ConsentPolicy.CCPA.value, policyInfo[CONSENT_POLICY])
        assertTrue(policyInfo[CONSENT_DO_NOT_SELL] as Boolean)
        assertFalse(policyInfo.keys.contains(CONSENT_STATUS))
        assertFalse(policyInfo.keys.contains(CONSENT_CATEGORIES))
    }

    @Test
    fun testConsentManagementPolicy_Ccpa_WhenStatusNotConsented() {
        every { preferences.consentStatus } returns ConsentStatus.NOT_CONSENTED
        every { preferences.consentCategories } returns null

        val ccpaPolicy = ConsentPolicy.CCPA.create(preferences)
        assertFalse(ccpaPolicy.shouldQueue())
        assertFalse(ccpaPolicy.shouldDrop())
        assertEquals(GRANT_PARTIAL_CONSENT, ccpaPolicy.consentLoggingEventName)

        val policyInfo = ccpaPolicy.policyStatusInfo()
        assertEquals(ConsentPolicy.CCPA.value, policyInfo[CONSENT_POLICY])
        assertFalse(policyInfo[CONSENT_DO_NOT_SELL] as Boolean)
        assertFalse(policyInfo.keys.contains(CONSENT_CATEGORIES))
    }

    @Test
    fun testConsentManagementPolicy_Custom_ReturnsCustomPolicy() {
        val mockPolicy: ConsentManagementPolicy = mockk(relaxed = true)
        ConsentPolicy.CUSTOM.setCustomPolicy(mockPolicy)

        val policy = ConsentPolicy.CUSTOM.create(preferences)
        assertSame(mockPolicy, policy)

        verify {
            mockPolicy.userConsentPreferences = preferences
        }
    }
}