package com.tealium.inapppurchasetracker

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.tealium.core.Tealium

class PurchaseListener: PurchasesUpdatedListener {
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        Tealium.names().forEach { instanceName ->
            purchases?.forEach {  purchaseItem ->
                Tealium[instanceName]?.inAppPurchaseManager?.trackInAppPurchase(purchaseItem)
            }
        }
    }
}