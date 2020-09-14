package com.tealium.mobile

import android.app.Application
import com.tealium.collectdispatcher.Collect
import com.tealium.core.*
import com.tealium.core.validation.DispatchValidator
import com.tealium.dispatcher.Dispatch
import com.tealium.hosteddatalayer.hostedDataLayerEventMappings
import com.tealium.lifecycle.Lifecycle
import com.tealium.remotecommanddispatcher.RemoteCommands
import com.tealium.remotecommanddispatcher.remoteCommands
import com.tealium.remotecommanddispatcher.remotecommands.JsonRemoteCommand
import com.tealium.remotecommands.RemoteCommand
import com.tealium.tagmanagementdispatcher.TagManagement
import com.tealium.visitorservice.VisitorProfile
import com.tealium.visitorservice.VisitorServiceDelegate
import com.tealium.visitorservice.visitorService

object TealiumHelper {
    lateinit var instance: Tealium

    fun init(application: Application) {
        val config = TealiumConfig(application,
                "tealiummobile",
                "android",
                Environment.DEV,
                modules = mutableSetOf(Modules.Lifecycle),//, Modules.VisitorService, Modules.HostedDataLayer),
                dispatchers = mutableSetOf(Dispatchers.Collect, Dispatchers.TagManagement, Dispatchers.RemoteCommands)
        ).apply {
            useRemoteLibrarySettings = true
            hostedDataLayerEventMappings = mapOf("pdp" to "product_id")
        }

        instance = Tealium("instance_1", config) {
            consentManager.enabled = true
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
