package com.tealium.inapppurchasetracker

import com.android.billingclient.api.Purchase

interface InAppPurchaseTracker {
    fun trackInAppPurchase(purchaseItem: Purchase)
}