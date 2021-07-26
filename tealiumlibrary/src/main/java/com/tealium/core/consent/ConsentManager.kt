package com.tealium.core.consent

import com.tealium.core.*
import com.tealium.core.messaging.EventRouter
import com.tealium.core.messaging.LibrarySettingsUpdatedListener
import com.tealium.core.settings.LibrarySettings
import com.tealium.core.validation.DispatchValidator
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.TealiumEvent
import com.tealium.tealiumlibrary.BuildConfig
import java.lang.Exception
import java.util.concurrent.TimeUnit

class ConsentManager(
    private val context: TealiumContext,
    private val eventRouter: EventRouter,
    private var librarySettings: LibrarySettings,
    val policy: ConsentPolicy? = context.config.consentManagerPolicy
) : Collector, DispatchValidator, LibrarySettingsUpdatedListener {

    override val name: String = MODULE_NAME
    override var enabled: Boolean = context.config.consentManagerEnabled ?: false
    private val consentSharedPreferences = ConsentSharedPreferences(context.config)
    private val consentManagementPolicy: ConsentManagementPolicy?
    val expiry: ConsentExpiry

    init {
        consentManagementPolicy = try {
            policy?.create(UserConsentPreferences(userConsentStatus, userConsentCategories))
        } catch (ex: Exception) {
            Logger.qa(BuildConfig.TAG, "Error creating ConsentPolicy: ${ex.message}")
            null
        }
        expiry = context.config.consentExpiry ?: consentManagementPolicy?.defaultConsentExpiry
                ?: ConsentExpiry(365, TimeUnit.DAYS)
        expireConsent()
    }

    /**
     * Used by the Consent Manager module to determine if the consent selections are expired.
     */
    private var lastConsentUpdate: Long?
        get() = consentSharedPreferences.lastUpdate
        set(value) {
            consentSharedPreferences.lastUpdate = value
        }

    /**
     * Sets the current Consent Status.
     * Initial status is [ConsentStatus.UNKNOWN] and will instruct the SDK to queue any [Dispatch] items until an
     * explicit Consent Status is provided.
     * Setting this value to [ConsentStatus.CONSENTED] will also set the [userConsentCategories] to
     * all available categories. To opt-in to only a subset of categories then use either
     * [userConsentCategories] or [setUserConsentStatus].
     */
    var userConsentStatus: ConsentStatus
        get() = consentSharedPreferences.consentStatus
        set(value) {
            lastConsentUpdate = System.currentTimeMillis()
            when (value) {
                ConsentStatus.CONSENTED -> setUserConsentStatus(value, ConsentCategory.ALL)
                ConsentStatus.NOT_CONSENTED -> setUserConsentStatus(value, null)
                ConsentStatus.UNKNOWN -> setUserConsentStatus(value, null)
            }
        }

    /**
     * Sets the current set of categories that the user has consented to.
     * Initial status is null and is therefore not opted in by default.
     * Setting this value to any subset of categories will also set [userConsentStatus] to
     * [ConsentStatus.CONSENTED]
     */
    var userConsentCategories: Set<ConsentCategory>?
        get() = consentSharedPreferences.consentCategories
        set(value) {
            if (value.isNullOrEmpty()) {
                setUserConsentStatus(ConsentStatus.NOT_CONSENTED, null)
            } else {
                setUserConsentStatus(ConsentStatus.CONSENTED, value)
            }
        }

    /**
     * Sets whether Consent should be logged.
     */
    var isConsentLoggingEnabled = context.config.consentManagerLoggingEnabled ?: false

    /**
     * Sends an HTTP request with the current policy status information to the configured endpoint.
     */
    private fun logConsentUpdate() {
        consentManagementPolicy?.let { policy ->
            if (policy.consentLoggingEnabled) {
                // profile override checked in dispatchers, url override checked in Collect dispatcher
                context.track(TealiumEvent(policy.consentLoggingEventName, policy.policyStatusInfo()))
            }
        }
    }

    /**
     * Checks if the consent selections are expired.
     * If so, resets consent preferences and triggers optional callback.
     */
    private fun expireConsent() {
        lastConsentUpdate?.let {
            if (isExpired(it)) {
                userConsentStatus = ConsentStatus.UNKNOWN
            }
        }
    }

    /**
     * Checks if value is expired.
     */
    private fun isExpired(timestamp: Long): Boolean {
        if (timestamp == 0.toLong()) {
            return false
        }
        return (timestamp <
                System.currentTimeMillis() - expiry.unit.toMillis(expiry.time))
    }

    /**
     * Resets the chosen ConsentStatus and set of ConsentCategory back to their defaults.
     */
    fun reset() {
        consentSharedPreferences.reset()
        notifyPreferencesUpdated(userConsentStatus, userConsentCategories)
    }

    /**
     * Sets the given ConsentStatus and set of ConsentCategory items into storage. Also notifies
     * any listeners.
     */
    private fun setUserConsentStatus(
        userConsentStatus: ConsentStatus,
        userConsentCategories: Set<ConsentCategory>?
    ) {
        if (consentSharedPreferences.consentStatus == userConsentStatus && consentSharedPreferences.consentCategories == userConsentCategories) return

        consentSharedPreferences.setConsentStatus(userConsentStatus, userConsentCategories)
        notifyPreferencesUpdated(userConsentStatus, userConsentCategories)
    }

    /**
     * Constructs a new [UserConsentPreferences] object with the current state and notifies the [ConsentPolicy]
     * that's in force and then any any other listeners.
     */
    private fun notifyPreferencesUpdated(
        userConsentStatus: ConsentStatus,
        userConsentCategories: Set<ConsentCategory>?
    ) {
        consentManagementPolicy?.let {
            val preferences = UserConsentPreferences(userConsentStatus, userConsentCategories)
            it.userConsentPreferences = preferences
            eventRouter.onUserConsentPreferencesUpdated(preferences, it)

            if (isConsentLoggingEnabled) {
                logConsentUpdate()
            }
        }
    }

    /**
     * Delegates whether or not to queue this event to the current [ConsentPolicy] in force, otherwise false
     */
    override fun shouldQueue(dispatch: Dispatch?): Boolean {
        expireConsent()
        return consentManagementPolicy?.shouldQueue()
            ?: false
    }

    /**
     * Delegates whether or not to drop this event to the current [ConsentPolicy] in force, otherwise false
     */
    override fun shouldDrop(dispatch: Dispatch): Boolean {
        return consentManagementPolicy?.shouldDrop()
            ?: false
    }

    /**
     * Returns the status information from the current [ConsentPolicy] in force, else an empty map.
     */
    override suspend fun collect(): Map<String, Any> {
        return if (userConsentStatus != ConsentStatus.UNKNOWN && consentManagementPolicy != null)
            consentManagementPolicy.policyStatusInfo()
        else emptyMap()
    }

    override fun onLibrarySettingsUpdated(settings: LibrarySettings) {
        this.librarySettings = settings
    }

    companion object {
        const val MODULE_NAME = "ConsentManager"
        const val MODULE_VERSION = BuildConfig.LIBRARY_VERSION

        fun isConsentGrantedEvent(dispatch: Dispatch): Boolean {
            return (ConsentManagerConstants.GRANT_FULL_CONSENT == dispatch[CoreConstant.TEALIUM_EVENT]
                    || ConsentManagerConstants.GRANT_PARTIAL_CONSENT == dispatch[CoreConstant.TEALIUM_EVENT])
        }
    }
}