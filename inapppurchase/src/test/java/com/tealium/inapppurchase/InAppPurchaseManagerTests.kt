package com.tealium.inapppurchase

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.tealium.core.TealiumContext
import com.tealium.dispatcher.TealiumEvent
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InAppPurchaseManagerTests {
    @RelaxedMockK
    lateinit var mockContext: TealiumContext

    @RelaxedMockK
    lateinit var mockContext2: TealiumContext

    @RelaxedMockK
    lateinit var mockkBillingClient: BillingClient

    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(mainThreadSurrogate)

        mockkStatic(BillingClient::class)
        every { BillingClient.newBuilder(any()).setListener(any()).enablePendingPurchases().build() } returns mockkBillingClient
        every { mockkBillingClient.startConnection(any()) } just Runs
    }

    @Test
    fun purchaseManager_createMultipleAndReturnSameInstance() {
        val tracker1 = InAppPurchaseManager.create(mockContext)
        val tracker2 = InAppPurchaseManager.create(mockContext2)

        assertSame(tracker1, tracker2)
    }

    @Test
    fun purchaseManager_manualTrackPurchase() {
        val manager = InAppPurchaseManager(mockContext)

        val purchase = mockk<Purchase>()
        every { purchase.orderId } returns "order12345"
        every { purchase.purchaseTime } returns 0L
        every { purchase.quantity } returns 3
        every { purchase.skus } returns arrayListOf("sku1", "sku2")
        every { purchase.isAutoRenewing } returns false
        every { purchase.purchaseState } returns 0

        manager.trackInAppPurchase(purchase)

        verify {
            mockContext.track(match {
                it is TealiumEvent
                        && it.eventName == "in_app_purchase"
                        && it.payload()["purchase_order_id"] == "order12345"
                        && it.payload()["purchase_timestamp"] == 0L
                        && it.payload()["purchase_quantity"] == 3
                        && it.payload()["purchase_skus"] == arrayListOf("sku1", "sku2")
                        && it.payload()["purchase_is_auto_renewing"] == false
            })
        }
    }

    @Test
    fun purchaseManager_manualTrackPurchaseWithData() {
        val manager = InAppPurchaseManager(mockContext)

        val purchase2 = mockk<Purchase>()
        every { purchase2.orderId } returns "order12345"
        every { purchase2.purchaseTime } returns 0L
        every { purchase2.quantity } returns 3
        every { purchase2.skus } returns arrayListOf("sku1", "sku2")
        every { purchase2.isAutoRenewing } returns false
        every { purchase2.purchaseState } returns 0

        manager.trackInAppPurchase(purchase2, mapOf("key1" to "value1", "key2" to "value2"))

        verify {
            mockContext.track(match {
                it is TealiumEvent
                        && it.eventName == "in_app_purchase"
                        && it.payload()["key1"] == "value1"
                        && it.payload()["key2"] == "value2"
            })
        }
    }
}