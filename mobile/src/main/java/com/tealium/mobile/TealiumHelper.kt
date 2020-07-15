package com.tealium.mobile

import android.app.Application
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
                modules = mutableSetOf(Modules.Lifecycle, Modules.VisitorService),
                dispatchers = mutableSetOf(Dispatchers.Collect, Dispatchers.TagManagement, Dispatchers.RemoteCommands)
        ).apply { collectors.add(Collectors.Location) }

        instance = Tealium("instance_1", config) {
            consentManager.enabled = true
            visitorService?.delegate = object : VisitorServiceDelegate {
                override fun didUpdate(visitorProfile: VisitorProfile) {
                    Logger.dev("--", "did update vp with $visitorProfile")
                }
            }

            remoteCommands?.add(localJsonCommand)
            remoteCommands?.add(remoteJsonCommand)
            remoteCommands?.add(customRemoteJsonCommand)
        }
    }

    val localJsonCommand = object : RemoteCommand("someJsonId", "testingRCs", RemoteCommandType.LOCAL, filename = "remoteCommands.json") {
        override fun onInvoke(response: Response) {
            // ...do something here
        }
    }

    val remoteJsonCommand = object: RemoteCommand("someRemoteId", "testingRCs", RemoteCommandType.REMOTE, remoteUrl = "https://tags.tiqcdn.com/dle/services-christina/tagbridge/firebase.json") {
        override fun onInvoke(response: Response) {
            // ...do something here
        }
    }

    val customRemoteJsonCommand = object: RemoteCommand("someRemoteId", "testingRCs", RemoteCommandType.REMOTE, remoteUrl = "https://httpbin.org/json") {
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
