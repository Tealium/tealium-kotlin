package com.tealium.core.internal

import android.app.Activity
import com.tealium.core.ActivityManager
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.LinkedList
import java.util.Queue

@RunWith(RobolectricTestRunner::class)
class ActivityStatusObservableTests {

    @MockK(relaxed = true)
    private lateinit var listener: ActivityManager.ActivityLifecycleListener

    @MockK
    private lateinit var activity: Activity

    private lateinit var queue: Queue<ActivityManager.ActivityStatus>
    private lateinit var activityStatusObservable: ActivityManagerImpl.ActivityStatusObservable
    private lateinit var created: ActivityManager.ActivityStatus

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        created = createStatus(ActivityManager.ActivityLifecycleType.Created)
        queue = LinkedList()
        activityStatusObservable = ActivityManagerImpl.ActivityStatusObservable(queue)
    }

    @Test
    fun isTimedOut_Is_False_When_Not_Timed_Out_Yet() {
        assertFalse(activityStatusObservable.isTimedOut)
    }

    @Test
    fun isTimedOut_Is_True_After_Timeout_Called() {
        activityStatusObservable.timeout()
        assertTrue(activityStatusObservable.isTimedOut)
    }

    @Test
    fun timeout_Clears_Queue() {
        activityStatusObservable.publish(created)
        activityStatusObservable.timeout()

        assertTrue(queue.isEmpty())
    }

    @Test
    fun publish_Enqueues_Updates_When_Not_Timed_Out() {
        activityStatusObservable.publish(created)

        assertTrue(queue.contains(created))
    }

    @Test
    fun publish_No_Longer_Enqueues_After_Timeout_Called() {
        activityStatusObservable.publish(created)

        activityStatusObservable.timeout()
        activityStatusObservable.publish(created)

        assertTrue(queue.isEmpty())
    }

    @Test
    fun publish_Publishes_To_All_Subscribers() {
        activityStatusObservable.registerObserver(listener)
        val listener2 = mockk<ActivityManager.ActivityLifecycleListener>(relaxed = true)
        activityStatusObservable.registerObserver(listener2)

        activityStatusObservable.publish(created)

        verify {
            listener.onActivityLifecycleUpdated(created)
            listener2.onActivityLifecycleUpdated(created)
        }
    }

    @Test
    fun publish_Publishes_To_All_Subscribers_Even_When_One_Fails() {
        activityStatusObservable.registerObserver(listener)
        every { listener.onActivityLifecycleUpdated(any()) } throws Exception()
        val listener2 = mockk<ActivityManager.ActivityLifecycleListener>(relaxed = true)
        activityStatusObservable.registerObserver(listener2)

        activityStatusObservable.publish(created)

        verify {
            listener.onActivityLifecycleUpdated(created)
            listener2.onActivityLifecycleUpdated(created)
        }
    }

    @Test
    fun subscribe_Subscribes_Listener_To_Future_Updates() {
        activityStatusObservable.registerObserver(listener)

        activityStatusObservable.publish(created)

        verify {
            listener.onActivityLifecycleUpdated(created)
        }
    }

    @Test
    fun subscribe_Emits_Buffered_Updates_When_Not_Timed_Out() {
        val paused = createStatus(ActivityManager.ActivityLifecycleType.Paused)
        val resumed = createStatus(ActivityManager.ActivityLifecycleType.Resumed)
        queue.addAll(listOf(created, resumed))
        activityStatusObservable.registerObserver(listener)

        activityStatusObservable.publish(paused)

        verifyOrder {
            listener.onActivityLifecycleUpdated(created)
            listener.onActivityLifecycleUpdated(resumed)
            listener.onActivityLifecycleUpdated(paused)
        }
    }

    @Test
    fun subscribe_Does_Not_Emit_Buffered_Updates_When_Timed_Out() {
        queue.add(created)

        activityStatusObservable.timeout()
        activityStatusObservable.registerObserver(listener)

        verify {
            listener wasNot Called
        }
    }

    @Test
    fun subscribe_Does_Not_Emit_Buffered_Updates_When_Duplicate_Listener() {
        queue.add(created)
        activityStatusObservable.registerObserver(listener)
        activityStatusObservable.registerObserver(listener)

        verify(exactly = 1) {
            listener.onActivityLifecycleUpdated(created)
        }
    }

    @Test
    fun unsubscribe_Stops_Listener_From_Receiving_Future_Updates() {
        activityStatusObservable.registerObserver(listener)
        activityStatusObservable.publish(created)

        activityStatusObservable.unregisterObserver(listener)
        activityStatusObservable.publish(created)

        verify(exactly = 1) {
            listener.onActivityLifecycleUpdated(created)
        }
    }

    private fun createStatus(
        type: ActivityManager.ActivityLifecycleType,
        activity: Activity = this.activity
    ): ActivityManager.ActivityStatus =
        ActivityManager.ActivityStatus(type, activity)
}