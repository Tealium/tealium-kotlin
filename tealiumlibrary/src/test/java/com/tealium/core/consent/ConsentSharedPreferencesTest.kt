package com.tealium.core.consent

import android.app.Application
import android.content.SharedPreferences
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.consent.ConsentManagerConstants.CONSENT_CATEGORIES
import com.tealium.core.consent.ConsentManagerConstants.CONSENT_STATUS
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class ConsentSharedPreferencesTest {

    @MockK
    lateinit var context: Application

    @MockK
    lateinit var file: File

    @MockK
    lateinit var sharedPreferences: SharedPreferences

    @MockK
    lateinit var editor: SharedPreferences.Editor

    private lateinit var consentSharedPreferences: ConsentSharedPreferences

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { context.filesDir } returns file
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.apply() } just Runs

        val config = TealiumConfig(context, "test", "profile", Environment.QA)
        consentSharedPreferences = ConsentSharedPreferences(config)
    }

    @Test
    fun testConsentStatus_InitialValueIsUnknown() {
        every { sharedPreferences.getString(CONSENT_STATUS, any()) } returns ConsentStatus.default().value

        assertEquals(ConsentStatus.UNKNOWN, consentSharedPreferences.consentStatus)
    }

    @Test
    fun testConsentStatus_InitialValueIsNotUnknown() {
        every { sharedPreferences.getString(CONSENT_STATUS, any()) } returns ConsentStatus.NOT_CONSENTED.value

        assertEquals(ConsentStatus.NOT_CONSENTED, consentSharedPreferences.consentStatus)
    }

    @Test
    fun testConsentCategories_RemovesKeyWhenNull() {
        every { editor.remove(any()) } returns editor
        consentSharedPreferences.consentCategories = null

        verify { editor.remove(CONSENT_CATEGORIES) }
    }

    @Test
    fun testConsentCategories_UpdatesStoredCategories() {
        every { editor.putStringSet(CONSENT_CATEGORIES, any()) } returns editor
        consentSharedPreferences.consentCategories = setOf(
                ConsentCategory.MONITORING,
                ConsentCategory.CDP,
                ConsentCategory.EMAIL)

        verify {
            editor.putStringSet(CONSENT_CATEGORIES, setOf(
                ConsentCategory.MONITORING.value,
                ConsentCategory.CDP.value,
                ConsentCategory.EMAIL.value))
        }
    }

    @Test
    fun testConsentReset_GetsReset() {
        every { editor.putString(CONSENT_STATUS, any()) } returns editor
        every { editor.remove(any()) } returns editor

        consentSharedPreferences.reset()

        verify {
            editor.putString(CONSENT_STATUS, "unknown")
            editor.remove(CONSENT_CATEGORIES)
        }
    }
}