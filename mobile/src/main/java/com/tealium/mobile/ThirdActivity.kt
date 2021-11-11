package com.tealium.mobile

import android.app.Activity
import android.os.Bundle
import com.tealium.autotracking.ActivityDataCollector
import com.tealium.autotracking.Autotracked

class ThirdActivity: Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.third_activity)
    }
}