package com.tealium.mobile

import android.app.Application
import android.util.Log
import com.tealium.collectdispatcher.Collect
import com.tealium.core.*
import com.tealium.core.validation.DispatchValidator
import com.tealium.dispatcher.Dispatch
import com.tealium.lifecycle.Lifecycle
import com.tealium.location.Location
import com.tealium.remotecommanddispatcher.*
import com.tealium.tagmanagementdispatcher.TagManagementRemoteCommand
import com.tealium.tagmanagementdispatcher.TagManagement
import com.tealium.visitorservice.VisitorProfile
import com.tealium.visitorservice.VisitorService
import com.tealium.visitorservice.VisitorServiceDelegate
import com.tealium.visitorservice.visitorService

object TealiumHelper {
    lateinit var instance: Tealium

    fun init(application: Application) {
        val config = TealiumConfig(application,
                "services-christina",
                "firebase",
                Environment.DEV,
                modules = mutableSetOf(Modules.Lifecycle),
                dispatchers = mutableSetOf(Dispatchers.Collect, Dispatchers.TagManagement, Dispatchers.RemoteCommands)
        ).apply {
//            collectors.add(Collectors.Location)
            useRemoteLibrarySettings = true
        }

        instance = Tealium("instance_1", config) {
            consentManager.enabled = true
            visitorService?.delegate = object : VisitorServiceDelegate {
                override fun didUpdate(visitorProfile: VisitorProfile) {
                    Logger.dev("--", "did update vp with $visitorProfile")
                }
            }

            remoteCommands?.add(localJsonCommand)
            remoteCommands?.add(remoteJsonCommand)
            remoteCommands?.add(webviewRemoteCommand)

            val request = RemoteCommandRequest.tagManagementRequest("tealium://command?request=%7B%22foo%22%3A%22bar%22%7D")
            Logger.dev(BuildConfig.TAG, "%%%%%%%%%%%%%%%%%%% Request here: ${request.response?.requestPayload}")
        }
    }

    val webviewRemoteCommand = object : RemoteCommand("bgcolor", "testing Webview RCs") {
        override fun onInvoke(response: Response) {
            Logger.dev(BuildConfig.TAG, "ResponsePayload for webview RemoteCommand ${response.requestPayload}")
        }
    }

    val localJsonCommand = object : RemoteCommand("localJsonCommand", "testingRCs", RemoteCommandType.JSON, filename = "remoteCommand.json") {
        override fun onInvoke(response: Response) {
            Logger.dev(BuildConfig.TAG, "ResponsePayload for local JSON RemoteCommand ${response.requestPayload}")
        }
    }

    val remoteJsonCommand = object: RemoteCommand("someRemoteId", "testingRCs", RemoteCommandType.JSON, remoteUrl = "https://tags.tiqcdn.com/dle/services-christina/tagbridge/firebase.json") {
        override fun onInvoke(response: Response) {
            // ...do something here
        }
    }

    val customRemoteJsonCommand = object: RemoteCommand("someRemoteId", "testingRCs", RemoteCommandType.JSON, remoteUrl = "https://httpbin.org/json") {
        override fun onInvoke(response: Response) {
            // ...do something here
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
