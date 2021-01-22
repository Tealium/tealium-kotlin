package com.tealium.autotracking

import android.app.Activity
import com.tealium.core.messaging.ActivityObserverListener

interface ActivityTracker: ActivityObserverListener  {
    fun trackActivity(activity: Activity, data: Map<String, Any>?)
    fun trackActivity(activityDataCollector: ActivityDataCollector, data: Map<String, Any>?)
}