package com.tealium.core.consent

import TealiumCollectorConstants.TEALIUM_VISITOR_ID
import com.tealium.core.*
import com.tealium.core.messaging.EventRouter
import com.tealium.core.messaging.LibrarySettingsUpdatedListener
import com.tealium.core.settings.LibrarySettings
import com.tealium.core.network.ConnectivityRetriever
import com.tealium.core.network.HttpClient
import com.tealium.core.network.NetworkClient
import com.tealium.core.validation.DispatchValidator
import com.tealium.dispatcher.Dispatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

class ConsentManager(private val config: TealiumConfig,
                     private val eventRouter: EventRouter,
                     private val visitorId: String,
                     private var librarySettings: LibrarySettings,
                     val policy: ConsentPolicy? = config.consentManagerPolicy
) : Collector, DispatchValidator, LibrarySettingsUpdatedListener {

    override val name: String = MODULE_NAME
    override var enabled: Boolean = config.consentManagerEnabled ?: false

    private val consentLoggingUrl = config.consentManagerLoggingUrl
            ?: "https://collect.tealiumiq.com/event"
    private val connectivity = ConnectivityRetriever.getInstance(config.application)
    private val consentSharedPreferences = ConsentSharedPreferences(config)
    private val consentManagementPolicy: ConsentManagementPolicy?
    private val httpClient: NetworkClient by lazy { HttpClient(config, connectivity) }
    var expiry: ConsentExpiry
    var onConsentExpiration: (()->Unit)? = config.onConsentExpiration

    init {
        consentManagementPolicy = policy?.create(UserConsentPreferences(userConsentStatus, userConsentCategories))
        expiry = config.consentExpiry ?: consentManagementPolicy?.defaultConsentExpiry ?: ConsentExpiry(365, TimeUnit.DAYS)
        expireConsent()
    }

    /**
     * Used by the Consent Manager module to determine if the consent selections are expired.
     */
    var consentLastSet: Long?
        get() = consentSharedPreferences?.lastSet
        set(value) {
            consentSharedPreferences?.lastSet = value
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
            if (value != ConsentStatus.UNKNOWN) {
                consentLastSet = System.currentTimeMillis()
            }
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
    var isConsentLoggingEnabled = config.consentManagerLoggingEnabled ?: false

    /**
     * Sends an HTTP request with the current policy status information to the configured endpoint.
     */
    private fun logConsentUpdate() {
        consentManagementPolicy?.let { policy ->
            if (policy.consentLoggingEnabled) {
                if ((connectivity.isConnected() && librarySettings.wifiOnly) || connectivity.isConnectedWifi()) {
                    // TODO: consider implementing a general network queue
                    CoroutineScope(IO).launch {
                        val json = JSONObject()
                        policy.policyStatusInfo().forEach {
                            json.put(it.key, it.value)
                        }
                        json.put(CoreConstant.TEALIUM_EVENT, policy.consentLoggingEventName)

                        json.put(TEALIUM_ACCOUNT, config.accountName)
                        json.put(TEALIUM_PROFILE, config.profileName)
                        json.put(TEALIUM_VISITOR_ID, visitorId)

                        httpClient.post(json.toString(), consentLoggingUrl, false)
                    }
                }
            }
        }
    }

    /**
     * Checks if the consent selections are expired.
     * If so, resets consent preferences and triggers optional callback.
     */
    fun expireConsent() {
        consentLastSet?.let {
            if (isExpired(it)) {
                userConsentStatus = ConsentStatus.UNKNOWN
                onConsentExpiration?.invoke()
            }
        }
    }

    /**
     * Checks if value is expired.
     */
    fun isExpired(timestamp: Long): Boolean {
        if (timestamp == 0.toLong()) { return false }
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
    private fun setUserConsentStatus(userConsentStatus: ConsentStatus, userConsentCategories: Set<ConsentCategory>?) {
        consentSharedPreferences.setConsentStatus(userConsentStatus, userConsentCategories)
        notifyPreferencesUpdated(userConsentStatus, userConsentCategories)
    }

    /**
     * Constructs a new [UserConsentPreferences] object with the current state and notifies the [ConsentPolicy]
     * that's in force and then any any other listeners.
     */
    private fun notifyPreferencesUpdated(userConsentStatus: ConsentStatus, userConsentCategories: Set<ConsentCategory>?) {
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
        return if (userConsentStatus == ConsentStatus.UNKNOWN && consentManagementPolicy != null)
            consentManagementPolicy.policyStatusInfo()
        else emptyMap()
    }

    override fun onLibrarySettingsUpdated(settings: LibrarySettings) {
        this.librarySettings = settings
    }

    companion object {
        const val MODULE_NAME = "CONSENT_MANAGER"
    }
}