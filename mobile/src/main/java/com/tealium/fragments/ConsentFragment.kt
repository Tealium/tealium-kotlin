package com.tealium.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tealium.autotracking.ActivityDataCollector
import com.tealium.autotracking.Autotracked
import com.tealium.autotracking.autoTracking
import com.tealium.core.Tealium
import com.tealium.core.consent.ConsentCategory
import com.tealium.core.consent.ConsentStatus
import com.tealium.mobile.BuildConfig
import com.tealium.mobile.databinding.FragmentConsentBinding

@Autotracked(name = "ConsentManagement")
class ConsentFragment : Fragment(), ActivityDataCollector {

    private lateinit var binding: FragmentConsentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentConsentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.consentButton.setOnClickListener {
            onConsented()
        }

        binding.notConsentedButton.setOnClickListener {
            onNotConsented()
        }

        binding.resetConsentButton.setOnClickListener {
            onResetConsentStatus()
        }

        binding.categoriesButton.setOnClickListener {
            onCategoriesButton()
        }

        Tealium[BuildConfig.TEALIUM_INSTANCE]?.autoTracking?.trackActivity(this)
    }

    private fun onConsented() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.consentManager?.userConsentStatus = ConsentStatus.CONSENTED
    }

    private fun onNotConsented() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.consentManager?.userConsentStatus = ConsentStatus.NOT_CONSENTED
    }

    private fun onResetConsentStatus() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.consentManager?.reset()
    }

    private fun onCategoriesButton() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.consentManager?.userConsentCategories = setOf(
                ConsentCategory.ANALYTICS,
                ConsentCategory.BIG_DATA
        )
    }

    override fun onCollectActivityData(activityName: String): Map<String, Any>? {
        // delegate to parent activity
        return (activity as? ActivityDataCollector)?.onCollectActivityData(activityName) ?: emptyMap()
    }
}