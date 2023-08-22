package com.tealium.adidentifier

import android.content.Context
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.appset.AppSet
import com.google.android.gms.appset.AppSetIdClient
import com.tealium.adidentifier.internal.AdIdInfoUpdatedMessenger
import com.tealium.adidentifier.internal.AdvertisingInfoUpdatedListener
import com.tealium.adidentifier.internal.AppSetInfoUpdatedMessenger
import com.tealium.core.*
import com.tealium.core.persistence.DataLayer
import java.lang.Exception

/**
 * Module to retrieve Advertising ID from Google Play and save to Data Layer
 */
class AdIdentifier(
    private val tealiumContext: TealiumContext,
    private val adidInfoProvider: (Context) -> AdvertisingIdClient.Info = AdvertisingIdClient::getAdvertisingIdInfo,
    private val appSetClientProvider: (Context) -> AppSetIdClient = AppSet::getClient
) : Module {

    override val name: String
        get() = MODULE_NAME
    override var enabled: Boolean = true

    init {
        val updateListener = createUpdateListener(tealiumContext.dataLayer)
        tealiumContext.events.subscribe(updateListener)

        fetchAdInfo(tealiumContext.config.application)
        fetchAppSetInfo(tealiumContext.config.application)
    }

    /**
     * Fetches Advertising Info from AdvertisingIdClient
     */
    private fun fetchAdInfo(context: Context) {
        try {
            val adInfo = adidInfoProvider.invoke(context)

            saveAdIdInfoToDataLayer(adInfo.id, adInfo.isLimitAdTrackingEnabled)
        } catch (ex: Exception) {
            Logger.dev(BuildConfig.TAG, "Unable to retrieve AdvertisingIdInfo. See: ${ex.message}")
        }
    }

    private fun fetchAppSetInfo(context: Context) {
        val client = appSetClientProvider.invoke(context)
        val task = client.appSetIdInfo

        task.addOnSuccessListener {
            // Tasks get executed by default on Main thread.
            saveAppSetInfoToDataLayer(it.id, it.scope)
        }
    }

    private fun saveAdIdInfoToDataLayer(
        adId: String?,
        isLimitAdTrackingEnabled: Boolean?
    ) {
        tealiumContext.events.send(AdIdInfoUpdatedMessenger(adId, isLimitAdTrackingEnabled))
    }

    private fun saveAppSetInfoToDataLayer(appSetId: String?, appSetScope: Int?) {
        tealiumContext.events.send(AppSetInfoUpdatedMessenger(appSetId, appSetScope))
    }

    /**
     * Clears values and removes from data layer
     */
    fun removeAdInfo() {
        saveAdIdInfoToDataLayer(null, null)
    }

    /**
     * Clears values and removes from data layer
     */
    fun removeAppSetIdInfo() {
        saveAppSetInfoToDataLayer(null, null)
    }

    companion object : ModuleFactory {
        const val MODULE_NAME = "AdIdentifier"
        const val MODULE_VERSION = BuildConfig.LIBRARY_VERSION
        const val KEY_GOOGLE_ADID = "google_adid"
        const val KEY_GOOGLE_AD_TRACKING = "google_limit_ad_tracking"
        const val KEY_GOOGLE_APP_SET_ID = "google_app_set_id"
        const val KEY_GOOGLE_APP_SET_SCOPE = "google_app_set_scope"

        override fun create(context: TealiumContext): Module {
            return AdIdentifier(context)
        }

        internal fun createUpdateListener(dataLayer: DataLayer): AdvertisingInfoUpdatedListener {
            return object : AdvertisingInfoUpdatedListener {

                override fun onAdIdInfoUpdated(adId: String?, isLimitAdTrackingEnabled: Boolean?) {
                    if (adId != null) {
                        dataLayer.putString(KEY_GOOGLE_ADID, adId)
                    } else {
                        dataLayer.remove(KEY_GOOGLE_ADID)
                    }

                    if (isLimitAdTrackingEnabled != null) {
                        dataLayer.putBoolean(KEY_GOOGLE_AD_TRACKING, isLimitAdTrackingEnabled)
                    } else {
                        dataLayer.remove(KEY_GOOGLE_AD_TRACKING)
                    }
                }

                override fun onAppSetInfoUpdated(appSetId: String?, appSetScope: Int?) {
                    if (appSetId != null) {
                        dataLayer.putString(KEY_GOOGLE_APP_SET_ID, appSetId)
                    } else {
                        dataLayer.remove(KEY_GOOGLE_APP_SET_ID)
                    }

                    if (appSetScope != null) {
                        dataLayer.putInt(KEY_GOOGLE_APP_SET_SCOPE, appSetScope)
                    } else {
                        dataLayer.remove(KEY_GOOGLE_APP_SET_SCOPE)
                    }
                }
            }
        }
    }
}

val Tealium.adIdentifier: AdIdentifier?
    get() = modules.getModule(AdIdentifier::class.java)

val Modules.AdIdentifier: ModuleFactory
    get() = com.tealium.adidentifier.AdIdentifier