package com.tealium.mobile

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.tealium.core.Tealium
import com.tealium.dispatcher.TealiumEvent
import com.tealium.dispatcher.TealiumView
import com.tealium.fragments.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class MainActivity : AppCompatActivity(), CoroutineScope by CoroutineScope(Dispatchers.Default), ModuleListFragment.Callbacks {

    companion object {
        private val TAG = MainActivity::class.qualifiedName
    }

    private val tealium: Tealium
        get() = TealiumHelper.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val currentFragment =
                supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment == null) {
            val moduleListFragment = ModuleListFragment.newInstance()
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragment_container, moduleListFragment)
                    .commit()
        }

        trackButton.setOnClickListener {
            onTrack()
        }

        val viewDispatch = TealiumView("MAIN ACTIVITY")
        tealium.track(viewDispatch)
    }

    private fun onTrack() {
        val eventDispatch = TealiumEvent( "event1", mutableMapOf("key1" to "value1", "key2" to 2))

        tealium.track(eventDispatch)
    }

    override fun onModuleSelected(moduleName: String) {
        val fragment: Fragment? = when (moduleName) {
            "Consent Manager" -> ConsentFragment()
            "Visitor Service" -> VisitorServiceFragment()
            "Location" -> LocationFragment()
            "Hosted Data Layer" -> HostedDataLayerFragment()
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