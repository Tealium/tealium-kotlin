package com.tealium.mobile

import android.app.Application
import com.android.billingclient.api.Purchase
import com.tealium.adidentifier.AdIdentifier
import com.tealium.autotracking.*
import com.tealium.collectdispatcher.Collect
import com.tealium.core.*
import com.tealium.core.consent.*
import com.tealium.core.events.EventTrigger
import com.tealium.core.messaging.UserConsentPreferencesUpdatedListener
import com.tealium.core.validation.DispatchValidator
import com.tealium.crashreporter.CrashReporter
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.TealiumEvent
import com.tealium.dispatcher.TealiumView
import com.tealium.hosteddatalayer.HostedDataLayer
import com.tealium.hosteddatalayer.hostedDataLayerEventMappings
import com.tealium.inapppurchase.InAppPurchaseManager
import com.tealium.inapppurchase.inAppPurchaseManager
import com.tealium.lifecycle.Lifecycle
import com.tealium.media.Media
import com.tealium.media.mediaBackgroundSessionEnabled
import com.tealium.media.mediaBackgroundSessionEndInterval
import com.tealium.remotecommanddispatcher.RemoteCommands
import com.tealium.remotecommanddispatcher.remoteCommands
import com.tealium.remotecommands.RemoteCommand
import com.tealium.tagmanagementdispatcher.TagManagement
import com.tealium.visitorservice.VisitorProfile
import com.tealium.visitorservice.VisitorService
import com.tealium.visitorservice.VisitorUpdatedListener
import com.tealium.momentsapi.EngineResponse
import com.tealium.momentsapi.ErrorCode
import com.tealium.momentsapi.MomentsApi
import com.tealium.momentsapi.MomentsApiRegion
import com.tealium.momentsapi.ResponseListener
import com.tealium.momentsapi.momentsApi
import com.tealium.momentsapi.momentsApiRegion
import java.util.concurrent.TimeUnit

object TealiumHelper : ActivityDataCollector {

    fun init(application: Application) {
        val config = TealiumConfig(
            application,
            "tealiummobile",
            "demo",
            Environment.DEV,
            modules = mutableSetOf(
                Modules.Lifecycle,
                Modules.VisitorService,
                Modules.HostedDataLayer,
                Modules.CrashReporter,
                Modules.AdIdentifier,
                Modules.InAppPurchaseManager,
                Modules.AutoTracking,
                Modules.Media,
                QueryParamProviderModule,
                Modules.MomentsApi
            ),
            dispatchers = mutableSetOf(
                Dispatchers.Collect,
                Dispatchers.TagManagement,
                Dispatchers.RemoteCommands
            )
        ).apply {
            useRemoteLibrarySettings = true
//            sessionCountingEnabled = false
            hostedDataLayerEventMappings = mapOf("pdp" to "product_id")
            // Uncomment one of the following lines to set the appropriate Consent Policy
            // and enable the consent manager
            consentManagerPolicy = ConsentPolicy.GDPR
            // consentManagerPolicy = ConsentPolicy.CCPA
            consentExpiry = ConsentExpiry(1, TimeUnit.DAYS)

            timedEventTriggers = mutableListOf(
                EventTrigger.forEventName("start_event", "end_event")
            )

            mediaBackgroundSessionEnabled = false
            mediaBackgroundSessionEndInterval = 5000L  // end session after 5 seconds

            autoTrackingMode =
                if (BuildConfig.AUTO_TRACKING) AutoTrackingMode.FULL else AutoTrackingMode.NONE
            // autoTrackingBlocklistFilename = "autotracking-blocklist.json"
            // autoTrackingBlocklistUrl = "https://tags.tiqcdn.com/dle/tealiummobile/android/autotracking-blocklist.json"
            autoTrackingCollectorDelegate = TealiumHelper
            // overrideConsentCategoriesKey = "my_consent_categories_key"

            visitorIdentityKey = BuildConfig.IDENTITY_KEY
            momentsApiRegion = MomentsApiRegion.US_EAST
        }

        Tealium.create(BuildConfig.TEALIUM_INSTANCE, config) {
            events.subscribe(object : UserConsentPreferencesUpdatedListener {
                override fun onUserConsentPreferencesUpdated(
                    userConsentPreferences: UserConsentPreferences,
                    policy: ConsentManagementPolicy
                ) {
                    if (userConsentPreferences.consentStatus == ConsentStatus.UNKNOWN) {
                        Logger.dev(BuildConfig.TAG, "Re-prompt for consent")
                    }
                }
            })

            events.subscribe(object : VisitorUpdatedListener {
                override fun onVisitorUpdated(visitorProfile: VisitorProfile) {
                    Logger.dev("--", "did update vp with $visitorProfile")
                }
            })

            remoteCommands?.add(localJsonCommand, filename = "remoteCommand.json")
            remoteCommands?.add(webViewRemoteCommand)
        }
    }

