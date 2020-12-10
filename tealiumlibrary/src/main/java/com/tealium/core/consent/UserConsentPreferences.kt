package com.tealium.core.consent

import org.json.JSONArray
import java.util.*
import java.util.concurrent.TimeUnit

data class UserConsentPreferences(val consentStatus: ConsentStatus, val consentCategories: Set<ConsentCategory>? = null)
data class ConsentExpiry(val time: Long, val unit: TimeUnit)

enum class ConsentStatus(val value: String) {
    UNKNOWN("unknown"),
    CONSENTED("consented"),
    NOT_CONSENTED("notConsented");

    override fun toString(): String {
        return value
    }

    companion object {
        fun default(): ConsentStatus = UNKNOWN

        fun consentStatus(value: String): ConsentStatus {
            return when (value.toLowerCase(Locale.ROOT)) {
                CONSENTED.value.toLowerCase(Locale.ROOT) -> CONSENTED
                NOT_CONSENTED.value.toLowerCase(Locale.ROOT) -> NOT_CONSENTED
                else -> UNKNOWN
            }
        }
    }
}

enum class ConsentCategory(val value: String) {
    AFFILIATES("affiliates"),
    ANALYTICS("analytics"),
    BIG_DATA("big_data"),
    CDP("cdp"),
    COOKIEMATCH("cookiematch"),
    CRM("crm"),
    DISPLAY_ADS("display_ads"),
    EMAIL("email"),
    ENGAGEMENT("engagement"),
    MOBILE("mobile"),
    MONITORING("monitoring"),
    PERSONALIZATION("personalization"),
    SEARCH("search"),
    SOCIAL("social"),
    MISC("misc");

    override fun toString(): String {
        return value
    }

    companion object {
        val ALL = values().toSet()

        fun consentCategory(category: String): ConsentCategory? {
            return when (category.toLowerCase(Locale.ROOT)) {
                AFFILIATES.value.toLowerCase(Locale.ROOT) -> AFFILIATES
                ANALYTICS.value.toLowerCase(Locale.ROOT) -> ANALYTICS
                BIG_DATA.value.toLowerCase(Locale.ROOT) -> BIG_DATA
                CDP.value.toLowerCase(Locale.ROOT) -> CDP
                COOKIEMATCH.value.toLowerCase(Locale.ROOT) -> COOKIEMATCH
                CRM.value.toLowerCase(Locale.ROOT) -> CRM
                DISPLAY_ADS.value.toLowerCase(Locale.ROOT) -> DISPLAY_ADS
                EMAIL.value.toLowerCase(Locale.ROOT) -> EMAIL
                ENGAGEMENT.value.toLowerCase(Locale.ROOT) -> ENGAGEMENT
                MOBILE.value.toLowerCase(Locale.ROOT) -> MOBILE
                MONITORING.value.toLowerCase(Locale.ROOT) -> MONITORING
                PERSONALIZATION.value.toLowerCase(Locale.ROOT) -> PERSONALIZATION
                SEARCH.value.toLowerCase(Locale.ROOT) -> SEARCH
                SOCIAL.value.toLowerCase(Locale.ROOT) -> SOCIAL
                MISC.value.toLowerCase(Locale.ROOT) -> MISC
                else -> null
            }
        }

        fun consentCategories(categories: Set<String>): Set<ConsentCategory> {
            return categories.mapNotNull { consentCategory(it) }.toSet()
        }
    }
}

fun Set<ConsentCategory>.toJsonArray(): JSONArray {
    return JSONArray(this.map { it.value })
}

enum class ConsentPolicy(val value: String) {
    GDPR("gdpr") {
        override fun create(userConsentPreferences: UserConsentPreferences): ConsentManagementPolicy {
            return GdprConsentManagementPolicy(userConsentPreferences)
        }
    },
    CCPA("ccpa") {
        override fun create(userConsentPreferences: UserConsentPreferences): ConsentManagementPolicy {
            return CcpaConsentManagementPolicy(userConsentPreferences)
        }
    };

    abstract fun create(userConsentPreferences: UserConsentPreferences): ConsentManagementPolicy
}

interface ConsentManagementPolicy {

    val name: String
    var userConsentPreferences: UserConsentPreferences
    val consentLoggingEnabled: Boolean
    val consentLoggingEventName: String
    val defaultConsentExpiry: ConsentExpiry

    val cookieUpdateRequired: Boolean
    val cookieUpdateEventName: String

    fun policyStatusInfo(): Map<String, Any>
    fun shouldQueue(): Boolean
    fun shouldDrop(): Boolean
}

private class GdprConsentManagementPolicy(initialConsentPreferences: UserConsentPreferences) : ConsentManagementPolicy {

    override val name: String = ConsentPolicy.GDPR.value
    override var userConsentPreferences: UserConsentPreferences = initialConsentPreferences
    override val consentLoggingEnabled: Boolean = true
    override val consentLoggingEventName: String
        get() = when (userConsentPreferences.consentStatus) {
            ConsentStatus.CONSENTED -> {
                userConsentPreferences.consentCategories?.let {
                    when (it.count()) {
                        ConsentCategory.ALL.count() -> ConsentManagerConstants.GRANT_FULL_CONSENT
                        else -> ConsentManagerConstants.GRANT_PARTIAL_CONSENT
                    }
                } ?: ConsentManagerConstants.GRANT_PARTIAL_CONSENT
            }
            else -> ConsentManagerConstants.DECLINE_CONSENT
        }
    override val defaultConsentExpiry: ConsentExpiry = ConsentExpiry(365, TimeUnit.DAYS)
    override val cookieUpdateRequired: Boolean = true
    override val cookieUpdateEventName: String = "update_consent_cookie"

    override fun policyStatusInfo(): Map<String, Any> {
        return mutableMapOf(
                ConsentManagerConstants.CONSENT_POLICY to name,
                ConsentManagerConstants.CONSENT_STATUS to userConsentPreferences.consentStatus
        ).apply {
            userConsentPreferences.consentCategories?.let {
                this[ConsentManagerConstants.CONSENT_CATEGORIES] = it.toJsonArray()
            }
        }
    }

    override fun shouldQueue(): Boolean {
        return userConsentPreferences.consentStatus == ConsentStatus.UNKNOWN
    }

    override fun shouldDrop(): Boolean {
        return userConsentPreferences.consentStatus == ConsentStatus.NOT_CONSENTED
    }
}

private class CcpaConsentManagementPolicy(initialConsentPreferences: UserConsentPreferences) : ConsentManagementPolicy {

    override val name: String = ConsentPolicy.CCPA.value
    override var userConsentPreferences: UserConsentPreferences = initialConsentPreferences
    override val consentLoggingEnabled: Boolean = false
    override val consentLoggingEventName: String
        get() = if (userConsentPreferences.consentStatus == ConsentStatus.CONSENTED)
            ConsentManagerConstants.GRANT_FULL_CONSENT
        else ConsentManagerConstants.GRANT_PARTIAL_CONSENT
    override val defaultConsentExpiry: ConsentExpiry = ConsentExpiry(395, TimeUnit.DAYS)
    override val cookieUpdateRequired: Boolean = true
    override val cookieUpdateEventName: String = "set_dns_state"

    override fun policyStatusInfo(): Map<String, Any> {
        return mapOf(
                ConsentManagerConstants.CONSENT_POLICY to name,
                ConsentManagerConstants.CONSENT_DO_NOT_SELL to (userConsentPreferences.consentStatus == ConsentStatus.CONSENTED)
        )
    }

    override fun shouldQueue(): Boolean {
        return false
    }

    override fun shouldDrop(): Boolean {
        return false
    }
}