package com.tealium.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.tealium.core.Tealium
import com.tealium.location.location
import com.tealium.mobile.BuildConfig
import com.tealium.mobile.databinding.FragmentLocationBinding

class LocationFragment : Fragment() {

    private lateinit var binding: FragmentLocationBinding
    private val FINE_LOCATION_REQUEST_CODE = 100
    private val COARSE_LOCATION_REQUEST_CODE = 101

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requestLocationPermission()
        binding = FragmentLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonStartLocation.setOnClickListener {
            Tealium[BuildConfig.TEALIUM_INSTANCE]?.location?.startLocationTracking(true, 5000)
        }
    }

    private fun requestLocationPermission() {
        activity?.let {
            if (ContextCompat.checkSelfPermission(
                    it.applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    it,
                    Array<String>(1) { Manifest.permission.ACCESS_FINE_LOCATION },
                    FINE_LOCATION_REQUEST_CODE
                )
            }

            if (ContextCompat.checkSelfPermission(
                    it.applicationContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    it,
                    Array<String>(1) { Manifest.permission.ACCESS_COARSE_LOCATION },
                    COARSE_LOCATION_REQUEST_CODE
                )
            }
        }
    }
}