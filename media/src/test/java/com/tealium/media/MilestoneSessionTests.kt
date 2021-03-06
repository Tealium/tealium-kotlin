package com.tealium.media

import com.tealium.core.TealiumContext
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MilestoneSessionTests {
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
    fun testMilestoneSession_SessionStart() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.MILESTONE)

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        verify { mockMediaSessionDispatcher.track(any(), any(), any()) }
    }

    @Test
    fun testMilestoneSession_MilestoneSent() = runBlocking {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.MILESTONE,
                duration = 100)

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        // wait 10 seconds
        delay(10000)

        // should not record milestone, no play event to start content
        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any())
        }
    }

    @Test
    fun testMilestoneSession_MilestonePlayPauseSuccess() = runBlocking {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.MILESTONE,
                duration = 100)

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)
        media.play()

        // wait 10 seconds - should record Milestone.Ten in mediaContent
        delay(10000)

        media.pause()
        media.endSession()

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any())
            mockMediaSessionDispatcher.track(MediaEvent.PLAY, any())
            mockMediaSessionDispatcher.track(MediaEvent.MILESTONE, any())
            mockMediaSessionDispatcher.track(MediaEvent.PAUSE, any())
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_END, any())
        }
    }
}