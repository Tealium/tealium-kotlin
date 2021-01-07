package com.tealium.core.consent

import android.app.Application
import android.content.SharedPreferences
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.consent.ConsentManagerConstants.KEY_CATEGORIES
import com.tealium.core.consent.ConsentManagerConstants.KEY_LAST_STATUS_UPDATE
import com.tealium.core.consent.ConsentManagerConstants.KEY_STATUS
import com.tealium.core.messaging.EventRouter
import com.tealium.dispatcher.TealiumView
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

class ConsentManagerTest {

    @MockK
    lateinit var context: Application

    @MockK
    lateinit var mockFile: File

    @RelaxedMockK
    lateinit var sharedPreferences: SharedPreferences

    @RelaxedMockK
    lateinit var editor: SharedPreferences.Editor

    @MockK(relaxed = true)
    lateinit var eventRouter: EventRouter

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

        config = TealiumConfig(context, "test", "profile", Environment.QA)
        config.consentExpiry = ConsentExpiry(1, TimeUnit.MINUTES)
        consentManager = ConsentManager(config, eventRouter, "visitor1234567890", mockk())
    }

    @Test
    fun consentManagerModuleDisabledByDefault() {
        assertFalse(consentManager.enabled)
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
        assertFalse(consentManager.shouldQueue(mockk()))
    }

    @Test
    fun consentManagerShouldDropDefaultsToFalse() {
        assertFalse(consentManager.shouldDrop(mockk()))
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
        every { editor.putStringSet(KEY_CATEGORIES, ConsentCategory.ALL.map { it.value }.toMutableSet()) } returns editor
        every { sharedPreferences.getString(any(), any()) } returns "consented"
        every { sharedPreferences.getStringSet(KEY_CATEGORIES, null) } returns ConsentCategory.ALL.map { it.value }.toMutableSet()
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
    fun userPreferencesUpdateListener_Called() {
        every { editor.putString(KEY_STATUS, "consented") } returns editor
        every { sharedPreferences.getString(any(), any()) } returns "consented"
        every { sharedPreferences.getStringSet(KEY_CATEGORIES, null) } returns setOf("engagement")
        every { editor.putStringSet(KEY_CATEGORIES, setOf("engagement")) } returns editor
        consentManager = ConsentManager(config, eventRouter, "", mockk(), ConsentPolicy.GDPR)
        consentManager.userConsentCategories = setOf(ConsentCategory.ENGAGEMENT)

        // no policy == no updates.
        verify(exactly = 1) {
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
        assertTrue(data.isEmpty())
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
    fun defaultConsentExpiryCCPA() {
        var localConfig = TealiumConfig(context, "test", "profile", Environment.QA)
        localConfig.consentManagerPolicy = ConsentPolicy.CCPA
        val localConsentManager = ConsentManager(localConfig, eventRouter, "visitor1234567890", mockk())
        assertEquals(395, localConsentManager.expiry.time)
        assertEquals(TimeUnit.DAYS, localConsentManager.expiry.unit)
    }

    @Test
    fun defaultConsentExpiryGDPR() {
        var localConfig = TealiumConfig(context, "test", "profile", Environment.QA)
        localConfig.consentManagerPolicy = ConsentPolicy.GDPR
        val localConsentManager = ConsentManager(localConfig, eventRouter, "visitor1234567890", mockk())
        assertEquals(365, localConsentManager.expiry.time)
        assertEquals(TimeUnit.DAYS, localConsentManager.expiry.unit)
    }

    @Test
    fun customConsentExpiry() {
        var localConfig = TealiumConfig(context, "test", "profile", Environment.QA)
        localConfig.consentManagerPolicy = ConsentPolicy.GDPR
        localConfig.consentExpiry = ConsentExpiry(90, TimeUnit.MINUTES)
        val localConsentManager = ConsentManager(localConfig, eventRouter, "visitor1234567890", mockk())
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
        consentManager = ConsentManager(config, eventRouter, "", mockk(), ConsentPolicy.GDPR)

        verify(exactly = 1) {
            eventRouter.onUserConsentPreferencesUpdated(any(), any())
        }
    }

    @Test
    fun expireConsentDoesntCallListener() {
        every { editor.putLong(KEY_LAST_STATUS_UPDATE, 0) } returns editor
        every { sharedPreferences.getLong(any(), any()) } returns 0
        consentManager = ConsentManager(config, eventRouter, "", mockk(), ConsentPolicy.GDPR)

        verify(exactly = 0) {
            eventRouter.onUserConsentPreferencesUpdated(any(), any())
        }
    }
}
