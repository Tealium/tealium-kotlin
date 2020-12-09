package com.tealium.core.consent

import com.tealium.core.TealiumConfig
import com.tealium.core.persistence.Expiry
import java.util.concurrent.TimeUnit

const val CONSENT_MANAGER_ENABLED = "consent_manager_enabled"
const val CONSENT_MANAGER_LOGGING_ENABLED = "consent_manager_logging_enabled"
const val CONSENT_MANAGER_LOGGING_URL = "consent_manager_logging_url"
const val CONSENT_MANAGER_POLICY = "consent_manager_policy"
const val CONSENT_EXPIRY = "consent_expiry"
const val CONSENT_EXPIRY_CALLBACK = "consent_expiry_callback"

var TealiumConfig.consentManagerEnabled: Boolean?
    get() = options[CONSENT_MANAGER_ENABLED] as? Boolean
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
var TealiumConfig.consentExpiry: ConsentExpiry
    get() = options[CONSENT_EXPIRY] as? ConsentExpiry ?: ConsentExpiry(365, TimeUnit.DAYS)
    set(value) {
        value?.let {
            options[CONSENT_EXPIRY] = it
        }
    }

/**
 * Sets an optional callback to be triggered upon consent expiration.
 */
var TealiumConfig.onConsentExpiration: (()->Unit)?
    get() = options[CONSENT_EXPIRY_CALLBACK] as? (()->Unit)
    set(value) {
        value?.let {
            options[CONSENT_EXPIRY_CALLBACK] = it
        }
    }