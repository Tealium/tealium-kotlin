package com.tealium.mobile

import android.app.Application
import android.util.Log

// KOTLIN LIB: Comment out from here
//import com.tealium.collectdispatcher.Collect
//import com.tealium.core.*
//import com.tealium.core.consent.ConsentPolicy
//import com.tealium.core.consent.consentManagerPolicy
//import com.tealium.core.validation.DispatchValidator
//import com.tealium.dispatcher.Dispatch
//import com.tealium.dispatcher.TealiumView
//import com.tealium.lifecycle.Lifecycle
//import com.tealium.location.Location
//import com.tealium.tagmanagementdispatcher.TagManagement
//import com.tealium.visitorservice.VisitorProfile
//import com.tealium.visitorservice.VisitorService
//import com.tealium.visitorservice.VisitorServiceDelegate
//import com.tealium.visitorservice.visitorService
//
//object TealiumHelper {
//    lateinit var instance: Tealium
//    lateinit var application: Application
//
//    fun init(application: Application) {
//        val config = TealiumConfig(application,
//                "tealiummobile",
//                "location",
//                Environment.DEV,
//                modules = mutableSetOf(Lifecycle, VisitorService),
//                dispatchers = mutableSetOf(Dispatchers.Collect, Dispatchers.TagManagement)
//        ).apply {
//            collectors.add(Collectors.Location)
//            useRemoteLibrarySettings = true
//        }
//
//        config.consentManagerPolicy = ConsentPolicy.CCPA
//        instance = Tealium("instance_1", config) {
//            consentManager.enabled = true
//            visitorService?.delegate = object : VisitorServiceDelegate {
//                override fun didUpdate(visitorProfile: VisitorProfile) {
//                    Logger.dev("--", "did update vp with $visitorProfile")
//                }
//            }
//        }
//    }
//
//    fun trackView(name: String, data: HashMap<String, Any>?) {
//        val viewDispatch = TealiumView(name, data)
//        instance.track(viewDispatch)
//    }
//
//    val customValidator: DispatchValidator by lazy {
//        object : DispatchValidator {
//            override fun shouldQueue(dispatch: Dispatch?): Boolean {
//                Logger.dev(BuildConfig.TAG, "shouldQueue: CustomValidator")
//                return false
//            }
//
//            override fun shouldDrop(dispatch: Dispatch): Boolean {
//                Logger.dev(BuildConfig.TAG, "shouldDrop: CustomValidator")
//                return false
//            }
//
//            override val name: String = "my validator"
//            override var enabled: Boolean = true
//        }
//    }
//
//}

// KOTLIN LIB COMMENT END


// UNCOMMENT FOR JAVA LIB

import com.tealium.crashreporter.CrashReporter
import com.tealium.library.Tealium
import com.tealium.lifecycle.LifeCycle

object TealiumHelper {
    lateinit var instance: Tealium
    lateinit var application: Application
    lateinit var config: Tealium.Config

    val persistentDataInstanceHash:Int get() {
            return (config.accountName + '.' +
                    config.profileName + '.' +
                    config.environmentName).hashCode()
    }

    val lifecycleInstanceHash:Int get() { // same for crash, consent,
        return (config.accountName +
                config.profileName +
                config.environmentName).hashCode()
    }

    fun init(application: Application) {
        config = Tealium.Config.create(application, "tealiummobile", "location", "dev")
        config.enableConsentManager("main")
        LifeCycle.setupInstance("main", config, true);
        this.application = application
        instance = Tealium.createInstance("main", config)
        CrashReporter.initialize("main", config, true)
        instance.consentManager.userConsentStatus = "consented"
        instance.trackEvent("hello", null)
        instance.dataSources.persistentDataSources.edit().putBoolean("hello", true).commit()
        getSharedPrefs("lifecycle")
        getSharedPrefs("crash")
        getSharedPrefs("userconsentpreferences")
        getSharedPrefsPersistentData()
    }

    fun trackView(name: String, data: HashMap<String, Any>?) {
        instance.trackView(name, data)
    }


    fun getSharedPrefs(name: String) {
        application?.let { application ->
            val key = "tealium.$name.${java.lang.Integer.toHexString(lifecycleInstanceHash)}"
            val prefs = application.getSharedPreferences(key, 0)
            Log.d("Prefs", prefs.all.toString())

        }
    }

    fun getSharedPrefsPersistentData(name: String = "datasources") {
        application?.let { application ->
            val key = "tealium.$name.${java.lang.Integer.toHexString(persistentDataInstanceHash)}"
            val prefs = application.getSharedPreferences(key, 0)
            Log.d("Prefs", prefs.all.toString())

        }
    }

}

// JAVA LIB COMMENT END