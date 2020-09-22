package com.tealium.mobile

import android.app.Application
import com.tealium.collectdispatcher.Collect
import com.tealium.core.*
import com.tealium.core.consent.ConsentPolicy
import com.tealium.core.consent.consentManagerEnabled
import com.tealium.core.consent.consentManagerPolicy
import com.tealium.core.validation.DispatchValidator
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.TealiumEvent
import com.tealium.dispatcher.TealiumView
import com.tealium.hosteddatalayer.HostedDataLayer
import com.tealium.hosteddatalayer.hostedDataLayerEventMappings
import com.tealium.lifecycle.Lifecycle
import com.tealium.location.Location
import com.tealium.remotecommanddispatcher.*
import com.tealium.remotecommanddispatcher.remotecommands.JsonRemoteCommand
import com.tealium.remotecommanddispatcher.remotecommands.RemoteCommand
import com.tealium.tagmanagementdispatcher.TagManagement
import com.tealium.visitorservice.VisitorProfile
import com.tealium.visitorservice.VisitorService
import com.tealium.visitorservice.VisitorServiceDelegate
import com.tealium.visitorservice.visitorService

object TealiumHelper {

    fun init(application: Application) {
        val config = TealiumConfig(application,
                "tealiummobile",
                "android",
                Environment.DEV,
                modules = mutableSetOf(Modules.Lifecycle, Modules.VisitorService, Modules.HostedDataLayer),
                dispatchers = mutableSetOf(Dispatchers.Collect, Dispatchers.TagManagement)
        ).apply {
            collectors.add(Collectors.Location)
            useRemoteLibrarySettings = true
            hostedDataLayerEventMappings = mapOf("pdp" to "product_id")

            // Uncomment to enable Consent Management
            // consentManagerEnabled = true
            // and, uncomment one of the following lines to set the appropriate Consent Policy
            // consentManagerPolicy = ConsentPolicy.GDPR
            // consentManagerPolicy = ConsentPolicy.CCPA

        }

        Tealium.create(BuildConfig.TEALIUM_INSTANCE, config) {
            visitorService?.delegate = object : VisitorServiceDelegate {
                override fun didUpdate(visitorProfile: VisitorProfile) {
                    Logger.dev("--", "did update vp with $visitorProfile")
                }
            }

            remoteCommands?.add(localJsonCommand)
            remoteCommands?.add(webViewRemoteCommand)
        }
    }

    val webViewRemoteCommand = object : RemoteCommand("bgcolor", "testing Webview RCs") {
        override fun onInvoke(response: Response) {
            Logger.dev(BuildConfig.TAG, "ResponsePayload for webView RemoteCommand ${response.requestPayload}")
        }
    }

    val localJsonCommand = object : JsonRemoteCommand("localJsonCommand", "testingRCs", filename = "remoteCommand.json") {
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
}
