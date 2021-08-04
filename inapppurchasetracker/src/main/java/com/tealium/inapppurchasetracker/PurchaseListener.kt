package com.tealium.inapppurchasetracker

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.tealium.core.Logger
import com.tealium.core.Tealium

class PurchaseListener: PurchasesUpdatedListener {
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        Tealium.names().forEach { instanceName ->
            purchases?.forEach {  purchaseItem ->
                when(purchaseItem.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> {
                        Logger.dev(
                            BuildConfig.TAG,
                            "Tracking purchase with order id: ${purchaseItem.orderId}"
                        )
                        Tealium[instanceName]?.inAppPurchaseManager?.trackInAppPurchase(purchaseItem)
                    }
                    Purchase.PurchaseState.PENDING -> {
                        // track?
                    }
                    else -> {
                        Logger.dev(BuildConfig.TAG, "Unable to track purchase: ${purchaseItem.orderId}")
                    }
                }
            }
        }
    }
}