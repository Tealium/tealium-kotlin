package com.tealium.mobile

import android.app.Application
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.tealium.adidentifier.AdIdentifier
import com.tealium.autotracking.*
import com.tealium.collectdispatcher.Collect
import com.tealium.core.*
import com.tealium.core.consent.*
import com.tealium.core.events.EventTrigger
import com.tealium.core.messaging.UserConsentPreferencesUpdatedListener
import com.tealium.core.persistence.Expiry
import com.tealium.core.validation.DispatchValidator
import com.tealium.crashreporter.CrashReporter
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.TealiumEvent
import com.tealium.dispatcher.TealiumView
import com.tealium.hosteddatalayer.HostedDataLayer
import com.tealium.hosteddatalayer.hostedDataLayerEventMappings
import com.tealium.lifecycle.Lifecycle
import com.tealium.remotecommanddispatcher.RemoteCommands
import com.tealium.remotecommanddispatcher.remoteCommands
import com.tealium.remotecommands.RemoteCommand
import com.tealium.tagmanagementdispatcher.TagManagement
import com.tealium.visitorservice.VisitorProfile
import com.tealium.visitorservice.VisitorService
import com.tealium.visitorservice.VisitorUpdatedListener
import java.util.concurrent.TimeUnit

object TealiumHelper : ActivityDataCollector {

    fun init(application: Application) {
        val config = TealiumConfig(application,
                "tealiummobile",
                "android",
                Environment.DEV,
                modules = mutableSetOf(Modules.Lifecycle, Modules.VisitorService, Modules.HostedDataLayer, Modules.CrashReporter, Modules.AdIdentifier, Modules.AutoTracking),
                dispatchers = mutableSetOf(Dispatchers.Collect, Dispatchers.TagManagement, Dispatchers.RemoteCommands)
        ).apply {
            useRemoteLibrarySettings = true
            hostedDataLayerEventMappings = mapOf("pdp" to "product_id")

            // Uncomment one of the following lines to set the appropriate Consent Policy
            // and enable the consent manager
            consentManagerPolicy = ConsentPolicy.GDPR
            // consentManagerPolicy = ConsentPolicy.CCPA
            consentExpiry = ConsentExpiry(1, TimeUnit.DAYS)

            timedEventTriggers = mutableListOf(
                    EventTrigger.forEventName("start_event", "end_event")
            )

            autoTrackingMode = if (BuildConfig.AUTO_TRACKING) AutoTrackingMode.FULL else AutoTrackingMode.NONE
            // autoTrackingBlocklistFilename = "autotracking-blocklist.json"
            // autoTrackingBlocklistUrl = "https://tags.tiqcdn.com/dle/tealiummobile/android/autotracking-blocklist.json"
            autoTrackingCollectorDelegate = TealiumHelper
        }

        Tealium.create(BuildConfig.TEALIUM_INSTANCE, config) {
            events.subscribe(object : UserConsentPreferencesUpdatedListener {
                override fun onUserConsentPreferencesUpdated(userConsentPreferences: UserConsentPreferences,
                                                             policy: ConsentManagementPolicy) {
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

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(BuildConfig.TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log
            Log.d(BuildConfig.TAG, "FCM Token: $token")
        })
    }

    val webViewRemoteCommand = object : RemoteCommand("bgcolor", "testing Webview RCs") {
        override fun onInvoke(response: Response) {
            Logger.dev(BuildConfig.TAG, "ResponsePayload for webView RemoteCommand ${response.requestPayload}")
        }
    }

    val localJsonCommand = object : RemoteCommand("localJsonCommand", "testingRCs") {
        override fun onInvoke(response: Response) {
            Logger.dev(BuildConfig.TAG, "ResponsePayload for local JSON RemoteCommand ${response.requestPayload}")
        }
    }

    fun trackView(name: String, data: Map<String, Any>?) {
        val viewDispatch = TealiumView(name, data)
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.track(viewDispatch)
    }

    fun trackEvent(name: String, data: Map<String, Any>?) {
        val eventDispatch = TealiumEvent(name, data)
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.track(eventDispatch)
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
