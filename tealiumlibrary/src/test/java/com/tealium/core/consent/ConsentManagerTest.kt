package com.tealium.core.consent

import android.app.Application
import android.content.SharedPreferences
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.messaging.EventRouter
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class ConsentManagerTest {

    @MockK
    lateinit var context: Application

    @MockK
    lateinit var mockFile: File

    @MockK
    lateinit var sharedPreferences: SharedPreferences

    @MockK
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

        every { sharedPreferences.getString(ConsentManagerSPKey.STATUS, "unknown") } returns "unknown"
        every { sharedPreferences.getStringSet(ConsentManagerSPKey.CATEGORIES, null) } returns null
        every { editor.putString(ConsentManagerSPKey.STATUS, "unknown") } returns editor

        config = TealiumConfig(context, "test", "profile", Environment.QA)
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
        every { editor.putString(ConsentManagerSPKey.STATUS, "notConsented") } returns editor
        every { sharedPreferences.getString(any(), any()) } returns "notConsented"
        every { editor.putStringSet(ConsentManagerSPKey.CATEGORIES, null) } returns editor
        every { editor.remove(any()) } returns editor
        consentManager.userConsentStatus = ConsentStatus.NOT_CONSENTED
        assertEquals(ConsentStatus.NOT_CONSENTED, consentManager.userConsentStatus)
        assertNull(consentManager.userConsentCategories)
    }

    @Test
    fun consentManagerSetSingleCategorySetsConsented() {
        every { editor.putString(ConsentManagerSPKey.STATUS, "consented") } returns editor
        every { sharedPreferences.getString(any(), any()) } returns "consented"
        every { sharedPreferences.getStringSet(ConsentManagerSPKey.CATEGORIES, null) } returns setOf("engagement")
        every { editor.putStringSet(ConsentManagerSPKey.CATEGORIES, setOf("engagement")) } returns editor
        consentManager.userConsentCategories = setOf(ConsentCategory.ENGAGEMENT)
        assertEquals(ConsentStatus.CONSENTED, consentManager.userConsentStatus)
        assertTrue(consentManager.userConsentCategories?.contains(ConsentCategory.ENGAGEMENT)!!)
    }

    @Test
    fun consentManagerSetCategoryNullSetsConsentStatusNotConsented() {
        every { editor.putString(ConsentManagerSPKey.STATUS, "notConsented") } returns editor
        every { sharedPreferences.getString(any(), any()) } returns "notConsented"
        every { editor.putStringSet(ConsentManagerSPKey.CATEGORIES, null) } returns editor
        every { editor.remove(any()) } returns editor
        consentManager.userConsentCategories = null
        assertEquals(ConsentStatus.NOT_CONSENTED, consentManager.userConsentStatus)
        assertNull(consentManager.userConsentCategories)
    }

    @Test
    fun consentManagerSetCategoryNullSetsConsentStatusUnknown() {
        every { editor.putString(ConsentManagerSPKey.STATUS, "unknown") } returns editor
        every { sharedPreferences.getString(any(), any()) } returns "unknown"
        every { editor.putStringSet(ConsentManagerSPKey.CATEGORIES, null) } returns editor
        every { editor.remove(any()) } returns editor
        consentManager.userConsentStatus = ConsentStatus.UNKNOWN
        assertEquals(ConsentStatus.UNKNOWN, consentManager.userConsentStatus)
        assertNull(consentManager.userConsentCategories)
    }

    @Test
    fun setUserConsentStatusConsentedSetsAllCategories() {
        every { editor.putString(ConsentManagerSPKey.STATUS, "consented") } returns editor
        every { editor.putStringSet(ConsentManagerSPKey.CATEGORIES, ConsentCategory.ALL.map { it.value }.toMutableSet()) } returns editor
        every { sharedPreferences.getString(any(), any()) } returns "consented"
        every { sharedPreferences.getStringSet(ConsentManagerSPKey.CATEGORIES, null) } returns ConsentCategory.ALL.map { it.value }.toMutableSet()
        consentManager.userConsentStatus = ConsentStatus.CONSENTED
        assertEquals(ConsentStatus.CONSENTED, consentManager.userConsentStatus)
        assertEquals(15, consentManager.userConsentCategories?.count())
    }

    @Test
    fun resetUserConsentPreferencesSuccess() {
        every { editor.putStringSet(ConsentManagerSPKey.CATEGORIES, null) } returns editor
        every { editor.remove(any()) } returns editor
        consentManager.reset()
        assertEquals(ConsentStatus.UNKNOWN, consentManager.userConsentStatus)
        assertNull(consentManager.userConsentCategories)
    }

    @Test
    fun userPreferencesUpdateListener_Called() {
        every { editor.putString(ConsentManagerSPKey.STATUS, "consented") } returns editor
        every { sharedPreferences.getString(any(), any()) } returns "consented"
        every { sharedPreferences.getStringSet(ConsentManagerSPKey.CATEGORIES, null) } returns setOf("engagement")
        every { editor.putStringSet(ConsentManagerSPKey.CATEGORIES, setOf("engagement")) } returns editor
        consentManager = ConsentManager(config, eventRouter, "", mockk(), ConsentPolicy.GDPR)
        consentManager.userConsentCategories = setOf(ConsentCategory.ENGAGEMENT)

        // no policy == no updates.
        verify(exactly = 1) {
            eventRouter.onUserConsentPreferencesUpdated(any(), any())
        }
    }

    @Test
    fun userPreferencesUpdateListener_NotCalled() {
        every { editor.putString(ConsentManagerSPKey.STATUS, "consented") } returns editor
        every { sharedPreferences.getString(any(), any()) } returns "consented"
        every { sharedPreferences.getStringSet(ConsentManagerSPKey.CATEGORIES, null) } returns setOf("engagement")
        every { editor.putStringSet(ConsentManagerSPKey.CATEGORIES, setOf("engagement")) } returns editor
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
}
