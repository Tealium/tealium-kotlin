package com.tealium.mobile

import android.app.Activity
import android.os.Bundle
import com.tealium.autotracking.ActivityDataCollector
import com.tealium.autotracking.Autotracked

@Autotracked(name = "SomeOtherName")
class SecondActivity: Activity(), ActivityDataCollector {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.second_activity)
    }

    override fun onCollectActivityData(activityName: String): Map<String, Any>? {
        return mapOf("some" to "data")
    }
}