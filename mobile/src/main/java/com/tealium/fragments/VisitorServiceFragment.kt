package com.tealium.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import com.tealium.core.Tealium
import com.tealium.core.messaging.VisitorIdUpdatedListener
import com.tealium.mobile.BuildConfig
import com.tealium.mobile.databinding.FragmentVisitorServiceBinding
import com.tealium.visitorservice.visitorService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class VisitorServiceFragment : Fragment() {

    private lateinit var binding: FragmentVisitorServiceBinding
    private var existingIdentity: String = ""
    private val visitorIdListener = object : VisitorIdUpdatedListener {
        override fun onVisitorIdUpdated(visitorId: String) {
            activity?.runOnUiThread {
                setVisitorId(visitorId)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVisitorServiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.fetchProfileButton.setOnClickListener {
            onFetchProfile()
        }

        binding.btnIdentifyUser.setOnClickListener {
            val identity = binding.editCurrentIdentity.text.toString()
            if (identity.isNotBlank()) {
                onSetIdentity(identity)
            }
        }

        binding.editCurrentIdentity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                binding.btnIdentifyUser.isEnabled = s.toString() != existingIdentity
            }
        })

        setVisitorId(Tealium[BuildConfig.TEALIUM_INSTANCE]?.visitorId ?: "")
    }

    override fun onResume() {
        super.onResume()

        existingIdentity =
            Tealium[BuildConfig.TEALIUM_INSTANCE]?.dataLayer?.getString(
                BuildConfig.IDENTITY_KEY,
            ) ?: ""
        binding.editCurrentIdentity.setText(existingIdentity)

        Tealium[BuildConfig.TEALIUM_INSTANCE]?.events?.subscribe(visitorIdListener)
    }

    override fun onDestroy() {
        super.onDestroy()

        Tealium[BuildConfig.TEALIUM_INSTANCE]?.events?.unsubscribe(visitorIdListener)
    }

    @UiThread
    private fun setVisitorId(visitorId: String) {
        binding.txtVisitorIdPlaceholder.text = visitorId
    }

    private fun onFetchProfile() {
        GlobalScope.launch {
            Tealium[BuildConfig.TEALIUM_INSTANCE]?.visitorService?.requestVisitorProfile()
        }
    }

    private fun onSetIdentity(identity: String) {
        existingIdentity = identity
        GlobalScope.launch {
            Tealium[BuildConfig.TEALIUM_INSTANCE]?.dataLayer?.putString(
                BuildConfig.IDENTITY_KEY,
                identity
            )
        }
    }
}