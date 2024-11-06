package com.tealium.core

import android.app.Activity
import android.app.Application
import com.tealium.core.internal.ActivityManagerImpl
import com.tealium.core.persistence.getTimestampMilliseconds

/**
 * Shared utility class for monitoring the current lifecycle state of a running application.
 *
 * This class should provide all Tealium instances with the current activity status.
 */
interface ActivityManager {

    /**
     * Possible types of activity behaviour, which align with the possible events triggered by the
     * [android.app.Application.ActivityLifecycleCallbacks]
     */
    enum class ActivityLifecycleType {
        Created,
        Started,
        Stopped,
        Resumed,
        Paused,
        Destroyed
    }

    /**
     * This class allows for activity events to be published as a single type.
     *
     *
     */
    data class ActivityStatus(
        val type: ActivityLifecycleType,
        val activity: Activity,
        val timestamp: Long = getTimestampMilliseconds()
    )

    /**
     * Subscribes the given [listener] to all future [ActivityStatus] updates.
     *
     * If there are previously queued [ActivityStatus] updates, then these will also be published to
     * the [listener]
     *
     * @param listener The listener to receive the [ActivityStatus] updates.
     */
    fun subscribe(listener: ActivityLifecycleListener)

    /**
     * Unsubscribes the given [listener] from all future [ActivityStatus] updates.
     *
     * @param listener The listener to stop receiving the [ActivityStatus] updates.
     */
    fun unsubscribe(listener: ActivityLifecycleListener)

    /**
     * Listener for [ActivityStatus] update events. These events follow the standard Android [Activity]
     * lifecycle events.
     */
    fun interface ActivityLifecycleListener {

        /**
         * Called whenever there is a new [ActivityStatus].
         */
        fun onActivityLifecycleUpdated(activityStatus: ActivityStatus)
    }

    companion object {
        private var instance: ActivityManagerImpl? = null
        fun getInstance(application: Application): ActivityManager {
            return instance ?: synchronized(this) {
                instance ?: ActivityManagerImpl(application).also {
                    instance = it
                }
            }
        }
    }
}