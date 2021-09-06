package com.tealium.core.consent
import android.content.SharedPreferences
import com.tealium.core.TealiumConfig
import com.tealium.dispatcher.Dispatch

/**
 * This class is responsible for the persistence of consent preferences as defined by [ConsentStatus]
 * and [ConsentCategory].
 */
internal class ConsentSharedPreferences(config: TealiumConfig) {

    private val sharedPreferences: SharedPreferences = config.application.getSharedPreferences(sharedPreferencesName(config), 0)

    var consentStatus: ConsentStatus = ConsentStatus.UNKNOWN
        get() {
            return ConsentStatus.consentStatus(
                    sharedPreferences.getString(
                        Dispatch.Keys.CONSENT_STATUS,
                        ConsentStatus.default().value
                    )!!
            )
        }
        set(value) {
            field = value
            sharedPreferences.edit()
                    .putString(Dispatch.Keys.CONSENT_STATUS, field.value)
                    .apply()
        }

    var consentCategories: Set<ConsentCategory>? = null
        get() {
            return sharedPreferences.getStringSet(Dispatch.Keys.CONSENT_CATEGORIES, null)?.let {
                ConsentCategory.consentCategories(it.filterNotNull().toSet())
            }
        }
        set(value) {
            field = value
            value?.let { categories ->
                sharedPreferences.edit()
                        .putStringSet(
                            Dispatch.Keys.CONSENT_CATEGORIES,
                            categories.map { category -> category.value }.toSet()
                        )
                        .apply()
            } ?: run {
                sharedPreferences.edit().remove(Dispatch.Keys.CONSENT_CATEGORIES).apply()
            }
        }

    var lastUpdate: Long? = null
        get() {
            return sharedPreferences.getLong(Dispatch.Keys.CONSENT_LAST_STATUS_UPDATE, 0)
        }
        set(value) {
            field = value
            field?.let {
                sharedPreferences.edit()
                        .putLong(Dispatch.Keys.CONSENT_LAST_STATUS_UPDATE, it)
                        .apply()
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
        return "tealium.userconsentpreferences." + Integer.toHexString((config.accountName + config.profileName + config.environment.environment).hashCode())
    }
}