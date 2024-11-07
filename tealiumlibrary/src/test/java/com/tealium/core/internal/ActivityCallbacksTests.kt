package com.tealium.core.internal

import android.app.Activity
import com.tealium.core.ActivityManager
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class ActivityCallbacksTests {

    @MockK(relaxed = true)
    private lateinit var activityObservable: ActivityManagerImpl.ActivityStatusObservable

    @MockK
    private lateinit var activity: Activity

    private lateinit var callbacks: ActivityManagerImpl.ActivityCallbacks

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        callbacks = ActivityManagerImpl.ActivityCallbacks(activityObservable)
    }

    @Test
    fun onActivityCreated_Publishes_Created_Update() {
        callbacks.onActivityCreated(activity, null)

        verify {
            activityObservable.publish(match {
                it.activity == activity
                        && it.type == ActivityManager.ActivityLifecycleType.Created
            })
        }
    }

    @Test
    fun onActivityStarted_Publishes_Started_Update() {
        callbacks.onActivityStarted(activity)

        verify {
            activityObservable.publish(match {
                it.activity == activity
                        && it.type == ActivityManager.ActivityLifecycleType.Started
            })
        }
    }

    @Test
    fun onActivityPaused_Publishes_Paused_Update() {
        callbacks.onActivityPaused(activity)

        verify {
            activityObservable.publish(match {
                it.activity == activity
                        && it.type == ActivityManager.ActivityLifecycleType.Paused
            })
        }
    }

    @Test
    fun onActivityResumed_Publishes_Resumed_Update() {
        callbacks.onActivityResumed(activity)

        verify {
            activityObservable.publish(match {
                it.activity == activity
                        && it.type == ActivityManager.ActivityLifecycleType.Resumed
            })
        }
    }

    @Test
    fun onActivityStopped_Publishes_Stopped_Update() {
        callbacks.onActivityStopped(activity)

        verify {
            activityObservable.publish(match {
                it.activity == activity
                        && it.type == ActivityManager.ActivityLifecycleType.Stopped
            })
        }
    }

    @Test
    fun onActivityDestroyed_Publishes_Destroyed_Update() {
        callbacks.onActivityDestroyed(activity)

        verify {
            activityObservable.publish(match {
                it.activity == activity
                        && it.type == ActivityManager.ActivityLifecycleType.Destroyed
            })
        }
    }
}