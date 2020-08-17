package com.tealium.mobile

//import com.tealium.core.*
//import com.tealium.core.validation.DispatchValidator
//import com.tealium.dispatcher.Dispatch
import android.app.Application
import android.util.Log
import com.tealium.core.TealiumUtils
import com.tealium.library.Tealium
import com.tealium.lifecycle.LifeCycle


object TealiumHelper {
    lateinit var instance: Tealium
    lateinit var application: Application
    lateinit var config: Tealium.Config

    val instanceHash:Int get() {
            return (config.accountName + '.' +
                    config.profileName + '.' +
                    config.environmentName).hashCode()
    }

    val lifecycleInstanceHash:Int get() {
        return (config.accountName +
                config.profileName +
                config.environmentName).hashCode()
    }

    fun init(application: Application) {
        config = Tealium.Config.create(application, "tealiummobile", "location", "dev")
        LifeCycle.setupInstance("main", config, true);
//        config.enableConsentManager()
        this.application = application
        instance = Tealium.createInstance("main", config)
//        instance.consentManager.userConsentStatus = "consented"
        instance.trackEvent("hello", null)
        instance.dataSources.persistentDataSources.edit().putBoolean("hello", true).commit()
        getSharedPrefs()


//        val config = TealiumConfig(application,
//                "tealiummobile",
//                "location",
//                Environment.DEV,
//                modules = mutableSetOf(Modules.Lifecycle, Modules.VisitorService),
//                dispatchers = mutableSetOf(Dispatchers.Collect, Dispatchers.TagManagement)
//        ).apply {
//            collectors.add(Collectors.Location)
//            useRemoteLibrarySettings = true
//        }
//
//        instance = Tealium("instance_1", config) {
//            consentManager.enabled = true
//            visitorService?.delegate = object : VisitorServiceDelegate {
//                override fun didUpdate(visitorProfile: VisitorProfile) {
//                    Logger.dev("--", "did update vp with $visitorProfile")
//                }
//            }
//        }
    }

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

    fun getSharedPrefs() {
        application?.let {
            application ->
            val key = "tealium.lifecycle.${java.lang.Integer.toHexString(lifecycleInstanceHash)}"
            val prefs = application.getSharedPreferences(key, 0)
            Log.d("Prefs", prefs.all.toString())
        }
    }


}
