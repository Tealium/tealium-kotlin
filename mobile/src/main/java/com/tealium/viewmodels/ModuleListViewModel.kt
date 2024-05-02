package com.tealium.viewmodels

import androidx.lifecycle.ViewModel

class ModuleListViewModel : ViewModel() {

    val moduleNames = listOf(
        "Consent Manager",
        "Crash Reporter",
        "Hosted Data Layer",
        "Location",
        "Timed Events",
        "Visitor Service",
        "Media Tracking",
        "WebView Consent Sync",
        "In App Purchase",
        "Moments API"
    )
}