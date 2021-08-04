package com.tealium.inapppurchasetracker

import com.android.billingclient.api.*
import com.tealium.core.*

class InAppPurchaseManager(
    private val context: TealiumContext,
    private val purchaseListener: PurchasesUpdatedListener = PurchaseListener(),
    private val purchaseTracker: InAppPurchaseTracker = InAppPurchaseAutoTracker(context, purchaseListener)
) : Module {

    override val name: String = ""
    override var enabled: Boolean = true

    // Manually track purchase
    fun trackInAppPurchase(purchaseItem: Purchase, data: Map<String, Any>? = null) {
        purchaseTracker.trackInAppPurchase(purchaseItem, data)
    }

    companion object : ModuleFactory {
        @Volatile private var instance: InAppPurchaseManager? = null
        private val contexts = mutableListOf<TealiumContext>()

        override fun create(context: TealiumContext): Module {
            contexts.add(context)
            return instance ?: synchronized(this) {
                instance ?: InAppPurchaseManager(context).also {
                    instance = it
                }
            }
        }
    }
}

val Modules.PurchaseReporter: ModuleFactory
    get() = com.tealium.inapppurchasetracker.InAppPurchaseManager

val Tealium.inAppPurchaseManager: InAppPurchaseManager?
    get() = modules.getModule(InAppPurchaseManager::class.java)