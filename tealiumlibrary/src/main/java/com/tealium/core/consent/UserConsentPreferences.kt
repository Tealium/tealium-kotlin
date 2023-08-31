package com.tealium.core.consent

import com.tealium.dispatcher.Dispatch
import org.json.JSONArray
import java.lang.Exception
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
            return when (value.lowercase(Locale.ROOT)) {
                CONSENTED.value.lowercase(Locale.ROOT) -> CONSENTED
                NOT_CONSENTED.value.lowercase(Locale.ROOT) -> NOT_CONSENTED
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
            return when (category.lowercase(Locale.ROOT)) {
                AFFILIATES.value.lowercase(Locale.ROOT) -> AFFILIATES
                ANALYTICS.value.lowercase(Locale.ROOT) -> ANALYTICS
                BIG_DATA.value.lowercase(Locale.ROOT) -> BIG_DATA
                CDP.value.lowercase(Locale.ROOT) -> CDP
                COOKIEMATCH.value.lowercase(Locale.ROOT) -> COOKIEMATCH
                CRM.value.lowercase(Locale.ROOT) -> CRM
                DISPLAY_ADS.value.lowercase(Locale.ROOT) -> DISPLAY_ADS
                EMAIL.value.lowercase(Locale.ROOT) -> EMAIL
                ENGAGEMENT.value.lowercase(Locale.ROOT) -> ENGAGEMENT
                MOBILE.value.lowercase(Locale.ROOT) -> MOBILE
                MONITORING.value.lowercase(Locale.ROOT) -> MONITORING
                PERSONALIZATION.value.lowercase(Locale.ROOT) -> PERSONALIZATION
                SEARCH.value.lowercase(Locale.ROOT) -> SEARCH
                SOCIAL.value.lowercase(Locale.ROOT) -> SOCIAL
                MISC.value.lowercase(Locale.ROOT) -> MISC
                else -> null
            }
        }

        fun consentCategories(categories: Set<String>): Set<ConsentCategory> {
            return categories.mapNotNull { consentCategory(it) }.toSet()
        }
    }
}

enum class ConsentPolicy(val value: String) {
    /**
     * Supports the General Data Protection Regulation.
     */
    GDPR("gdpr") {
        override fun create(userConsentPreferences: UserConsentPreferences): ConsentManagementPolicy {
            return GdprConsentManagementPolicy(userConsentPreferences)
        }

        override fun setCustomPolicy(policy: ConsentManagementPolicy) {
            // do nothing
        }
    },

    /**
     * Supports the California Consumer Privacy Act.
     */
    CCPA("ccpa") {
        override fun create(userConsentPreferences: UserConsentPreferences): ConsentManagementPolicy {
            return CcpaConsentManagementPolicy(userConsentPreferences)
        }

        override fun setCustomPolicy(policy: ConsentManagementPolicy) {
            // do nothing
        }
    },

    /**
     * Uses a user-provided [ConsentManagementPolicy] that should be provided via the
     * [setCustomPolicy] method prior to initialization of the Tealium SDK
     *
     * The latest copy of the [UserConsentPreferences] will be provided to the custom policy during
     * initialization.
     */
    CUSTOM("custom") {
        private var customPolicy: ConsentManagementPolicy? = null
        override fun create(userConsentPreferences: UserConsentPreferences): ConsentManagementPolicy {
            customPolicy?.let { policy ->
                policy.userConsentPreferences = userConsentPreferences
                return policy
            }
            throw Exception("Custom policy must have a ConsentManagementPolicy assigned. Ensure you have set one using setCustomPolicy(..)")
        }

        override fun setCustomPolicy(policy: ConsentManagementPolicy) {
            customPolicy = policy
        }
    };

    /**
     * Provides a ConsentManagementPolicy implementation for the chosen enum value
     */
    abstract fun create(userConsentPreferences: UserConsentPreferences): ConsentManagementPolicy

    /**
     * Assigns a custom [ConsentManagementPolicy] to be used. Only relevant for [CUSTOM] option, all
     * other options will ignore this.
     */
    abstract fun setCustomPolicy(policy: ConsentManagementPolicy)
}

interface ConsentManagementPolicy {

    /**
     * The name of the [ConsentManagementPolicy]
     */
    val name: String

    /**
     * The current [UserConsentPreferences].
     * This will be automatically updated by the [ConsentManager] when the preferences change.
     */
    var userConsentPreferences: UserConsentPreferences

    /**
     * Sets whether or not logging of consent changes are required. If set to true then ensure that
     * the logging destination is appropriate on [com.tealium.core.TealiumConfig.consentManagerLoggingUrl]
     */
    val consentLoggingEnabled: Boolean

    /**
     * Sets the event name (key: tealium_event) to use when logging a change in consent.
     */
    val consentLoggingEventName: String

    /**
     * Sets the default expiry time for this [ConsentManagementPolicy]. This is also overridable
     * from [com.tealium.core.TealiumConfig.consentExpiry]
     */
    val defaultConsentExpiry: ConsentExpiry

    /**
     * Sets whether or not to update a cookie in the TagManagement module's webview.
     */
    val cookieUpdateRequired: Boolean

    /**
     * Sets the event name to use when [cookieUpdateRequired] is set to true.
     */
    val cookieUpdateEventName: String

    /**
     * Returns a map of key value data to be added to the payload of each [com.tealium.dispatcher.Dispatch]
     * e.g.
     * ```
     * mapOf(
     *     ConsentManagerConstants.CONSENT_POLICY to name,
     *     ConsentManagerConstants.CONSENT_STATUS to userConsentPreferences.consentStatus
     * )
     * ```
     */
    fun policyStatusInfo(): Map<String, Any>

    /**
     * Returns whether or not any Dispatches should be queued according to the ConsentPolicy rules.
     * A return value of true will enqueue this and any further dispatches until a false return
     * value is returned, assuming other validators also agree.
     *
     * See [com.tealium.core.validation.DispatchValidator]
     *
     * @return true if dispatches should be queued (e.g. a [ConsentStatus.UNKNOWN] or expired state
     * of consent); otherwise false
     */
    fun shouldQueue(): Boolean

    /**
     * Returns whether or not any Dispatches should be dropped according to the ConsentPolicy rules.
     * A return value of true will stop the dispatch propagating any further through the SDK.
     *
     * See: [com.tealium.core.validation.DispatchValidator]
     *
     * @return true if dispatches should be dropped (e.g. a [ConsentStatus.NOT_CONSENTED] state of
     * consent) otherwise false.
     */
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
        return mutableMapOf<String, Any>(
            Dispatch.Keys.CONSENT_POLICY to name,
            Dispatch.Keys.CONSENT_STATUS to userConsentPreferences.consentStatus.value
        ).apply {
            userConsentPreferences.consentCategories?.let { it ->
                this[Dispatch.Keys.CONSENT_CATEGORIES] = it
                    .map { category -> category.value }
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
            Dispatch.Keys.CONSENT_POLICY to name,
            Dispatch.Keys.CONSENT_DO_NOT_SELL to (userConsentPreferences.consentStatus == ConsentStatus.CONSENTED)
        )
    }

    override fun shouldQueue(): Boolean {
        return false
    }

    override fun shouldDrop(): Boolean {
        return false
    }
}