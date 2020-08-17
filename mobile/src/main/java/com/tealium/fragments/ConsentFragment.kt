package com.tealium.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tealium.core.consent.ConsentCategory
import com.tealium.core.consent.ConsentStatus
import com.tealium.mobile.TealiumHelper
import com.tealium.mobile.R
import kotlinx.android.synthetic.main.fragment_consent.*

class ConsentFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_consent, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        consentButton.setOnClickListener {
            onConsented()
        }

        notConsentedButton.setOnClickListener {
            onNotConsented()
        }

        resetConsentButton.setOnClickListener {
            onResetConsentStatus()
        }

        categoriesButton.setOnClickListener {
            onCategoriesButton()
        }
    }

    private fun onConsented() {
//       TealiumHelper.instance.consentManager.userConsentStatus = ConsentStatus.CONSENTED
    }

    private fun onNotConsented() {
//       TealiumHelper.instance.consentManager.userConsentStatus = ConsentStatus.NOT_CONSENTED
    }

    private fun onResetConsentStatus() {
//       TealiumHelper.instance.consentManager.reset()
    }

    private fun onCategoriesButton() {
//       TealiumHelper.instance.consentManager.userConsentCategories = setOf(
//                ConsentCategory.ANALYTICS,
//                ConsentCategory.BIG_DATA
//        )
    }
}