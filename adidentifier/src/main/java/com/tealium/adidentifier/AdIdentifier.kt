package com.tealium.adidentifier

import android.content.Context
import android.os.Build
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.appset.AppSet
import com.tealium.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception

/**
 * Module to retrieve Advertising ID from Google Play and save to Data Layer
 */
class AdIdentifier(private val tealiumContext: TealiumContext) : Module {

    override val name: String
        get() = MODULE_NAME
    override var enabled: Boolean = true

    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Advertising ID from Google Play
     */
    private var adid: String? = null
        set(value) {
            field = value
            value?.let {
                tealiumContext.dataLayer.putString(KEY_GOOGLE_ADID, it)
            } ?: run {
                tealiumContext.dataLayer.remove(KEY_GOOGLE_ADID)
            }
        }

    /**
     * Limit Ad Tracking flag from Google Play
     */
    private var isLimitAdTrackingEnabled: Boolean? = null
        set(value) {
            field = value
            value?.let {
                tealiumContext.dataLayer.putBoolean(KEY_GOOGLE_AD_TRACKING, it)
            } ?: run {
                tealiumContext.dataLayer.remove(KEY_GOOGLE_AD_TRACKING)
            }
        }

    /**
     * App Set ID
     */
    private var appSetId: String? = null
        set(value) {
            field = value
            value?.let {
                tealiumContext.dataLayer.putString(KEY_GOOGLE_APP_SET_ID, it)
            } ?: run {
                tealiumContext.dataLayer.remove(KEY_GOOGLE_APP_SET_ID)
            }
        }

    /**
     * App Set Scope
     */
    private var appSetScope: Int? = null
        set(value) {
            field = value
            value?.let {
                tealiumContext.dataLayer.putInt(KEY_GOOGLE_APP_SET_SCOPE, it)
            } ?: run {
                tealiumContext.dataLayer.remove(KEY_GOOGLE_APP_SET_SCOPE)
            }
        }

    init {
        scope.launch {
            fetchAdInfo(tealiumContext.config.application)
            fetchAppSetInfo(tealiumContext.config.application)
        }
    }

    /**
     * Fetches Advertising Info from AdvertisingIdClient
     */
    private fun fetchAdInfo(context: Context) {
        try {
            val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
            if (adInfo.id != null) {
                adid = adInfo.id
            }
            isLimitAdTrackingEnabled = adInfo.isLimitAdTrackingEnabled
        } catch (ex: Exception) {
            Logger.dev(BuildConfig.TAG, "Unable to retrieve AdvertisingIdInfo. See: ${ex.message}")
        }
    }

    private fun fetchAppSetInfo(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val client = AppSet.getClient(context)
            val task = client.appSetIdInfo

            task.addOnSuccessListener {
                appSetId = it.id
                appSetScope = it.scope
            }
        }
    }

    /**
     * Clears values and removes from data layer
     */
    fun removeAdInfo() {
        adid = null
        isLimitAdTrackingEnabled = null
    }

    /**
     * Clears values and removes from data layer
     */
    fun removeAppSetIdInfo() {
        appSetId = null
        appSetScope = null
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
    }
}

val Tealium.adIdentifier: AdIdentifier?
    get() = modules.getModule(AdIdentifier::class.java)

val Modules.AdIdentifier: ModuleFactory
    get() = com.tealium.adidentifier.AdIdentifier