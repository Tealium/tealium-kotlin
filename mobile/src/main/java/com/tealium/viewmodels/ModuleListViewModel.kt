package com.tealium.viewmodels

import androidx.lifecycle.ViewModel

class ModuleListViewModel : ViewModel() {

    val moduleNames = listOf(
            "Consent Manager",
            "Hosted Data Layer",
            "Location",
            "Timed Events",
            "Visitor Service")
}