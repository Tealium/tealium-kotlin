package com.tealium.tagmanagementdispatcher

import android.os.Handler
import com.tealium.tagmanagementdispatcher.internal.Delayed
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class WebViewInitPolicyTest {

    @RelaxedMockK
    private lateinit var subscriber: WebViewInitPolicy.WebViewInitPolicyReadyListener

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun immediate_Invokes_Subscribers_Immediately() {
        WebViewInitPolicy.immediate()
            .subscribe(subscriber)
        verify {
            subscriber.onWebViewInitPolicyReady()
        }
    }

    @Test
    fun delayed_Invokes_Subscribers_After_Delay() {
        val policy = WebViewInitPolicy.delayed(500)
        policy.subscribe(subscriber)
        verify(inverse = true) {
            subscriber.onWebViewInitPolicyReady()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        verify(exactly = 1) {
            subscriber.onWebViewInitPolicyReady()
        }
    }

    @Test
    fun delayed_Posts_Task_Delayed_By_Time_Since_Init() {
        val timingProvider = mockk<() -> Long>()
        every { timingProvider.invoke() } returnsMany listOf(0, 100, 200)
        val handler = mockk<Handler>(relaxed = true)
        val delay = 500L

        val policy = Delayed(delay, timingProvider, handler)
        policy.subscribe(subscriber)
        policy.subscribe(subscriber)

        verify {
            handler.postDelayed(any(), delay - 100)
            handler.postDelayed(any(), delay - 200)
        }
    }

    @Test
    fun userTriggered_Invokes_Subscribers_After_Calling_Ready() {
        val policy = WebViewInitPolicy.userTriggered()
        policy.subscribe(subscriber)
        verify(inverse = true) {
            subscriber.onWebViewInitPolicyReady()
        }

        policy.ready()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        verify(exactly = 1) {
            subscriber.onWebViewInitPolicyReady()
        }
    }

    @Test
    fun userTriggered_Does_Not_Invoke_Subscribers_After_Calling_Ready_Multiple_Times() {
        val policy = WebViewInitPolicy.userTriggered()
        policy.subscribe(subscriber)
        verify(inverse = true) {
            subscriber.onWebViewInitPolicyReady()
        }

        policy.ready()
        policy.ready()
        policy.ready()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        verify(exactly = 1) {
            subscriber.onWebViewInitPolicyReady()
        }
    }

    @Test
    fun onMainThreadIdle_Invokes_Subscribers_When_Main_Looper_Idle() {
        val policy = WebViewInitPolicy.onMainThreadIdle()
        policy.subscribe(subscriber)
        verify(inverse = true) {
            subscriber.onWebViewInitPolicyReady()
        }

        ShadowLooper.idleMainLooper()
        verify(exactly = 1) {
            subscriber.onWebViewInitPolicyReady()
        }
    }

    @Test
    fun onMainThreadIdle_Does_Not_Invoke_Subscribers_After_Idle_Multiple_Times() {
        val policy = WebViewInitPolicy.onMainThreadIdle()
        policy.subscribe(subscriber)
        verify(inverse = true) {
            subscriber.onWebViewInitPolicyReady()
        }

        ShadowLooper.idleMainLooper()
        ShadowLooper.idleMainLooper()
        ShadowLooper.idleMainLooper()
        verify(exactly = 1) {
            subscriber.onWebViewInitPolicyReady()
        }
    }
}