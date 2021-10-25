package com.tealium.adidentifier

import com.google.android.gms.ads.identifier.AdvertisingIdClient
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

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { tealiumContext.config } returns config
        every { tealiumContext.dataLayer } returns dataLayer

        mockkStatic(AdvertisingIdClient::class)

        every { AdvertisingIdClient.getAdvertisingIdInfo(any()) } returns adInfo

        every { adInfo.id } returns "ad_id"
        every { adInfo.isLimitAdTrackingEnabled } returns false
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
}