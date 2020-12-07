package com.tealium.core.consent
import android.content.SharedPreferences
import com.tealium.core.TealiumConfig
import com.tealium.core.consent.ConsentManagerConstants.KEY_STATUS
import com.tealium.core.consent.ConsentManagerConstants.KEY_CATEGORIES
import com.tealium.core.consent.ConsentManagerConstants.KEY_LAST_SET
import java.time.LocalDateTime
import java.util.*

/**
 * This class is responsible for the persistence of consent preferences as defined by [ConsentStatus]
 * and [ConsentCategory].
 */
internal class ConsentSharedPreferences(config: TealiumConfig) {

    private val sharedPreferences: SharedPreferences = config.application.getSharedPreferences(sharedPreferencesName(config), 0)

    var consentStatus: ConsentStatus = ConsentStatus.UNKNOWN
        get() {
            return ConsentStatus.consentStatus(
                    sharedPreferences.getString(KEY_STATUS, ConsentStatus.default().value)!!
            )
        }
        set(value) {
            field = value
            sharedPreferences.edit()
                    .putString(KEY_STATUS, field.value)
                    .apply()
        }

    var consentCategories: Set<ConsentCategory>? = null
        get() {
            return sharedPreferences.getStringSet(KEY_CATEGORIES, null)?.let {
                ConsentCategory.consentCategories(it.filterNotNull().toSet())
            }
        }
        set(value) {
            field = value
            value?.let { categories ->
                sharedPreferences.edit()
                        .putStringSet(KEY_CATEGORIES, categories.map { category -> category.value }.toSet())
                        .apply()
            } ?: run {
                sharedPreferences.edit().remove(KEY_CATEGORIES).apply()
            }
        }

    var lastSet: Long? = System.currentTimeMillis()
        get() {
            return sharedPreferences.getLong(KEY_LAST_SET, System.currentTimeMillis())
        }
        set(value) {
            field = value
            field?.let {
                sharedPreferences.edit()
                        .putLong(KEY_LAST_SET, it)
                        .apply()
            }
        }


    fun setConsentStatus(consentStatus: ConsentStatus, consentCategories: Set<ConsentCategory>? = null) {
        this.lastSet = System.currentTimeMillis()
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