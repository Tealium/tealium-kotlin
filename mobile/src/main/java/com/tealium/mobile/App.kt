package com.tealium.mobile

import android.app.Application

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Tealium at App creation.
        TealiumHelper.init(this)
    }
}
