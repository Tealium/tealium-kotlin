package com.tealium.installreferrer

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
import com.tealium.core.persistence.Expiry
import com.tealium.installreferrer.InstallReferrer.ReferrerDetailsUpdatedListener

class InstallReferrer(
    private val context: TealiumContext,
    private val referrerClient: InstallReferrerClient =
        InstallReferrerClient.newBuilder(context.config.application)
            .build()
) : Module {

    override val name: String
        get() = "InstallReferrer"
    override var enabled: Boolean = true

    init {
        context.events.subscribe(ReferrerDetailsUpdatedListener { referrerDetails ->
            save(referrerDetails)
        })
        referrerClient.startConnection(object : InstallReferrerStateListener {

            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        Logger.dev(BuildConfig.TAG, "Connection established")
                        try {
                            val referrerDetails = referrerClient.installReferrer
                            context.events.send(ReferrerDetailsUpdatedMessenger(referrerDetails))
                        } catch (e: RemoteException) {
                            Logger.prod(BuildConfig.TAG, "InstallReferrer Remote Exception")
                        } finally {
                            referrerClient.endConnection()
                        }
                    }

                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                        Logger.prod(
                            BuildConfig.TAG,
                            "API not available on the current Play Store app."
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

    var referrer: String? = null
        set(value) {
            field = value
            value?.let {
                context.dataLayer.putString(
                    InstallReferrerConstants.KEY_INSTALL_REFERRER,
                    it,
                    Expiry.FOREVER
                )
            }
        }

    var referrerBegin: Long? = null
        set(value) {
            field = value
            value?.let {
                context.dataLayer.putLong(
                    InstallReferrerConstants.KEY_INSTALL_REFERRER_BEGIN_TIMESTAMP,
                    it,
                    Expiry.FOREVER
                )
            }
        }

    var referrerClick: Long? = null
        set(value) {
            field = value
            value?.let {
                context.dataLayer.putLong(
                    InstallReferrerConstants.KEY_INSTALL_REFERRER_CLICK_TIMESTAMP,
                    it,
                    Expiry.FOREVER
                )
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

    private fun interface ReferrerDetailsUpdatedListener : ExternalListener {
        fun onReferrerDetailsUpdated(referrerDetails: ReferrerDetails)
    }

    private class ReferrerDetailsUpdatedMessenger(private val referrerDetails: ReferrerDetails) :
        ExternalMessenger<ReferrerDetailsUpdatedListener>(ReferrerDetailsUpdatedListener::class) {
        override fun deliver(listener: ReferrerDetailsUpdatedListener) {
            listener.onReferrerDetailsUpdated(referrerDetails)
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