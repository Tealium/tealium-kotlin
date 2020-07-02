package com.tealium.core

import android.app.Application
import com.tealium.core.collection.*
import com.tealium.core.validation.DispatchValidator
import java.io.File

enum class Environment(val environment: String) {
    DEV("dev"),
    QA("qa"),
    PROD("prod")
}

const val TEALIUM_ACCOUNT = "tealium_account"
const val TEALIUM_PROFILE = "tealium_profile"

object Collectors {
    // for extension methods to register CollectorFactory types.
    @JvmStatic
    val core = mutableSetOf(AppCollector,
        ConnectivityCollector,
        DeviceCollector,
        TimeCollector)
}

object Dispatchers {
    // for extension methods to register Dispatcher
}

object Modules {
    // for extension methods to register Modules
}

class TealiumConfig @JvmOverloads constructor(val application: Application,
                    val accountName: String,
                    val profileName: String,
                    val environment: Environment,
                    var dataSourceId: String? = null,
                    val collectors: MutableSet<CollectorFactory> = Collectors.core,
                    val dispatchers: MutableSet<DispatcherFactory> = mutableSetOf(),
                    val modules: MutableSet<ModuleFactory> = mutableSetOf()) {

    val validators: MutableSet<DispatchValidator> = mutableSetOf()
    private val pathName = "${application.filesDir}${File.separatorChar}tealium${File.separatorChar}${accountName}${File.separatorChar}${profileName}${File.separatorChar}${environment.environment}"
    val tealiumDirectory: File = File(pathName)
    val options = mutableMapOf<String, Any>()

    init {
        tealiumDirectory.mkdirs()
    }
}

const val LIBRARY_SETTINGS_OVERRIDE_URL = "override_library_settings_url"

var TealiumConfig.overrideLibrarySettingsUrl: String?
    get() = options[LIBRARY_SETTINGS_OVERRIDE_URL] as? String
    set(value) {
        value?.let {
            options[LIBRARY_SETTINGS_OVERRIDE_URL] = it
        }
    }