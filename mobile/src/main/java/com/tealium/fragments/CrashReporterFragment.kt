package com.tealium.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tealium.crashreporter.crashReporter
import com.tealium.mobile.R
import com.tealium.mobile.TealiumHelper
import kotlinx.android.synthetic.main.fragment_crash_reporter.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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
        GlobalScope.launch {
            println("Causing crash")
            val thread = Thread()
            val exception = RuntimeException("crash")
            TealiumHelper.instance.crashReporter?.uncaughtException(thread, exception)
        }
    }
}