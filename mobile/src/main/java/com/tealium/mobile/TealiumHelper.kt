package com.tealium.mobile

import android.app.Application
import com.android.billingclient.api.Purchase
import android.util.Log
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
import com.tealium.transformations.TransformationModule
import com.tealium.transformations.transformations
import com.tealium.visitorservice.VisitorProfile
import com.tealium.visitorservice.VisitorService
import com.tealium.visitorservice.VisitorUpdatedListener
import com.tealium.visitorservice.overrideVisitorServiceProfile
import java.util.concurrent.TimeUnit

object TealiumHelper : ActivityDataCollector {

    fun init(application: Application) {
        val config = TealiumConfig(application,
                "tealiummobile",
                "android",
                Environment.DEV,
                modules = mutableSetOf(
                        TransformationModule
//                        Modules.Lifecycle,
//                        Modules.VisitorService,
//                        Modules.HostedDataLayer,
//                        Modules.CrashReporter,
//                        Modules.AdIdentifier,
//                        Modules.InAppPurchaseManager,
//                        Modules.AutoTracking,
//                        Modules.Media
                ),
                dispatchers = mutableSetOf(
                    Dispatchers.Collect,
//                    Dispatchers.TagManagement,
                    Dispatchers.RemoteCommands
                )
        )

        Tealium.create(BuildConfig.TEALIUM_INSTANCE, config) {
            remoteCommands?.add(localJsonCommand, filename = "remoteCommand.json")
//            remoteCommands?.add(webViewRemoteCommand)
        }
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

    fun fetchConsentCategories(): String? {
        return Tealium[BuildConfig.TEALIUM_INSTANCE]?.consentManager?.userConsentCategories?.joinToString(",")
    }

    fun setConsentCategories(categories: Set<String>) {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.consentManager?.userConsentCategories = ConsentCategory.consentCategories(categories)
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
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.inAppPurchaseManager?.trackInAppPurchase(purchase, data)
    }

    fun retrieveDatalayer() : Map<String, Any>? {
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

    fun testJs(js: String, file: String? = null) {
        if (file != null) {
            Tealium[BuildConfig.TEALIUM_INSTANCE]?.transformations?.executeJavascript(js, file)
        } else {
            Tealium[BuildConfig.TEALIUM_INSTANCE]?.transformations?.executeJavascript(js)
        }
    }
}
