package com.tealium.core.consent

import com.tealium.core.consent.ConsentManagerConstants.CONSENT_CATEGORIES
import com.tealium.core.consent.ConsentManagerConstants.CONSENT_STATUS
import android.content.SharedPreferences
import com.tealium.core.TealiumConfig

/**
 * This class is responsible for the persistence of consent preferences as defined by [ConsentStatus]
 * and [ConsentCategory].
 */
internal class ConsentSharedPreferences(config: TealiumConfig) {

    private val sharedPreferences: SharedPreferences = config.application.getSharedPreferences(sharedPreferencesName(config), 0)

    var consentStatus: ConsentStatus = ConsentStatus.UNKNOWN
        get() {
            return ConsentStatus.consentStatus(
                    sharedPreferences.getString(CONSENT_STATUS, ConsentStatus.default().value)!!
            )
        }
        set(value) {
            field = value
            sharedPreferences.edit()
                    .putString(CONSENT_STATUS, field.value)
                    .apply()
        }

    var consentCategories: Set<ConsentCategory>? = null
        get() {
            return sharedPreferences.getStringSet(CONSENT_CATEGORIES, null)?.let {
                ConsentCategory.consentCategories(it.filterNotNull().toSet())
            }
        }
        set(value) {
            field = value
            value?.let { categories ->
                sharedPreferences.edit()
                        .putStringSet(CONSENT_CATEGORIES, categories.map { category -> category.value }.toSet())
                        .apply()
            } ?: run {
                sharedPreferences.edit().remove(CONSENT_CATEGORIES).apply()
            }
        }

    fun setConsentStatus(consentStatus: ConsentStatus, consentCategories: Set<ConsentCategory>? = null) {
        this.consentStatus = consentStatus
        this.consentCategories = consentCategories
    }

    fun reset() {
        consentStatus = ConsentStatus.UNKNOWN
        consentCategories = null
    }

    private fun sharedPreferencesName(config: TealiumConfig): String {
        return "tealium.consentpreferences." + Integer.toHexString((config.accountName + config.profileName + config.environment).hashCode())
    }
}