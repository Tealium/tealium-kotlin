package com.tealium.inapppurchase

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.tealium.core.TealiumContext
import com.tealium.dispatcher.TealiumEvent
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InAppPurchaseAutoTrackingTests {
    @RelaxedMockK
    lateinit var mockContext: TealiumContext

    @RelaxedMockK
    lateinit var mockPurchaseListener: PurchasesUpdatedListener

    @RelaxedMockK
    lateinit var mockkBillingClient: BillingClient

    @RelaxedMockK
    lateinit var mockPurchase: Purchase

    lateinit var purchaseTracker: InAppPurchaseAutoTracker

    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(mainThreadSurrogate)

        every { mockPurchase.orderId } returns "order12345"
        every { mockPurchase.purchaseTime } returns 0L
        every { mockPurchase.quantity } returns 3
        every { mockPurchase.skus } returns arrayListOf("sku1", "sku2")
        every { mockPurchase.isAutoRenewing } returns false

        purchaseTracker = InAppPurchaseAutoTracker(mockContext, mockPurchaseListener, mockkBillingClient)
    }

    @Test
    fun trackInAppPurchase_ValidTrack() {
        purchaseTracker.trackInAppPurchase(mockPurchase)

        verify {
            mockContext.track(match {
                it is TealiumEvent
                        && it.eventName == "in_app_purchase"
                        && it.payload()["purchase_order_id"] == "order12345"
                        && it.payload()["purchase_timestamp"] == 0L
                        && it.payload()["purchase_date"] == "1970-01-01T00:00:00Z"
                        && it.payload()["purchase_quantity"] == 3
                        && it.payload()["purchase_skus"] == arrayListOf("sku1", "sku2")
                        && it.payload()["purchase_is_auto_renewing"] == false
            })
        }
    }

    @Test
    fun trackInAppPurchase_ValidTrackWithData() {
        purchaseTracker.trackInAppPurchase(mockPurchase, mapOf("key1" to "value1", "key2" to "value2"))

        verify {
            mockContext.track(match {
                it is TealiumEvent
                        && it.eventName == "in_app_purchase"
                        && it.payload()["purchase_order_id"] == "order12345"
                        && it.payload()["purchase_timestamp"] == 0L
                        && it.payload()["purchase_date"] == "1970-01-01T00:00:00Z"
                        && it.payload()["purchase_quantity"] == 3
                        && it.payload()["purchase_skus"] == arrayListOf("sku1", "sku2")
                        && it.payload()["purchase_is_auto_renewing"] == false
                        && it.payload()["key1"] == "value1"
                        && it.payload()["key2"] == "value2"
            })
        }
    }
}