package com.tealium.core.validation

import com.tealium.core.messaging.LibrarySettingsUpdatedListener
import com.tealium.core.model.LibrarySettings
import com.tealium.core.network.Connectivity
import com.tealium.dispatcher.Dispatch

/**
 * The connectivitiy validator will queue requests based on the current Library Settings. If there
 * is no network connectivity, or the settings dictate that data should only be sent when a WiFi
 * connection is detected, then the Dispatch will be queued.
 */
class ConnectivityValidator(private val connectivityRetriever: Connectivity, private var librarySettings: LibrarySettings): DispatchValidator, LibrarySettingsUpdatedListener {

    override val name: String = "CONNECTIVITY_VALIDATOR"
    override var enabled: Boolean = true

    override fun shouldQueue(dispatch: Dispatch?): Boolean {
        return when(librarySettings.wifiOnly) {
            true -> {
                !(connectivityRetriever.isConnected() && connectivityRetriever.isConnectedWifi())
            }
            false -> {
                !connectivityRetriever.isConnected()
            }
        }
    }

    override fun shouldDrop(dispatch: Dispatch): Boolean {
        return false
    }

    override fun onLibrarySettingsUpdated(settings: LibrarySettings) {
        librarySettings = settings
    }
}