package com.tealium.adidentifier

import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.appset.AppSet
import com.google.android.gms.appset.AppSetIdClient
import com.google.android.gms.appset.AppSetIdInfo
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.persistence.DataLayer
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Before
import org.junit.Test

class AdIdentifierTests {

    @MockK
    lateinit var tealiumContext: TealiumContext

    @RelaxedMockK
    lateinit var config: TealiumConfig

    @RelaxedMockK
    lateinit var dataLayer: DataLayer

    @MockK
    lateinit var adInfo: AdvertisingIdClient.Info

    @MockK
    lateinit var appSetClient: AppSetIdClient

    @MockK
    lateinit var appSetIdInfo: AppSetIdInfo


    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { tealiumContext.config } returns config
        every { tealiumContext.dataLayer } returns dataLayer

        mockkStatic(AdvertisingIdClient::class)

        every { AdvertisingIdClient.getAdvertisingIdInfo(any()) } returns adInfo
        every { adInfo.id } returns "ad_id"
        every { adInfo.isLimitAdTrackingEnabled } returns false

        every { appSetIdInfo.scope } returns 1
        every { appSetIdInfo.id } returns "app_set_id"

        mockkStatic(AppSet::class)
        every { AppSet.getClient(any()) } returns appSetClient
        every { appSetClient.appSetIdInfo } returns AppSetIdInfoTask(appSetIdInfo)
    }

    @Test
    fun fetchAdInfo_AddsToDataLayer_WhenAdInfoAvailable() {
        AdIdentifier.create(tealiumContext) as AdIdentifier

        verify(timeout = 100) {
            dataLayer.putString("google_adid", "ad_id", any())
            dataLayer.putBoolean("google_limit_ad_tracking", false, any())
        }
    }

    @Test
    fun fetchAdInfo_DoesNotAddToDataLayer_WhenAdInfoUnavailable() {
        every { AdvertisingIdClient.getAdvertisingIdInfo(any()) } returns null
        AdIdentifier.create(tealiumContext) as AdIdentifier

        verify(timeout = 100) {
            dataLayer wasNot Called
        }
    }

    @Test
    fun fetchAdInfo_DoesNotAddToDataLayer_WhenGoogleApiUnavailable() {
        AdIdentifier.create(tealiumContext) as AdIdentifier

        verify(timeout = 100) {
            dataLayer wasNot Called
        }
    }

    @Test
    fun removeAdInfo_SuccessfulRemovalFromDataLayer() {
        val adIdentifier = AdIdentifier.create(tealiumContext) as AdIdentifier
        adIdentifier.removeAdInfo()

        verify {
            dataLayer.remove("google_adid")
            dataLayer.remove("google_limit_ad_tracking")
        }
    }

    @Test
    fun fetchAppSetIdInfo() {
//        mockkStatic(AppSet::class)
//        every { AppSet.getClient(any()) } returns appSetClient
//        every { appSetClient.appSetIdInfo } returns AppSetIdInfoTask(appSetIdInfo)

        AdIdentifier.create(tealiumContext) as AdIdentifier

//        verify {
//            dataLayer.putInt("google_app_set_scope", 1, any())
//            dataLayer.putString("google_app_set_id", "app_set_id", any())
//        }
    }

    @Test
    fun removeAppSetIdInfo() {
        val adIdentifier = AdIdentifier.create(tealiumContext) as AdIdentifier
        adIdentifier.removeAppSetIdInfo()

        verify {
            dataLayer.remove("google_app_set_id")
            dataLayer.remove("google_app_set_scope")
        }
    }
}