package com.tealium.inapppurchase

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.tealium.core.Logger
import com.tealium.core.Tealium

class PurchaseListener: PurchasesUpdatedListener {
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        Tealium.names().forEach { instanceName ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    purchases?.forEach {  purchaseItem ->
                        when(purchaseItem.purchaseState) {
                            Purchase.PurchaseState.PURCHASED -> {
                                Logger.dev(BuildConfig.TAG, "Tracking purchase with order id: ${purchaseItem.orderId}")

                                Tealium[instanceName]?.inAppPurchaseManager?.trackInAppPurchase(purchaseItem)
                            }
                            Purchase.PurchaseState.PENDING -> {
                                Logger.dev(BuildConfig.TAG, "Purchase pending for order id: ${purchaseItem.orderId}")
                                Tealium[instanceName]?.inAppPurchaseManager?.trackInAppPurchase(purchaseItem)
                            }
                            else -> {
                                Logger.dev(BuildConfig.TAG, "Unable to track purchase: ${purchaseItem.orderId}")
                            }
                        }
                    }
                }

                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    // track cancellation?
                    Logger.dev(BuildConfig.TAG, "Unable to track purchase. User cancelled.")
                }

                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    Logger.dev(BuildConfig.TAG, "Unable to track purchase. Item is already owned.")
                }
                else -> {
                    Logger.dev(BuildConfig.TAG, "Unable to track purchases")
                }
            }
        }
    }
}