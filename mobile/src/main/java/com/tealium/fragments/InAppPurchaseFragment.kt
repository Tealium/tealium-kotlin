package com.tealium.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.billingclient.api.Purchase
import com.tealium.mobile.R
import com.tealium.mobile.TealiumHelper
import com.tealium.mobile.databinding.FragmentInAppPurchaseBinding
import com.tealium.mobile.databinding.FragmentMediaBinding
import org.json.JSONArray
import org.json.JSONObject

class InAppPurchaseFragment : Fragment() {

    private lateinit var binding: FragmentInAppPurchaseBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInAppPurchaseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.purchaseButton.setOnClickListener {
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

