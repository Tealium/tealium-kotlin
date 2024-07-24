package com.tealium.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import com.tealium.core.Tealium
import com.tealium.mobile.BuildConfig
import com.tealium.mobile.TealiumHelper
import com.tealium.mobile.databinding.FragmentMomentsApiBinding
import com.tealium.momentsapi.EngineResponse
import com.tealium.momentsapi.ErrorCode
import com.tealium.momentsapi.ResponseListener
import com.tealium.momentsapi.momentsApi
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MomentsApiFragment : Fragment() {
    private lateinit var binding: FragmentMomentsApiBinding
    private var lastEngineId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMomentsApiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sharedPreferences = requireActivity().getPreferences(Context.MODE_PRIVATE)

        val savedEngineId = sharedPreferences.getString("engineId", "")
        binding.editEngineId.setText(savedEngineId)

        binding.editEngineId.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                lastEngineId = s.toString()
                sharedPreferences.edit().putString("engineId", lastEngineId).apply()
            }
        })

        binding.fetchEngineResponseButton.setOnClickListener {
            TealiumHelper.trackEvent("fetch engine button click", emptyMap())
            Executors.newSingleThreadScheduledExecutor().schedule({
                onFetchEngineData()
            }, 550, TimeUnit.MILLISECONDS)
        }
    }

    private fun onFetchEngineData() {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.momentsApi?.fetchEngineResponse(
            lastEngineId,
            object : ResponseListener<EngineResponse> {
                override fun success(data: EngineResponse) {
                    activity?.runOnUiThread {
                        setEngineResponseData(data)
                    }
                }

                override fun failure(errorCode: ErrorCode, message: String) {
                    activity?.runOnUiThread {
                        createToast("Engine Response Error: ${errorCode.value} - $message")
                    }
                }
            })
    }

    @UiThread
    private fun setEngineResponseData(engineResponse: EngineResponse) {
        engineResponse.audiences?.let {
            binding.txtAudiencesLabel.visibility = View.VISIBLE
            binding.txtAudiencesPlaceholder.visibility = View.VISIBLE
            binding.txtAudiencesPlaceholder.text = it.toString()
        }

        engineResponse.badges?.let {
            binding.txtBadgesLabel.visibility = View.VISIBLE
            binding.txtBadgesPlaceholder.visibility = View.VISIBLE
            binding.txtBadgesPlaceholder.text = it.toString()
        }

        engineResponse.strings?.let {
            binding.txtStringsLabel.visibility = View.VISIBLE
            binding.txtStringsPlaceholder.visibility = View.VISIBLE
            binding.txtStringsPlaceholder.text = it.toString()
        }

        engineResponse.numbers?.let {
            binding.txtNumbersLabel.visibility = View.VISIBLE
            binding.txtNumbersPlaceholder.visibility = View.VISIBLE
            binding.txtNumbersPlaceholder.text = it.toString()
        }

        engineResponse.booleans?.let {
            binding.txtBooleansLabel.visibility = View.VISIBLE
            binding.txtBooleansPlaceholder.visibility = View.VISIBLE
            binding.txtBooleansPlaceholder.text = it.toString()
        }

        engineResponse.dates?.let {
            binding.txtDatesLabel.visibility = View.VISIBLE
            binding.txtDatesPlaceholder.visibility = View.VISIBLE
            binding.txtDatesPlaceholder.text = it.toString()
        }
    }

    @UiThread
    private fun createToast(message: String) {
        Toast.makeText(this@MomentsApiFragment.context, message, Toast.LENGTH_SHORT)
            .show()
    }
}