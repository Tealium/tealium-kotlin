package com.tealium.core

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.tealium.core.messaging.EventRouter
import com.tealium.core.persistence.DataLayer
import com.tealium.core.persistence.Expiry

class ActivityObserver(config: TealiumConfig, val eventRouter: EventRouter) {

    private val application: Application = config.application
    private val activityLifecylceCallbacks: Application.ActivityLifecycleCallbacks

    init {
        activityLifecylceCallbacks = createActivityLifecycleCallbacks()
        application.registerActivityLifecycleCallbacks(activityLifecylceCallbacks)
    }

    private fun createActivityLifecycleCallbacks(): Application.ActivityLifecycleCallbacks {
        return object : Application.ActivityLifecycleCallbacks {
            override fun onActivityPaused(activity: Activity?) {
                eventRouter.onActivityPaused(activity)
            }

            override fun onActivityResumed(activity: Activity?) {
                eventRouter.onActivityResumed(activity)
            }

            override fun onActivityStarted(activity: Activity?) {

            }

            override fun onActivityDestroyed(activity: Activity?) {

            }

            override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {

            }

            override fun onActivityStopped(activity: Activity?) {
                eventRouter.onActivityStopped(activity?.isChangingConfigurations ?: false, activity)
            }

            override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {

            }
        }
    }
}