package com.tealium.mobile

import android.app.Application
import com.google.firebase.FirebaseApp

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Tealium at App creation.
        TealiumHelper.init(this)

        if (BuildConfig.AUTO_TRACKING_PUSH_ENABLED) {
            FirebaseApp.initializeApp(this)
        }
    }
}
