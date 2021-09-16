package com.tealium.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.fragment.app.Fragment
import com.tealium.mobile.TealiumHelper
import com.tealium.mobile.databinding.FragmentConsentSyncBinding
import org.json.JSONObject

class ConsentSyncFragment : Fragment() {

    private lateinit var binding: FragmentConsentSyncBinding
    private var categories: String? = TealiumHelper.fetchConsentCategories()
    private val url =
        "https://tags.tiqcdn.com/utag/tealiummobile/consent-manager-demo/prod/mobile.html?consent_categories=${categories ?: ""}"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentConsentSyncBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val webView: WebView = binding.consentSyncWebView
        webView.settings.run {
            javaScriptEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        webView.loadUrl(url)
        webView.addJavascriptInterface(this, "tealiumAndroid")
    }

    @JavascriptInterface
    fun postMessage(str: String?) {
        val consentObj = JSONObject(str)
        val categories = consentObj.optString(CONSENT_CATEGORIES, "")
        if (!categories.isNullOrEmpty()) {
            TealiumHelper.setConsentCategories(categories.split(",").toSet())
        }
    }

    companion object {
        const val CONSENT_CATEGORIES = "categories"
    }
}