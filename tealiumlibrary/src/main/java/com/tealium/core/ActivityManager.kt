package com.tealium.core

import android.app.Activity
import android.app.Application
import com.tealium.core.internal.ActivityManagerImpl
import com.tealium.core.internal.TealiumInitProvider
import com.tealium.core.persistence.getTimestampMilliseconds

/**
 * Shared utility class for monitoring the current lifecycle state of a running application - [ActivityStatus]
 * objects are buffered to allow delayed/lazy loading ot [Tealium] instances, off of the main thread,
 * yet still receive the [ActivityStatus] in the order they occurred. This allows for the minimum
 * work to be done to ensure correct behavior of the [Tealium] instances.
 *
 * By default, this class will be instantiated automatically at [Application.onCreate] by the [TealiumInitProvider],
 * buffering for a default of 10 seconds.
 * The user can remove this provider, or customise the buffer time via the regular Android Manifest
 * merger process.
 *
 * **Info** - If the user chooses to remove the [TealiumInitProvider], and initialize the [ActivityManager] directly,
 * they should be aware of the following:
 *
 * The [ActivityManager] will buffer [ActivityStatus] objects for a defined period of time, and as such,
 * will also hold static references to any [Activity] instances until either the buffer time has elapsed,
 * or the [clear] method has been called directly by the user.
 *
 * Typically, it is safe to call [clear] after all expected [Tealium] instances have been instantiated.
 *
 * **Note** - Negative buffer times will buffer indefinitely, and users that configure as such **must**
 * ensure that the [clear] method is called at some point to stop memory leaks caused by holding onto [Activity] objects.
 *
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
     * @param type The [Activity] lifecycle type
     * @param activity The [Activity] instance for this lifecycle event
     * @param timestamp The system timestamp in milliseconds that this event occured
     *
     * @see [Application.ActivityLifecycleCallbacks]
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
     * Clears all buffered [ActivityStatus] objects, so that future subscribers will not receive
     * historical updates.
     *
     * If the [ActivityManager] has been configured with a negative timeout, then [Activity] instances
     * can remain in memory indefinitely, and
     */
    fun clear()

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

        /**
         * Returns the singleton [ActivityManager] instance.
         *
         * If the instance is not created, then it will be created, buffering [ActivityStatus] updates
         * for the default timeout of 10 seconds.
         *
         * @param application The [Application] to subscribe to for [Application.ActivityLifecycleCallbacks]
         */
        @JvmStatic
        fun getInstance(application: Application): ActivityManager =
            getInstance(application, 10)

        /**
         * Returns the singleton [ActivityManager] instance.
         *
         * If the instance is not created, then it will be created, buffering [ActivityStatus] updates
         * for the given [activityBufferInSeconds] in seconds.
         *
         * @param application The [Application] to subscribe to for [Application.ActivityLifecycleCallbacks]
         * @param activityBufferInSeconds The time in seconds to buffer [ActivityStatus] updates. Negative values will buffer indefinitely
         * and the user should ensure that [clear] is called at some point to prevent a memory leak.
         */
        @JvmStatic
        fun getInstance(application: Application, activityBufferInSeconds: Long): ActivityManager {
            return instance ?: synchronized(this) {
                instance ?: ActivityManagerImpl(application, timeoutSeconds = activityBufferInSeconds).also {
                    instance = it
                }
            }
        }
    }
}