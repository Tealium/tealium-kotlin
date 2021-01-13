package com.tealium.core.consent

import com.tealium.core.TealiumConfig

@Deprecated("This key is being removed. Setting/not setting a valid ConsentManagerPolicy " +
        "will enable/disable the ConsentManager by default.")
const val CONSENT_MANAGER_ENABLED = "consent_manager_enabled"
const val CONSENT_MANAGER_LOGGING_ENABLED = "consent_manager_logging_enabled"
const val CONSENT_MANAGER_LOGGING_URL = "consent_manager_logging_url"
const val CONSENT_MANAGER_POLICY = "consent_manager_policy"
const val CONSENT_EXPIRY = "consent_expiry"

var TealiumConfig.consentManagerEnabled: Boolean?
    get() = (options[CONSENT_MANAGER_ENABLED] as? Boolean).let {
        return if (it != null) {
            it
        } else {
            consentManagerPolicy != null
        }
    }
    @Deprecated("Setting of this property is no longer required - Instead, setting a valid" +
            " ConsentPolicy on config.consentManagerPolicy will enable the ConsentManager",
            level = DeprecationLevel.WARNING)
    set(value) {
        value?.let {
            options[CONSENT_MANAGER_ENABLED] = it
        }
    }

var TealiumConfig.consentManagerLoggingEnabled: Boolean?
    get() = options[CONSENT_MANAGER_LOGGING_ENABLED] as? Boolean
    set(value) {
        value?.let {
            options[CONSENT_MANAGER_LOGGING_ENABLED] = it
        }
    }

var TealiumConfig.consentManagerLoggingUrl: String?
    get() = options[CONSENT_MANAGER_LOGGING_URL] as? String
    set(value) {
        value?.let {
            options[CONSENT_MANAGER_LOGGING_URL] = it
        }
    }

var TealiumConfig.consentManagerPolicy: ConsentPolicy?
    get() = options[CONSENT_MANAGER_POLICY] as? ConsentPolicy
    set(value) {
        value?.let {
            options[CONSENT_MANAGER_POLICY] = it
        }
    }

/**
 * Sets the consent expiration.
 */
var TealiumConfig.consentExpiry: ConsentExpiry?
    get() = options[CONSENT_EXPIRY] as? ConsentExpiry
    set(value) {
        value?.let {
            options[CONSENT_EXPIRY] = it
        }
    }