package com.tealium.core

import android.app.Application
import com.tealium.core.collection.*
import com.tealium.core.settings.LibrarySettings
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

    /**
     * A set of validators where any custom [DispatchValidator]s can be added. These will be merged
     * in with the built in validators when initializing the library.
     */
    val validators: MutableSet<DispatchValidator> = mutableSetOf()

    private val pathName = "${application.filesDir}${File.separatorChar}tealium${File.separatorChar}${accountName}${File.separatorChar}${profileName}${File.separatorChar}${environment.environment}"
    val tealiumDirectory: File = File(pathName)

    /**
     * Map of key-value pairs supporting override options that do not have a direct property on the
     * [TealiumConfig] object.
     */
    val options = mutableMapOf<String, Any>()

    /**
     * Gets and sets the initial LibrarySettings for the library. Useful defaults have already been
     * set on the [LibrarySettings] default constructor, but the default settings used by the
     * library can be set here.
     */
    var overrideDefaultLibrarySettings: LibrarySettings? = null

    /**
     * Sets whether or not to fetch publish settings from a remote host.
     */
    var useRemoteLibrarySettings: Boolean = false

    /**
     * Sets the remote URL to use when requesting updated remote publish settings.
     */
    var overrideLibrarySettingsUrl: String? = null

    init {
        tealiumDirectory.mkdirs()
    }
}