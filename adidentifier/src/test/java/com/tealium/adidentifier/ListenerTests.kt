package com.tealium.adidentifier

import com.tealium.adidentifier.internal.AdvertisingInfoUpdatedListener
import com.tealium.core.persistence.DataLayer
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ListenerTests {

    @RelaxedMockK
    private lateinit var dataLayer: DataLayer

    private lateinit var advertisingInfoUpdatedListener: AdvertisingInfoUpdatedListener

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        advertisingInfoUpdatedListener = AdIdentifier.createUpdateListener(dataLayer)
    }

    @Test
    fun onAdIdInfoUpdated_RemovesFromDataLayer_WhenNull() {
        advertisingInfoUpdatedListener.onAdIdInfoUpdated(null, null)

        verify {
            dataLayer.remove(AdIdentifier.KEY_GOOGLE_ADID)
            dataLayer.remove(AdIdentifier.KEY_GOOGLE_AD_TRACKING)
        }
    }

    @Test
    fun onAdIdInfoUpdated_AddsToDataLayer_WhenNotNull() {
        advertisingInfoUpdatedListener.onAdIdInfoUpdated("ad_id", false)

        verify {
            dataLayer.putString(AdIdentifier.KEY_GOOGLE_ADID, "ad_id", any())
            dataLayer.putBoolean(AdIdentifier.KEY_GOOGLE_AD_TRACKING, false, any())
        }
    }

    @Test
    fun onAppSetInfoUpdated_RemovesFromDataLayer_WhenNull() {
        advertisingInfoUpdatedListener.onAppSetInfoUpdated(null, null)

        verify {
            dataLayer.remove(AdIdentifier.KEY_GOOGLE_APP_SET_ID)
            dataLayer.remove(AdIdentifier.KEY_GOOGLE_APP_SET_SCOPE)
        }
    }

    @Test
    fun onAppSetInfoUpdated_AddsToDataLayer_WhenNotNull() {
        advertisingInfoUpdatedListener.onAppSetInfoUpdated("app_set_id", 1)

        verify {
            dataLayer.putString(AdIdentifier.KEY_GOOGLE_APP_SET_ID, "app_set_id", any())
            dataLayer.putInt(AdIdentifier.KEY_GOOGLE_APP_SET_SCOPE, 1, any())
        }
    }
}