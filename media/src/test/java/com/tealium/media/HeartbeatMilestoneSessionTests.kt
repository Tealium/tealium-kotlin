package com.tealium.media

import com.tealium.core.TealiumContext
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HeartbeatMilestoneSessionTests {

    @RelaxedMockK
    lateinit var mockContext: TealiumContext

    @RelaxedMockK
    lateinit var mockMediaSessionDispatcher: MediaSessionDispatcher

    private lateinit var mediaContent: MediaContent

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun testHeartbeatMilestone_SessionStart() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.HEARTBEAT_MILESTONE,
                duration = 100)

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        verify { mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any(), any()) }
    }

    @Test
    fun testHeartbeatMilestone_MilestoneSent() = runBlocking {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.HEARTBEAT_MILESTONE,
                duration = 100
        )

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)
        media.play()

        // wait 25 seconds - should record Heartbeat & Milestone
        delay(25000)

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any())
            // 10 sec. ping
            mockMediaSessionDispatcher.track(MediaEvent.HEARTBEAT, any())
            // Milestone.TEN
            mockMediaSessionDispatcher.track(MediaEvent.MILESTONE, any())
            // 20 sec. ping
            mockMediaSessionDispatcher.track(MediaEvent.HEARTBEAT, any())
            // Milestone.TWENTY_FIVE
            mockMediaSessionDispatcher.track(MediaEvent.MILESTONE, any())

        }
    }
}