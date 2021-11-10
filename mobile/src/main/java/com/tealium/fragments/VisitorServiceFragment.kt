package com.tealium.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tealium.core.Tealium
import com.tealium.mobile.BuildConfig
import com.tealium.mobile.databinding.FragmentVisitorServiceBinding
import com.tealium.visitorservice.visitorService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class VisitorServiceFragment : Fragment() {

    private lateinit var binding: FragmentVisitorServiceBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentVisitorServiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.fetchProfileButton.setOnClickListener {
            onFetchProfile()
        }
    }

    private fun onFetchProfile() {
        GlobalScope.launch {
            Tealium[BuildConfig.TEALIUM_INSTANCE]?.visitorService?.requestVisitorProfile()
        }
    }
}