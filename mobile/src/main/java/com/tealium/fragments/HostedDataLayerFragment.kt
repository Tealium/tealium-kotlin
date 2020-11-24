package com.tealium.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tealium.core.Tealium
import com.tealium.hosteddatalayer.hostedDataLayer
import com.tealium.mobile.BuildConfig
import com.tealium.mobile.R
import kotlinx.android.synthetic.main.fragment_hosted_data_layer.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class HostedDataLayerFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_hosted_data_layer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        clearCacheButton.setOnClickListener {
            onClearCache()
        }
    }

    private fun onClearCache() {
        GlobalScope.launch {
            Tealium[BuildConfig.TEALIUM_INSTANCE]?.hostedDataLayer?.clearCache()
        }
    }
}