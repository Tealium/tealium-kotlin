package com.tealium.installreferrer

import InstallReferrerConstants
import android.app.Application
import android.os.RemoteException
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.tealium.core.Logger
import com.tealium.core.Module
import com.tealium.core.ModuleFactory
import com.tealium.core.Modules
import com.tealium.core.TealiumContext
import com.tealium.core.messaging.ExternalListener
import com.tealium.core.messaging.ExternalMessenger
import com.tealium.core.messaging.MessengerService
import com.tealium.core.persistence.DataLayer
import com.tealium.core.persistence.Expiry

class InstallReferrer(
    application: Application,
    private val dataLayer: DataLayer,
    private val events: MessengerService,
    private val referrerClient: InstallReferrerClient = InstallReferrerClient.newBuilder(application)
        .build()
) : Module {

    constructor(context: TealiumContext) : this(
        context.config.application, context.dataLayer, context.events
    )

    override val name: String
        get() = "InstallReferrer"
    override var enabled: Boolean = true

    private fun fetchInstallReferrer() {
        events.subscribe(ReferrerClientConnectedListener(::onClientConnected))
        referrerClient.startConnection(object : InstallReferrerStateListener {

            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        Logger.dev(BuildConfig.TAG, "Connection established")
                        // take the fetching of referrer details, and unbinding off of the main thread
                        events.send(ReferrerClientConnectedMessenger())
                    }

                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                        Logger.prod(
                            BuildConfig.TAG, "API not available on the current Play Store app."
                        )
                    }

                    InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                        Logger.prod(BuildConfig.TAG, "Connection couldn't be established.")
                    }
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                Logger.qa(BuildConfig.TAG, "Service Disconnected")
            }
        })
    }

    var referrer: String? = dataLayer.getString(InstallReferrerConstants.KEY_INSTALL_REFERRER)
        set(value) {
            field = value
            value?.let {
                dataLayer.putString(
                    InstallReferrerConstants.KEY_INSTALL_REFERRER, it, Expiry.FOREVER
                )
            }
        }

    var referrerBegin: Long? =
        dataLayer.getLong(InstallReferrerConstants.KEY_INSTALL_REFERRER_BEGIN_TIMESTAMP)
        set(value) {
            field = value
            value?.let {
                dataLayer.putLong(
                    InstallReferrerConstants.KEY_INSTALL_REFERRER_BEGIN_TIMESTAMP,
                    it,
                    Expiry.FOREVER
                )
            }
        }

    var referrerClick: Long? =
        dataLayer.getLong(InstallReferrerConstants.KEY_INSTALL_REFERRER_CLICK_TIMESTAMP)
        set(value) {
            field = value
            value?.let {
                dataLayer.putLong(
                    InstallReferrerConstants.KEY_INSTALL_REFERRER_CLICK_TIMESTAMP,
                    it,
                    Expiry.FOREVER
                )
            }
        }

    init {
        // Install Referrer is kept for 90 days, unmodified, unless uninstalled
        // We only need to fetch it if we don't have it.
        if (referrer.isNullOrEmpty()) {
            fetchInstallReferrer()
        }
    }

    internal fun onClientConnected() {
        try {
            val referrerDetails = referrerClient.installReferrer
            save(referrerDetails)
        } catch (_: RemoteException) {
            Logger.prod(BuildConfig.TAG, "InstallReferrer Remote Exception")
        } finally {
            referrerClient.endConnection()
        }
    }

    internal fun save(details: ReferrerDetails) {
        if (details.installReferrer.isEmpty()) {
            return
        }
        referrer = details.installReferrer
        referrerBegin = details.installBeginTimestampSeconds
        referrerClick = details.referrerClickTimestampSeconds
    }

    private fun interface ReferrerClientConnectedListener : ExternalListener {
        fun onReferrerClientConnected()
    }

    private class ReferrerClientConnectedMessenger() :
        ExternalMessenger<ReferrerClientConnectedListener>(ReferrerClientConnectedListener::class) {
        override fun deliver(listener: ReferrerClientConnectedListener) {
            listener.onReferrerClientConnected()
        }
    }

    companion object : ModuleFactory {
        const val MODULE_VERSION = BuildConfig.LIBRARY_VERSION

        override fun create(context: TealiumContext): Module {
            return InstallReferrer(context)
        }
    }
}

val Modules.InstallReferrer: ModuleFactory
    get() = com.tealium.installreferrer.InstallReferrer