package com.tealium.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tealium.mobile.R
import kotlinx.android.synthetic.main.fragment_crash_reporter.*

class CrashReporterFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_crash_reporter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        createCrashButton.setOnClickListener {
            causeCrash()
        }
    }

    private fun causeCrash() {
        throw RuntimeException("crash")
    }
}