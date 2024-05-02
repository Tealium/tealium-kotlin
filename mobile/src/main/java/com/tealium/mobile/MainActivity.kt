package com.tealium.mobile

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.tealium.autotracking.Autotracked
import com.tealium.fragments.*
import com.tealium.mobile.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Autotracked
class MainActivity : AppCompatActivity(), CoroutineScope by CoroutineScope(Dispatchers.Default), ModuleListFragment.Callbacks {

    companion object {
        private val TAG = MainActivity::class.qualifiedName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentFragment =
                supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment == null) {
            val moduleListFragment = ModuleListFragment.newInstance()
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragment_container, moduleListFragment)
                    .commit()
        }

        binding.trackButton.setOnClickListener {
            onTrack()
        }

        binding.secondActivity.setOnClickListener {
            val intent = Intent(this@MainActivity, SecondActivity::class.java)
            startActivity(intent)
        }
        binding.thirdActivity.setOnClickListener {
            val intent = Intent(this@MainActivity, ThirdActivity::class.java)
            startActivity(intent)
        }

        if (!BuildConfig.AUTO_TRACKING) {
            TealiumHelper.trackView("MAIN ACTIVITY", null)
        }
    }

    private fun onTrack() {
        TealiumHelper.trackEvent("event1", mapOf("key1" to "value1", "key2" to 2))
    }

    override fun onModuleSelected(moduleName: String) {
        val fragment: Fragment? = when (moduleName) {
            "Consent Manager" -> ConsentFragment()
            "Visitor Service" -> VisitorServiceFragment()
            "Location" -> LocationFragment()
            "Timed Events" -> TimedEventsFragment()
            "Hosted Data Layer" -> HostedDataLayerFragment()
            "Crash Reporter" -> CrashReporterFragment()
            "Media Tracking" -> MediaFragment()
            "WebView Consent Sync" -> ConsentSyncFragment()
            "In App Purchase" -> InAppPurchaseFragment()
            "Moments API" -> MomentsApiFragment()
            else -> null
        }
        fragment?.let {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, it)
                    .addToBackStack(null)
                    .commit()
        }
    }
}