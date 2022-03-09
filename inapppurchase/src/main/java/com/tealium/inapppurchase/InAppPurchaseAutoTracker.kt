package com.tealium.inapppurchase

import android.app.Activity
import com.android.billingclient.api.*
import com.tealium.core.DateUtils
import com.tealium.core.Logger
import com.tealium.core.TealiumContext
import com.tealium.dispatcher.TealiumEvent
import java.util.*

class InAppPurchaseAutoTracker(
    private val context: TealiumContext,
    private val purchaseListener: PurchasesUpdatedListener = PurchaseListener(),
    private var billingClient: BillingClient? = null
) : InAppPurchaseTracker {

    private fun startConnection() {
        if (billingClient == null) {
            billingClient = BillingClient.newBuilder(context.config.application)
                .setListener(purchaseListener)
                .enablePendingPurchases()
                .build()
        }

        if (billingClient?.isReady == true) {
            billingClient?.startConnection(this)
        }
    }

    private fun endConnection() {
        billingClient?.endConnection()
        billingClient = null
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                Logger.dev(BuildConfig.TAG, "Connection established to BillingClient")
            }
            else -> {
                Logger.dev(BuildConfig.TAG, "Unable to connect: BillingClient is not available")
            }
        }
    }

    override fun onBillingServiceDisconnected() {
        startConnection()
    }

    override fun trackInAppPurchase(purchaseItem: Purchase, data: Map<String, Any>?) {
        val purchaseData = mutableMapOf<String, Any>(
            "autotracked" to true,
            PURCHASE_ORDER_ID to purchaseItem.orderId,
            PURCHASE_TIMESTAMP to purchaseItem.purchaseTime,
            PURCHASE_DATE to DateUtils.formatDate(Date(purchaseItem.purchaseTime)),
            PURCHASE_QUANTITY to purchaseItem.quantity,
            PURCHASE_SKUS to purchaseItem.skus,
            PURCHASE_AUTORENEWING to purchaseItem.isAutoRenewing,
            PURCHASE_STATE to purchaseItem.purchaseState
        )

        data?.let {
            purchaseData.putAll(it)
        }

        context.track(TealiumEvent(IN_APP_PURCHASE_EVENT, purchaseData))
    }

    override fun onActivityPaused(activity: Activity?) {
        // do nothing
    }

    override fun onActivityResumed(activity: Activity?) {
        if (billingClient == null) {
            startConnection()
        }
    }

    override fun onActivityStopped(activity: Activity?, isChangingConfiguration: Boolean) {
        billingClient?.let {
            if (it.connectionState == BillingClient.ConnectionState.CLOSED) {
                endConnection()
            }
        }
    }

    private companion object {
        private const val IN_APP_PURCHASE_EVENT = "in_app_purchase"
        const val PURCHASE_ORDER_ID = "purchase_order_id"

        @Deprecated(
            "Use PURCHASE_DATE instead.",
            replaceWith = ReplaceWith("PURCHASE_DATE"),
            level = DeprecationLevel.WARNING
        )
        const val PURCHASE_TIMESTAMP = "purchase_timestamp"
        const val PURCHASE_DATE = "purchase_date"
        const val PURCHASE_QUANTITY = "purchase_quantity"
        const val PURCHASE_SKUS = "purchase_skus"
        const val PURCHASE_AUTORENEWING = "purchase_is_auto_renewing"
        const val PURCHASE_STATE = "purchase_state"
    }
}