    val webViewRemoteCommand = object : RemoteCommand("bgcolor", "testing Webview RCs") {
        override fun onInvoke(response: Response) {
            Logger.dev(
                BuildConfig.TAG,
                "ResponsePayload for webView RemoteCommand ${response.requestPayload}"
            )
        }
    }

    val localJsonCommand = object : RemoteCommand("localJsonCommand", "testingRCs") {
        override fun onInvoke(response: Response) {
            Logger.dev(
                BuildConfig.TAG,
                "ResponsePayload for local JSON RemoteCommand ${response.requestPayload}"
            )
        }
    }

    fun fetchConsentCategories(): String? {
        return Tealium[BuildConfig.TEALIUM_INSTANCE]?.consentManager?.userConsentCategories?.joinToString(
            ","
        )
    }

    fun setConsentCategories(categories: Set<String>) {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.consentManager?.userConsentCategories =
            ConsentCategory.consentCategories(categories)
    }

    fun trackView(name: String, data: Map<String, Any>?) {
        val viewDispatch = TealiumView(name, data)
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.track(viewDispatch)
    }

    fun trackEvent(name: String, data: Map<String, Any>?) {
        val eventDispatch = TealiumEvent(name, data)
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.track(eventDispatch)
    }

    fun trackPurchase(purchase: Purchase, data: Map<String, Any>?) {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.inAppPurchaseManager?.trackInAppPurchase(
            purchase,
            data
        )
    }

    fun getMomentsVisitorData() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.momentsApi?.fetchEngineResponse(
            "4625fd31-cd87-444e-9470-7467f2e963ba",
            object :
                ResponseListener<EngineResponse> {
                override fun success(data: EngineResponse) {
                    Logger.dev(BuildConfig.TAG, "Visitor data badges: ${data.badges.toString()}")
                    Logger.dev(BuildConfig.TAG, "Visitor data audiences: ${data.audiences.toString()}")
                    Logger.dev(BuildConfig.TAG, "Visitor data properties: ${data.attributes.toString()}")
                    Logger.dev(BuildConfig.TAG, "Visitor data string properties: ${data.strings.toString()}")
                    Logger.dev(BuildConfig.TAG, "Visitor data booleans properties: ${data.booleans.toString()}")
                    Logger.dev(BuildConfig.TAG, "Visitor data dates properties: ${data.dates.toString()}")
                    Logger.dev(BuildConfig.TAG, "Visitor data numbers properties: ${data.numbers.toString()}")
                }

                override fun failure(errorCode: ErrorCode, message: String) {
                    Logger.dev(BuildConfig.TAG, "Moments API Error - ${errorCode.value}: $message")
                }
            }
        )
    }

    fun retrieveDatalayer(): Map<String, Any>? {
        return Tealium[BuildConfig.TEALIUM_INSTANCE]?.gatherTrackData()
    }

    val customValidator: DispatchValidator by lazy {
        object : DispatchValidator {
            override fun shouldQueue(dispatch: Dispatch?): Boolean {
                Logger.dev(BuildConfig.TAG, "shouldQueue: CustomValidator")
                return false
            }

            override fun shouldDrop(dispatch: Dispatch): Boolean {
                Logger.dev(BuildConfig.TAG, "shouldDrop: CustomValidator")
                return false
            }

            override val name: String = "my validator"
            override var enabled: Boolean = true
        }
    }

    override fun onCollectActivityData(activityName: String): Map<String, Any>? {
        return mapOf("global_data" to "value")
    }
}

class QueryParamProviderModule : Module, QueryParameterProvider {
    override val name: String = "SampleQueryParamProvider"
    override var enabled: Boolean = true

    override suspend fun provideParameters(): Map<String, List<String>> {
        return mapOf(
            "query_param1" to listOf("QueryParamProvider_value1"),
            "query_param2" to listOf("QueryParamProvider_value2"),
        )
    }

    companion object : ModuleFactory {
        override fun create(context: TealiumContext): Module {
            return QueryParamProviderModule()
        }
    }
}
