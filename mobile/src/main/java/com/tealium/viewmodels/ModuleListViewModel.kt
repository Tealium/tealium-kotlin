package com.tealium.viewmodels

import androidx.lifecycle.ViewModel

class ModuleListViewModel : ViewModel() {

    val moduleNames = listOf(
            "Consent Manager",
            "Location",
            "Visitor Service")
}