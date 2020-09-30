package com.tealium.core

import android.app.Application
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.consent.ConsentCategory
import com.tealium.core.consent.ConsentManagerConstants.KEY_CATEGORIES
import com.tealium.core.consent.ConsentManagerConstants.KEY_STATUS
import com.tealium.core.consent.ConsentStatus
import io.mockk.MockKAnnotations
import junit.framework.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*

class MigrationTests {

    // file names from legacy java library
    val consentPreferencesNamePrefix = "tealium.userconsentpreferences"
    val persistentDataSourcesPreferencesNamePrefix = "tealium.datasources"
    lateinit var consentPreferences: SharedPreferences
    lateinit var dataSourcesPreferences: SharedPreferences

    lateinit var application: Application
    lateinit var config: TealiumConfig
    lateinit var tealium: Tealium

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        application = ApplicationProvider.getApplicationContext()
        config = TealiumConfig(application, "test", "test", Environment.DEV)
        consentPreferences = application.getSharedPreferences("$consentPreferencesNamePrefix.${getHashCodeString(config)}", 0)
        dataSourcesPreferences = application.getSharedPreferences("$persistentDataSourcesPreferencesNamePrefix.${getHashCodeString(config, ".")}", 0)
    }

    @After
    fun tearDown() {
        consentPreferences.edit().clear()
        dataSourcesPreferences.edit().clear()
    }

    @Test
    fun constants_consent_areSameAsLegacyJavaLibrary() {
        // consent manager
        assertEquals("status", KEY_STATUS)
        assertEquals("categories", KEY_CATEGORIES)
        // consent status
        assertEquals("unknown", ConsentStatus.UNKNOWN.value)
        assertEquals("consented", ConsentStatus.CONSENTED.value)
        assertEquals("notConsented", ConsentStatus.NOT_CONSENTED.value)
        // consent categories
        assertEquals("affiliates", ConsentCategory.AFFILIATES.value)
        assertEquals("analytics", ConsentCategory.ANALYTICS.value)
        assertEquals("big_data", ConsentCategory.BIG_DATA.value)
        assertEquals("cdp", ConsentCategory.CDP.value)
        assertEquals("cookiematch", ConsentCategory.COOKIEMATCH.value)
        assertEquals("crm", ConsentCategory.CRM.value)
        assertEquals("display_ads", ConsentCategory.DISPLAY_ADS.value)
        assertEquals("email", ConsentCategory.EMAIL.value)
        assertEquals("engagement", ConsentCategory.ENGAGEMENT.value)
        assertEquals("mobile", ConsentCategory.MOBILE.value)
        assertEquals("monitoring", ConsentCategory.MONITORING.value)
        assertEquals("personalization", ConsentCategory.PERSONALIZATION.value)
        assertEquals("search", ConsentCategory.SEARCH.value)
        assertEquals("social", ConsentCategory.SOCIAL.value)
        assertEquals("misc", ConsentCategory.MISC.value)
    }

    @Test
    fun consent_status_consentedStatusIsMigrated() {
        consentPreferences.edit().putString("status", "consented").commit()

        tealium = Tealium.create("instance_name", config)
        assertEquals(ConsentStatus.CONSENTED, tealium.consentManager.userConsentStatus)
    }

    @Test
    fun consent_status_notConsentedStatusIsMigrated() {
        consentPreferences.edit().putString("status", "notConsented").commit()

        tealium = Tealium.create("instance_name", config)
        assertEquals(ConsentStatus.NOT_CONSENTED, tealium.consentManager.userConsentStatus)
    }

    @Test
    fun consent_status_unknownStatusIsMigrated() {
        consentPreferences.edit().putString("status", "unknown").commit()

        tealium = Tealium.create("instance_name", config)
        assertEquals(ConsentStatus.UNKNOWN, tealium.consentManager.userConsentStatus)
    }

    @Test
    fun consent_categories_categoryListIsMigrated() {
        consentPreferences.edit().putStringSet("categories", setOf("affiliates", "email", "cdp")).commit()

        tealium = Tealium.create("instance_name", config)
        val categories = tealium.consentManager.userConsentCategories!!
        assertNotNull(categories)
        assertEquals(3, categories.size)
        assertTrue(categories.contains(ConsentCategory.AFFILIATES))
        assertTrue(categories.contains(ConsentCategory.EMAIL))
        assertTrue(categories.contains(ConsentCategory.CDP))
    }

    @Test
    fun consent_categories_nullCategoryListIsMigrated() {
        consentPreferences.edit().putStringSet("categories", null).commit()

        tealium = Tealium.create("instance_name", config)
        val categories = tealium.consentManager.userConsentCategories
        assertNull(categories)
    }

    @Test
    fun dataSources_dataGetsMigrated() {
        with(dataSourcesPreferences.edit()) {
            putString("my_string", "string_value")
            putInt("my_int", 100)
            putFloat("my_float", 100.10F)
            putLong("my_long", 100L)
            putBoolean("my_boolean_true", true)
            putBoolean("my_boolean_false", false)
            putStringSet("my_string_set", setOf("string_value_1", "string_value_2"))
        }.commit()

        tealium = Tealium.create("instance_name", config)
        assertTrue(tealium.dataLayer.contains("my_string"))
        assertTrue(tealium.dataLayer.contains("my_int"))
        assertTrue(tealium.dataLayer.contains("my_float"))
        assertTrue(tealium.dataLayer.contains("my_long"))
        assertTrue(tealium.dataLayer.contains("my_boolean_true"))
        assertTrue(tealium.dataLayer.contains("my_boolean_false"))
        assertTrue(tealium.dataLayer.contains("my_string_set"))

        assertEquals("string_value", tealium.dataLayer.getString("my_string"))
        assertEquals(100, tealium.dataLayer.getInt("my_int"))
        assertEquals(100.10F.toDouble(), tealium.dataLayer.getDouble("my_float"))
        assertEquals(100L, tealium.dataLayer.getLong("my_long"))
        assertTrue(tealium.dataLayer.getBoolean("my_boolean_true")!!)
        assertFalse(tealium.dataLayer.getBoolean("my_boolean_false")!!)
        assertTrue(Arrays.equals(arrayOf("string_value_1", "string_value_2"), tealium.dataLayer.getStringArray("my_string_set")!!.sortedArray()))
    }

    fun getHashCodeString(config: TealiumConfig, delimiter: String = ""): String {
        return Integer.toHexString(
                (config.accountName + delimiter +
                        config.profileName + delimiter +
                        config.environment.environment).hashCode())
    }
}