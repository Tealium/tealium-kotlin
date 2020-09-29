package com.tealium.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tealium.core.Tealium
import com.tealium.mobile.BuildConfig
import com.tealium.mobile.R
import com.tealium.visitorservice.visitorService
import kotlinx.android.synthetic.main.fragment_visitor_service.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class VisitorServiceFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_visitor_service, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchProfileButton.setOnClickListener {
            onFetchProfile()
        }
    }

    private fun onFetchProfile() {
        GlobalScope.launch {
            println("fetch profile")
            Tealium[BuildConfig.TEALIUM_INSTANCE]?.visitorService?.requestVisitorProfile()
        }
    }
}