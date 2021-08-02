package com.tealium.inapppurchasetracker

import com.android.billingclient.api.*
import com.tealium.core.Tealium
import com.tealium.core.TealiumContext
import com.tealium.dispatcher.TealiumEvent

class InAppPurchaseAutoTracker(
    private val context: TealiumContext,
    private val purchaseListener: PurchasesUpdatedListener = PurchaseListener(),
    private val billingClient: BillingClient = BillingClient.newBuilder(context.config.application)
        .setListener(purchaseListener)
        .build()
) : InAppPurchaseTracker, BillingClientStateListener {

    private var isBillingServiceConnected: Boolean = false


    init {
        billingClient.startConnection(this)
    }


    private fun restartConnection(billingClientStateListener: BillingClientStateListener) {
        billingClient.startConnection(billingClientStateListener)
    }

    override fun onBillingSetupFinished(p0: BillingResult) {
        // do something
        isBillingServiceConnected = true
    }

    override fun onBillingServiceDisconnected() {
        // do something
        isBillingServiceConnected = false
        restartConnection(this)
    }

    override fun trackInAppPurchase(purchaseItem: Purchase) {
        val purchaseData = mutableMapOf<String, Any>(
            "purchase" to purchaseItem.orderId,
            "purchase_timestamp" to purchaseItem.purchaseTime,
            "purchase_quantity" to purchaseItem.quantity,
            "purchase_skus" to purchaseItem.skus,
            "purchase_is_auto_renewing" to purchaseItem.isAutoRenewing
        )

        context.track(TealiumEvent(IN_APP_PURCHASE_EVENT_NAME, purchaseData))
    }

    private companion object {
        private const val IN_APP_PURCHASE_EVENT_NAME = "purchase"
    }
}