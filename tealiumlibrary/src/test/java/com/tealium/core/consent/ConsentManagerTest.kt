package com.tealium.core.consent

import android.app.Application
import android.content.SharedPreferences
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.consent.ConsentManagerConstants.KEY_CATEGORIES
import com.tealium.core.consent.ConsentManagerConstants.KEY_STATUS
import com.tealium.core.messaging.EventRouter
import com.tealium.core.network.Connectivity
import com.tealium.core.network.ConnectivityRetriever
import com.tealium.core.network.HttpClient
import com.tealium.core.settings.LibrarySettings
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ConsentManagerTest {

    @MockK
    lateinit var context: Application

    @MockK
    lateinit var mockFile: File

    @MockK
    lateinit var sharedPreferences: SharedPreferences

    @MockK
    lateinit var editor: SharedPreferences.Editor

    @MockK
    lateinit var mockTealiumContext: TealiumContext

    @RelaxedMockK
    lateinit var mockHttpClient: HttpClient

    @RelaxedMockK
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
        every { editor.remove(any()) } returns editor

        config = TealiumConfig(context, "test", "profile", Environment.QA)
        every { mockTealiumContext.config } returns config
        every { mockTealiumContext.visitorId } returns "visitor1234567890"
        every { mockTealiumContext.httpClient } returns mockHttpClient
        consentManager = ConsentManager(mockTealiumContext, eventRouter, mockk())
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
        consentManager = ConsentManager(mockTealiumContext, eventRouter, mockk(), ConsentPolicy.GDPR)
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
    fun consentLoggingEnabled_SendsUpdatedTealiumVisitorId() = runBlocking {
        config.consentManagerLoggingEnabled = true
        val mockSettings: LibrarySettings = mockk()
        every { mockSettings.wifiOnly } returns false

        val mockConnectivity: Connectivity = mockk()
        every { mockConnectivity.isConnected() } returns true
        every { mockConnectivity.isConnectedWifi() } returns true

        mockkObject(ConnectivityRetriever)
        every { ConnectivityRetriever.getInstance(any<Application>()) } returns mockConnectivity

        consentManager = ConsentManager(mockTealiumContext, eventRouter, mockSettings, ConsentPolicy.GDPR)
        consentManager.userConsentStatus = ConsentStatus.UNKNOWN
        coVerify(timeout = 500) {
            mockHttpClient.post(match {
                it.contains("visitor1234567890")
            }, any(), any())
        }

        every { mockTealiumContext.visitorId } returns "newVisitor"
        consentManager.userConsentStatus = ConsentStatus.UNKNOWN

        coVerify(timeout = 500) {
            mockHttpClient.post(match {
                it.contains("newVisitor")
            }, any(), any())
        }
    }

    @Test
    fun consentLoggingEnabled_DoesNotSendWhenNotConnected() = runBlocking {
        config.consentManagerLoggingEnabled = true
        val mockSettings: LibrarySettings = mockk()
        every { mockSettings.wifiOnly } returns false

        val mockConnectivity: Connectivity = mockk()
        every { mockConnectivity.isConnected() } returns false
        every { mockConnectivity.isConnectedWifi() } returns false

        mockkObject(ConnectivityRetriever)
        every { ConnectivityRetriever.getInstance(any<Application>()) } returns mockConnectivity

        consentManager = ConsentManager(mockTealiumContext, eventRouter, mockSettings, ConsentPolicy.GDPR)
        consentManager.userConsentStatus = ConsentStatus.UNKNOWN
        coVerify(exactly = 0, timeout = 500) {
            mockHttpClient.post(any(), any(), any())
        }
    }

    @Test
    fun consentLoggingEnabled_DoesNotSendWhenWifiOnly() = runBlocking {
        config.consentManagerLoggingEnabled = true
        val mockSettings: LibrarySettings = mockk()
        every { mockSettings.wifiOnly } returns true

        val mockConnectivity: Connectivity = mockk()
        every { mockConnectivity.isConnected() } returns true
        every { mockConnectivity.isConnectedWifi() } returns false

        mockkObject(ConnectivityRetriever)
        every { ConnectivityRetriever.getInstance(any<Application>()) } returns mockConnectivity

        consentManager = ConsentManager(mockTealiumContext, eventRouter, mockSettings, ConsentPolicy.GDPR)
        consentManager.userConsentStatus = ConsentStatus.UNKNOWN
        coVerify(exactly = 0, timeout = 500) {
            mockHttpClient.post(any(), any(), any())
        }
    }
}
