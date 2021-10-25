package com.tealium.core.consent

import android.app.Application
import android.content.SharedPreferences
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.consent.ConsentManagerConstants.KEY_CATEGORIES
import com.tealium.core.consent.ConsentManagerConstants.KEY_STATUS
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
        every { sharedPreferences.getString(KEY_STATUS, any()) } returns ConsentStatus.default().value

        assertEquals(ConsentStatus.UNKNOWN, consentSharedPreferences.consentStatus)
    }

    @Test
    fun testConsentStatus_InitialValueIsNotUnknown() {
        every { sharedPreferences.getString(KEY_STATUS, any()) } returns ConsentStatus.NOT_CONSENTED.value

        assertEquals(ConsentStatus.NOT_CONSENTED, consentSharedPreferences.consentStatus)
    }

    @Test
    fun testConsentCategories_RemovesKeyWhenNull() {
        every { editor.remove(any()) } returns editor
        consentSharedPreferences.consentCategories = null

        verify { editor.remove(KEY_CATEGORIES) }
    }

    @Test
    fun testConsentCategories_UpdatesStoredCategories() {
        every { editor.putStringSet(KEY_CATEGORIES, any()) } returns editor
        consentSharedPreferences.consentCategories = setOf(
                ConsentCategory.MONITORING,
                ConsentCategory.CDP,
                ConsentCategory.EMAIL)

        verify {
            editor.putStringSet(
                KEY_CATEGORIES, setOf(
                    ConsentCategory.MONITORING.value,
                    ConsentCategory.CDP.value,
                    ConsentCategory.EMAIL.value
                )
            )
        }
    }

    @Test
    fun testConsentReset_GetsReset() {
        every { editor.putString(KEY_STATUS, any()) } returns editor
        every { editor.remove(any()) } returns editor

        consentSharedPreferences.reset()

        verify {
            editor.putString(KEY_STATUS, "unknown")
            editor.remove(KEY_CATEGORIES)
        }
    }
}