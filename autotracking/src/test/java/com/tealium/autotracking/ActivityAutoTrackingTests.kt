package com.tealium.autotracking

import android.app.Activity
import com.tealium.autotracking.internal.ActivityAutoTracker
import com.tealium.autotracking.internal.ActivityBlacklist
import com.tealium.core.TealiumContext
import com.tealium.dispatcher.TealiumView
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActivityAutoTrackingTests {

    @RelaxedMockK
    lateinit var mockContext: TealiumContext

    @MockK
    lateinit var mockDataCollector: ActivityDataCollector

    @MockK
    lateinit var mockBlacklist: ActivityBlacklist

    private var nonAnnotatedActivity = NonAnnotatedActivity()
    private var annotatedActivity = AnnotatedActivity()
    private var annotatedActivityWithOverride = AnnotatedActivityWithOverride()
    private var annotatedActivityWithoutTracking = AnnotatedActivityWithoutTracking()
    private var activityWithDataCollector = ActivityWithDataCollector()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        // not-blacklisted by default.
        every { mockBlacklist.isBlacklisted(any()) } returns false
    }

    @Test
    fun autoTrackingMode_None_DoesNotTrack() {
        val tracker = ActivityAutoTracker(mockContext, AutoTrackingMode.NONE)
        tracker.onActivityResumed(nonAnnotatedActivity)
        tracker.onActivityResumed(annotatedActivity)
        tracker.onActivityResumed(annotatedActivityWithOverride)

        verify(exactly = 0) {
            mockContext.track(any())
        }
    }

    @Test
    fun autoTrackingMode_Annotated_TracksAllActivities() {
        val tracker = ActivityAutoTracker(mockContext, AutoTrackingMode.ANNOTATED)
        tracker.onActivityResumed(nonAnnotatedActivity)
        tracker.onActivityResumed(annotatedActivity)
        tracker.onActivityResumed(annotatedActivityWithOverride)

        verify(exactly = 2) {
            mockContext.track(any())
        }
    }

    @Test
    fun autoTrackingMode_Annotated_SendsLocalClassName() {
        val tracker = ActivityAutoTracker(mockContext, AutoTrackingMode.ANNOTATED)
        tracker.onActivityResumed(annotatedActivity)

        verify {
            mockContext.track(match {
                it is TealiumView
                        && it.viewName == "AnnotatedActivity"
            })
        }
    }

    @Test
    fun autoTrackingMode_Annotated_SendsOverriddenActivityName() {
        val tracker = ActivityAutoTracker(mockContext, AutoTrackingMode.ANNOTATED)
        tracker.onActivityResumed(annotatedActivityWithOverride)

        verify {
            mockContext.track(match {
                it is TealiumView
                        && it.viewName == "overridden"
            })
        }
    }

    @Test
    fun autoTrackingMode_Full_TracksAllActivities() {
        val tracker = ActivityAutoTracker(mockContext, AutoTrackingMode.FULL)
        tracker.onActivityResumed(nonAnnotatedActivity)
        tracker.onActivityResumed(annotatedActivity)
        tracker.onActivityResumed(annotatedActivityWithOverride)

        verify(exactly = 3) {
            mockContext.track(any())
        }
    }

    @Test
    fun autoTrackingMode_Full_SendsLocalClassName() {
        val tracker = ActivityAutoTracker(mockContext, AutoTrackingMode.FULL)
        tracker.onActivityResumed(annotatedActivity)

        verify {
            mockContext.track(match {
                it is TealiumView
                        && it.viewName == "AnnotatedActivity"
            })
        }
    }

    @Test
    fun autoTrackingMode_Full_SendsOverriddenActivityName() {
        val tracker = ActivityAutoTracker(mockContext, AutoTrackingMode.FULL)
        tracker.onActivityResumed(annotatedActivityWithOverride)

        verify {
            mockContext.track(match {
                it is TealiumView
                        && it.viewName == "overridden"
            })
        }
    }

    @Test
    fun autoTrackingMode_Full_TrackSetToFalseDoesNotTrack() {
        val tracker = ActivityAutoTracker(mockContext, AutoTrackingMode.FULL)
        tracker.onActivityResumed(annotatedActivityWithoutTracking)

        verify(exactly = 0) {
            mockContext.track(any())
        }
    }

    @Test
    fun configChange_DoesNotTrack() {
        val tracker = ActivityAutoTracker(mockContext, AutoTrackingMode.FULL)
        tracker.onActivityResumed(nonAnnotatedActivity)
        tracker.onActivityStopped(nonAnnotatedActivity, true)
        tracker.onActivityResumed(nonAnnotatedActivity)

        verify(exactly = 1) {
            mockContext.track(any())
        }
    }

    @Test
    fun contextData_ManualTracking_AddsDataToDispatch() {
        val tracker = ActivityAutoTracker(mockContext, AutoTrackingMode.FULL)
        tracker.trackActivity(nonAnnotatedActivity, mapOf("manual_data" to "value"))

        verify {
            mockContext.track(match {
                it["manual_data"] == "value"
            })
        }
    }

    @Test
    fun contextData_ActivityCollector_AddsDataToDispatch() {
        val tracker = ActivityAutoTracker(mockContext, AutoTrackingMode.FULL)
        tracker.onActivityResumed(activityWithDataCollector)

        verify {
            mockContext.track(match {
                it["activity_data"] == "value"
            })
        }
    }

    @Test
    fun contextData_GlobalCollector_AddsDataToDispatch() {
        every { mockDataCollector.onCollectActivityData(any()) } returns mapOf("global_data" to "value")
        val tracker = ActivityAutoTracker(mockContext, AutoTrackingMode.FULL, mockDataCollector)
        tracker.onActivityResumed(nonAnnotatedActivity)

        verify {
            mockContext.track(match {
                it["global_data"] == "value"
            })
        }
    }

    @Test
    fun contextData_MultiCollector_AddsDataToDispatch() {
        every { mockDataCollector.onCollectActivityData(any()) } returns mapOf("global_data" to "value")
        val tracker = ActivityAutoTracker(mockContext, AutoTrackingMode.FULL, mockDataCollector)
        tracker.trackActivity(activityWithDataCollector as ActivityDataCollector, mapOf("manual_data" to "value"))

        verify {
            mockContext.track(match {
                it["manual_data"] == "value" &&
                        it["activity_data"] == "value" &&
                        it["global_data"] == "value"
            })
        }
    }

    @Test
    fun manualTracking_Full_TrackSetToFalseIsIgnored() {
        val tracker = ActivityAutoTracker(mockContext, AutoTrackingMode.FULL)
        tracker.trackActivity(annotatedActivityWithoutTracking, null)

        verify(exactly = 1) {
            mockContext.track(any())
        }
    }

    @Test
    fun blacklist_StopsAutoTracking() {
        every { mockBlacklist.isBlacklisted("NonAnnotatedActivity") } returns true
        val tracker = ActivityAutoTracker(mockContext, AutoTrackingMode.FULL, blacklist = mockBlacklist)
        tracker.trackActivity(nonAnnotatedActivity, null)

        verify(exactly = 0) {
            mockContext.track(any())
        }
    }
}

private class NonAnnotatedActivity : Activity()

@Autotracked()
private class AnnotatedActivity : Activity()

@Autotracked(name = "overridden")
private class AnnotatedActivityWithOverride : Activity()

@Autotracked(track = false)
private class AnnotatedActivityWithoutTracking : Activity()

private class ActivityWithDataCollector : Activity(), ActivityDataCollector {
    override fun onCollectActivityData(activityName: String): Map<String, Any>? {
        return mapOf("activity_data" to "value")
    }
}