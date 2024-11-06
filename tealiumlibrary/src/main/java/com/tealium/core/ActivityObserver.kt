package com.tealium.core

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.tealium.core.messaging.EventRouter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Deprecated("Replaced by [ActivityManager]")
class ActivityObserver(config: TealiumConfig, val eventRouter: EventRouter, private val backgroundScope: CoroutineScope) {

    private val application: Application = config.application
    private val activityLifecycleCallbacks: Application.ActivityLifecycleCallbacks

    init {
        activityLifecycleCallbacks = createActivityLifecycleCallbacks()
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }

    private fun createActivityLifecycleCallbacks(): Application.ActivityLifecycleCallbacks {
        return object : Application.ActivityLifecycleCallbacks {
            override fun onActivityPaused(activity: Activity) {
                eventRouter.onActivityPaused(activity)
            }

            override fun onActivityResumed(activity: Activity) {
                eventRouter.onActivityResumed(activity)
            }

            override fun onActivityStarted(activity: Activity) {

            }

            override fun onActivityDestroyed(activity: Activity) {

            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

            }

            override fun onActivityStopped(activity: Activity) {
                eventRouter.onActivityStopped(activity, activity.isChangingConfigurations)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

            }
        }
    }
}