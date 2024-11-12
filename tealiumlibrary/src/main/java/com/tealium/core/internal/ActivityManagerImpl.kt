package com.tealium.core.internal

import android.app.Activity
import android.app.Application
import android.database.Observable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.tealium.core.Tealium
import com.tealium.core.ActivityManager
import com.tealium.tealiumlibrary.BuildConfig
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.TimeUnit

/**
 * This is the default [ActivityManager] implementation, expected to be subscribed to by Tealium instances
 * and not necessarily the individual components. It is expected to be initialized during
 * [Application.onCreate] to ensure that all updates are collected.
 * This allows for [Tealium] instances to be loaded later in the initialization process, and on non-main
 * Threads to minimize impact on startup time.
 *
 * To further minimize startup times, Activity and Application state updates are collected and
 * published on the Main Thread so as not to require the Tealium processing thread to be created
 * during the launch.
 */
class ActivityManagerImpl internal constructor(
    application: Application,
    timeoutSeconds: Long = 10L,
    mainHandler: Handler = Handler(Looper.getMainLooper()),
    private val activityStatusObservable: ActivityStatusObservable = ActivityStatusObservable(),
    private val activityMonitor: ActivityCallbacks = ActivityCallbacks(
        activityStatusObservable
    )
) : ActivityManager {

    override fun subscribe(listener: ActivityManager.ActivityLifecycleListener) {
        activityStatusObservable.registerObserver(listener)
    }

    override fun unsubscribe(listener: ActivityManager.ActivityLifecycleListener) {
        activityStatusObservable.unregisterObserver(listener)
    }

    override fun clear() {
        activityStatusObservable.timeout()
    }

    init {
        if (timeoutSeconds >= 0) {
            mainHandler.postDelayed({
                Log.d(
                    BuildConfig.TAG,
                    "Init grace period expired. Clearing buffered Activities"
                )
                activityStatusObservable.timeout()
            }, TimeUnit.SECONDS.toMillis(timeoutSeconds))
        }

        application.registerActivityLifecycleCallbacks(activityMonitor)
    }

    /**
     * [Application.ActivityLifecycleCallbacks] implementation to receive all [Activity] lifecycle
     * events.
     *
     * @param activityUpdates the [Observable] used for publishing the activity updates to
     */
    class ActivityCallbacks(
        private val activityUpdates: ActivityStatusObservable,
    ) : Application.ActivityLifecycleCallbacks {

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            notify(ActivityManager.ActivityLifecycleType.Created, activity)
        }

        override fun onActivityStarted(activity: Activity) {
            notify(ActivityManager.ActivityLifecycleType.Started, activity)
        }

        override fun onActivityResumed(activity: Activity) {
            notify(ActivityManager.ActivityLifecycleType.Resumed, activity)
        }

        override fun onActivityPaused(activity: Activity) {
            notify(ActivityManager.ActivityLifecycleType.Paused, activity)
        }

        override fun onActivityStopped(activity: Activity) {
            notify(ActivityManager.ActivityLifecycleType.Stopped, activity)
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {
            notify(ActivityManager.ActivityLifecycleType.Destroyed, activity)
        }

        private fun notify(
            lifecycleType: ActivityManager.ActivityLifecycleType,
            activity: Activity
        ) {
            activityUpdates.publish(
                ActivityManager.ActivityStatus(
                    lifecycleType,
                    activity
                )
            )
        }
    }

    class ActivityStatusObservable(
        private val queue: Queue<ActivityManager.ActivityStatus> = LinkedList()
    ) : Observable<ActivityManager.ActivityLifecycleListener>() {

        @Volatile
        var isTimedOut: Boolean = false
            private set

        /**
         * Publishes a new [ActivityManager.ActivityStatus] event to all subscribers.
         *
         * Also buffers for future subscribers, if the [timeout] method has not already been called.
         *
         * @param status The new [ActivityManager.ActivityStatus] update to publish.
         */
        fun publish(status: ActivityManager.ActivityStatus) {
            synchronized(mObservers) {
                if (!isTimedOut) {
                    queue.add(status)
                }

                mObservers.forEach {
                    try {
                        it.onActivityLifecycleUpdated(status)
                    } catch (ignore: Exception) {
                    }
                }
            }
        }

        /**
         * Clears any buffered [ActivityManager.ActivityStatus] events, and stops buffering from this
         * point onwards
         */
        fun timeout() {
            synchronized(mObservers) {
                if (isTimedOut) return

                isTimedOut = true
                queue.clear()
            }
        }

        override fun registerObserver(listener: ActivityManager.ActivityLifecycleListener?) {
            if (listener == null) return

            try {
                synchronized(mObservers) {
                    super.registerObserver(listener)

                    if (!isTimedOut) {
                        queue.forEach {
                            listener.onActivityLifecycleUpdated(it)
                        }
                    }
                }
            } catch (ignore: Exception) {
            }
        }

        override fun unregisterObserver(listener: ActivityManager.ActivityLifecycleListener?) {
            if (listener == null) return

            try {
                super.unregisterObserver(listener)
            } catch (ignore: Exception) {
            }
        }
    }
}