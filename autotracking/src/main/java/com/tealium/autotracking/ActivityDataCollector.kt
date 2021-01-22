package com.tealium.autotracking

interface ActivityDataCollector {
    fun onCollectActivityData(activityName: String): Map<String, Any>?
}