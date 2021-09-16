package com.tealium.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tealium.mobile.databinding.FragmentCrashReporterBinding

class CrashReporterFragment : Fragment() {

    private lateinit var binding: FragmentCrashReporterBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCrashReporterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.createCrashButton.setOnClickListener {
            causeCrash()
        }
    }

    private fun causeCrash() {
        throw RuntimeException("crash")
    }
}