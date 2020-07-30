package com.tealium.installreferrer

import android.os.RemoteException

import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.tealium.core.*
import com.tealium.core.persistence.Expiry

class InstallReferrer(private val context: TealiumContext) : Module {

    override val name: String
        get() = "INSTALL_REFERRER_COLLECTOR"
    override var enabled: Boolean = true

    private var referrerClient = InstallReferrerClient.newBuilder(context.config.application).build()

    init {
        referrerClient.startConnection(object : InstallReferrerStateListener {

            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        Logger.dev(BuildConfig.TAG, "Connection established")
                        try {
                            save(referrerClient.getInstallReferrer())
                        } catch (e: RemoteException) {
                            Logger.prod(BuildConfig.TAG, "InstallReferrer Remote Exception")
                        } finally {
                            referrerClient.endConnection()
                        }
                    }
                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                        Logger.prod(BuildConfig.TAG, "API not available on the current Play Store app.")
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
                context.dataLayer.putString(InstallReferrerConstants.KEY_INSTALL_REFERRER,
                        it,
                        Expiry.FOREVER)
            }
        }

    var referrerBegin: Long? = null
        set(value) {
            field = value
            value?.let {
                context.dataLayer.putLong(InstallReferrerConstants.KEY_INSTALL_REFERRER_BEGIN_TIMESTAMP,
                        it,
                        Expiry.FOREVER)
            }
        }

    var referrerClick: Long? = null
        set(value) {
            field = value
            value?.let {
                context.dataLayer.putLong(InstallReferrerConstants.KEY_INSTALL_REFERRER_CLICK_TIMESTAMP,
                        it,
                        Expiry.FOREVER)
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

    companion object : ModuleFactory {
        override fun create(context: TealiumContext): Module {
            return InstallReferrer(context)
        }
    }
}

val Modules.InstallReferrer: ModuleFactory
    get() = com.tealium.installreferrer.InstallReferrer