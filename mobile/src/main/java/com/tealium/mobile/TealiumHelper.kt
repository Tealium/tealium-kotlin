package com.tealium.mobile

import android.app.Application
import com.tealium.collectdispatcher.Collect
import com.tealium.core.*
import com.tealium.core.validation.DispatchValidator
import com.tealium.dispatcher.Dispatch
import com.tealium.hosteddatalayer.HostedDataLayer
import com.tealium.hosteddatalayer.hostedDataLayerEventMappings
import com.tealium.lifecycle.Lifecycle
import com.tealium.location.Location
import com.tealium.tagmanagementdispatcher.TagManagement
import com.tealium.visitorservice.VisitorProfile
import com.tealium.visitorservice.VisitorService
import com.tealium.visitorservice.VisitorServiceDelegate
import com.tealium.visitorservice.visitorService

object TealiumHelper {
    lateinit var instance: Tealium

    fun init(application: Application) {
        val config = TealiumConfig(application,
                "tealiummobile",
                "location",
                Environment.DEV,
                modules = mutableSetOf(Modules.Lifecycle, Modules.VisitorService, Modules.HostedDataLayer),
                dispatchers = mutableSetOf(Dispatchers.Collect, Dispatchers.TagManagement)
        ).apply {
            collectors.add(Collectors.Location)
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
