package com.tealium.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tealium.core.Tealium
import com.tealium.hosteddatalayer.hostedDataLayer
import com.tealium.mobile.BuildConfig
import com.tealium.mobile.databinding.FragmentHostedDataLayerBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class HostedDataLayerFragment : Fragment() {

    private lateinit var binding: FragmentHostedDataLayerBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHostedDataLayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.clearCacheButton.setOnClickListener {
            onClearCache()
        }
    }

    private fun onClearCache() {
        GlobalScope.launch {
            Tealium[BuildConfig.TEALIUM_INSTANCE]?.hostedDataLayer?.clearCache()
        }
    }
}