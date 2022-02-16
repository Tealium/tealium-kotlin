package com.tealium.inapppurchase

import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.Purchase
import com.tealium.core.messaging.ActivityObserverListener

interface InAppPurchaseTracker: BillingClientStateListener, ActivityObserverListener {
    fun trackInAppPurchase(purchaseItem: Purchase, data: Map<String, Any>? = null)
}