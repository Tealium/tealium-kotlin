package com.tealium.adidentifier

import android.app.Application
import android.content.Context
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.appset.AppSetIdClient
import com.google.android.gms.appset.AppSetIdInfo
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.tasks.Task
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.persistence.DataLayer
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AdIdentifierTests {

    @MockK
    lateinit var mockApplication: Application

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

    private lateinit var adidProvider: (Context) -> AdvertisingIdClient.Info
    private lateinit var appSetClientProvider: (Context) -> AppSetIdClient

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { tealiumContext.config } returns config
        every { tealiumContext.config.application } returns mockApplication
        every { tealiumContext.dataLayer } returns dataLayer

        every { mockApplication.packageName } returns "testPackage"

        adidProvider = { adInfo }
        appSetClientProvider = { appSetClient }

        every { adInfo.id } returns "ad_id"
        every { adInfo.isLimitAdTrackingEnabled } returns false

        val mockAdInfoIdTask = mockk<Task<AppSetIdInfo>>()
        every { mockAdInfoIdTask.isSuccessful } returns true
        every { mockAdInfoIdTask.result } returns appSetIdInfo

        val slot = slot<com.google.android.gms.tasks.OnSuccessListener<AppSetIdInfo>>()
        every { mockAdInfoIdTask.addOnSuccessListener(capture(slot)) } answers {
            slot.captured.onSuccess(appSetIdInfo)
            mockAdInfoIdTask
        }

        every { appSetClient.appSetIdInfo } returns mockAdInfoIdTask
        every { appSetIdInfo.id } returns "app_set_id"
        every { appSetIdInfo.scope } returns 1
    }

    @Test
    fun fetchAdInfo_AddsToDataLayer_WhenAdInfoAvailable() {
        AdIdentifier(tealiumContext, adidProvider, appSetClientProvider)

        verify(timeout = 100) {
            dataLayer.putString("google_adid", "ad_id", any())
            dataLayer.putBoolean("google_limit_ad_tracking", false, any())
        }
    }

    @Test
    fun fetchAdInfo_DoesNotAddToDataLayer_WhenAdInfoUnavailable() {
        every { adInfo.id } returns null
        AdIdentifier(tealiumContext, adidProvider, appSetClientProvider)

        verify(timeout = 100) {
            dataLayer wasNot Called
        }
    }

    @Test
    fun fetchAdInfo_DoesNotAddToDataLayer_WhenGoogleApiUnavailable() {
        AdIdentifier(tealiumContext, {
            throw GooglePlayServicesNotAvailableException(0)
        }, appSetClientProvider)

        verify(timeout = 100) {
            dataLayer wasNot Called
        }
    }

    @Test
    fun removeAdInfo_SuccessfulRemovalFromDataLayer() {
        val adIdentifier = AdIdentifier(tealiumContext, adidProvider, appSetClientProvider)
        adIdentifier.removeAdInfo()

        verify(timeout = 100) {
            dataLayer.remove("google_adid")
            dataLayer.remove("google_limit_ad_tracking")
        }
    }

    @Test
    fun fetchAppSetIdInfo() {
        AdIdentifier(tealiumContext, adidProvider, appSetClientProvider)

        verify(timeout = 100) {
            dataLayer.putInt("google_app_set_scope", 1, any())
            dataLayer.putString("google_app_set_id", "app_set_id", any())
        }
    }

    @Test
    fun removeAppSetIdInfo() {
        val adIdentifier = AdIdentifier(tealiumContext, adidProvider, appSetClientProvider)
        adIdentifier.removeAppSetIdInfo()

        verify(timeout = 100) {
            dataLayer.remove("google_app_set_id")
            dataLayer.remove("google_app_set_scope")
        }
    }
}