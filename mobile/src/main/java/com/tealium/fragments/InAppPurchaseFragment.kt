package com.tealium.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.billingclient.api.Purchase
import com.tealium.mobile.R
import com.tealium.mobile.TealiumHelper
import kotlinx.android.synthetic.main.fragment_in_app_purchase.*
import org.json.JSONArray
import org.json.JSONObject

class InAppPurchaseFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_in_app_purchase,container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        purchaseButton.setOnClickListener {
            onPurchase()
        }
    }

    private fun onPurchase() {
        val purchaseData = JSONObject()
        purchaseData.put("orderId", "12345")
        purchaseData.put("purchaseTime", System.currentTimeMillis())
        purchaseData.put("quantity", 1)
        purchaseData.put("productIds", JSONArray().put("abc123"))
        purchaseData.put("autoRenewing", false)
        purchaseData.put("purchaseState", 1)

        val purchase = Purchase(purchaseData.toString(), "tealiumSign")
        TealiumHelper.trackPurchase(purchase, null)
    }
}

