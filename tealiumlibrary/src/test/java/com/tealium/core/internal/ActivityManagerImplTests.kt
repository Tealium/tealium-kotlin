package com.tealium.core.internal

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.ActivityManager
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper


@RunWith(RobolectricTestRunner::class)
class ActivityManagerImplTests {

    @MockK(relaxed = true)
    private lateinit var listener: ActivityManager.ActivityLifecycleListener

    @MockK(relaxed = true)
    private lateinit var activityStatusObservable: ActivityManagerImpl.ActivityStatusObservable

    @MockK(relaxed = true)
    private lateinit var callbacks: ActivityManagerImpl.ActivityCallbacks

    private lateinit var looper: Looper
    private lateinit var app: Application

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        app = ApplicationProvider.getApplicationContext()
        looper = Looper.myLooper()!!
    }

    @Test
    fun init_Registers_Callbacks_To_App_Lifecycle() {
        val application = mockk<Application>(relaxed = true)

        createActivityManager(
            application = application,
            callbacks = callbacks
        )

        verify {
            application.registerActivityLifecycleCallbacks(callbacks)
        }
    }

    @Test
    fun init_Clears_Buffer_After_Set_Timeout() {
        createActivityManager(
            bufferTimeout = 1
        )
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(timeout = 2000) {
            activityStatusObservable.timeout()
        }
    }

    @Test
    fun init_With_Negative_Timeout_Does_Not_Clear_Buffer() {
        createActivityManager(
            bufferTimeout = -1
        )
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(inverse = true) {
            activityStatusObservable.timeout()
        }
    }

    @Test
    fun subscribe_Subscribes_Listener_To_Observable() {
        val activityManager = createActivityManager()
        activityManager.subscribe(listener)

        verify {
            activityStatusObservable.registerObserver(listener)
        }
    }

    @Test
    fun unsubscribe_Unsubscribes_Listener_From_Observable() {
        val activityManager = createActivityManager()
        activityManager.unsubscribe(listener)

        verify {
            activityStatusObservable.unregisterObserver(listener)
        }
    }

    @Test
    fun clear_Clears_Observable_Buffer() {
        val activityManager = createActivityManager()
        activityManager.clear()

        verify {
            activityStatusObservable.timeout()
        }
    }

    private fun createActivityManager(
        application: Application = this.app,
        bufferTimeout: Long = 5,
        handler: Handler = Handler(looper),
        observable: ActivityManagerImpl.ActivityStatusObservable = this.activityStatusObservable,
        callbacks: ActivityManagerImpl.ActivityCallbacks = this.callbacks
    ) : ActivityManagerImpl =
        ActivityManagerImpl(
            application, bufferTimeout, handler, observable, callbacks
        )
}