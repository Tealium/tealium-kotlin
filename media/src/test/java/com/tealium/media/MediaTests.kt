package com.tealium.media

import android.app.Activity
import android.app.Application
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.media.segments.Ad
import com.tealium.media.segments.AdBreak
import com.tealium.media.segments.Chapter
import com.tealium.media.sessions.HeartbeatMilestoneSession
import com.tealium.media.sessions.HeartbeatSession
import com.tealium.media.sessions.MilestoneSession
import com.tealium.media.sessions.SignificantEventsSession
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class MediaTests {

    @MockK
    lateinit var context: Application

    @MockK
    lateinit var mockFile: File

    @RelaxedMockK
    lateinit var mockTealiumContext: TealiumContext

    @RelaxedMockK
    lateinit var mockMediaSessionDispatcher: MediaSessionDispatcher

    @RelaxedMockK
    lateinit var mockQoE: QoE

    private lateinit var mediaContent: MediaContent
    private lateinit var config: TealiumConfig
    private var testActivity = TestActivity()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { context.filesDir } returns mockFile

        config = TealiumConfig(context, "test", "profile", Environment.QA)
        every { mockTealiumContext.config } returns config
    }

    @Test
    fun testMedia_StartSessionWithSignificantEventSession() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        Assert.assertTrue(media.currentSession is SignificantEventsSession)
    }

    @Test
    fun testMedia_StartSessionWithHeartbeatSession() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.HEARTBEAT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        Assert.assertTrue(media.currentSession is HeartbeatSession)
    }

    @Test
    fun testMedia_StartSessionWithMilestoneSession() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.MILESTONE)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        Assert.assertTrue(media.currentSession is MilestoneSession)
    }

    @Test
    fun testMedia_StartSessionWithHeartbeatMilestoneSession() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.HEARTBEAT_MILESTONE)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        Assert.assertTrue(media.currentSession is HeartbeatMilestoneSession)
    }

    @Test
    fun testMedia_AdBreakStartSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        val adBreak = mockk<AdBreak>()
        every { adBreak.start() } just Runs

        media.startAdBreak(adBreak)

        verify {
            adBreak.start()
        }

        Assert.assertTrue(mediaContent.adBreakList.size == 1)
    }

    @Test
    fun testMedia_AdBreakEndSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        val adBreak = mockk<AdBreak>()
        every { adBreak.start() } just Runs
        every { adBreak.end() } just Runs

        media.startAdBreak(adBreak)
        media.endAdBreak()

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any(), any())
            adBreak.start()
            mockMediaSessionDispatcher.track(MediaEvent.ADBREAK_START, any(), any())
            adBreak.end()
            mockMediaSessionDispatcher.track(MediaEvent.ADBREAK_COMPLETE, any(), any())
        }

        Assert.assertTrue(mediaContent.adBreakList.size == 1)
    }

    @Test
    fun testMedia_AdBreakEndFail_WhenNoAdBreaksAdded() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        val adBreak = mockk<AdBreak>()
        every { adBreak.start() } just Runs
        every { adBreak.end() } just Runs

        media.endAdBreak()

        verify {
            adBreak wasNot Called
        }

        Assert.assertTrue(mediaContent.adBreakList.size == 0)
    }

    @Test
    fun testMedia_AdStartSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        val ad = mockk<Ad>()
        every { ad.start() } just Runs

        media.startAd(ad)

        verify {
            ad.start()
        }

        Assert.assertTrue(mediaContent.adList.size == 1)
    }

    @Test
    fun testMedia_AdEndSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        val ad = mockk<Ad>()
        every { ad.start() } just Runs
        every { ad.end() } just Runs

        media.startAd(ad)
        media.endAd()

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any(), any())
            ad.start()
            mockMediaSessionDispatcher.track(MediaEvent.AD_START, any(), any())
            ad.end()
            mockMediaSessionDispatcher.track(MediaEvent.AD_COMPLETE, any(), any())
        }

        Assert.assertTrue(mediaContent.adList.size == 1)
    }

    @Test
    fun testMedia_AdEndFail_WhenNoAdsAdded() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        val ad = mockk<Ad>()
        every { ad.end() } just Runs

        media.endAd()

        verify {
            ad wasNot Called
        }

        Assert.assertTrue(mediaContent.adList.size == 0)
    }

    @Test
    fun testMedia_AdClickSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        val ad = mockk<Ad>()
        every { ad.start() } just Runs

        media.startAd(ad)
        media.clickAd()

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any(), any())
            ad.start()
            mockMediaSessionDispatcher.track(MediaEvent.AD_START, any(), any())
            mockMediaSessionDispatcher.track(MediaEvent.AD_CLICK, any(), any())
        }

        // similar to end - ad gets removed from adList
        Assert.assertTrue(mediaContent.adList.size == 1)
    }

    @Test
    fun testMedia_AdClickFail_WhenNoAdsAdded() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        media.clickAd()

        Assert.assertTrue(mediaContent.adList.size == 0)
    }

    @Test
    fun testMedia_AdSkipSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        val ad = mockk<Ad>()
        every { ad.start() } just Runs

        media.startAd(ad)
        media.skipAd()

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any(), any())
            ad.start()
            mockMediaSessionDispatcher.track(MediaEvent.AD_START, any(), any())
            mockMediaSessionDispatcher.track(MediaEvent.AD_SKIP, any(), any())
        }

        // similar to end & click - ad gets removed from adList
        Assert.assertTrue(mediaContent.adList.size == 1)
    }

    @Test
    fun testMedia_AdSkipFail_WhenNoAdsAdded() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        media.skipAd()

        Assert.assertTrue(mediaContent.adList.size == 0)
    }

    @Test
    fun testMedia_ChapterStartSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        val chapter = mockk<Chapter>()
        every { chapter.start() } just Runs

        media.startChapter(chapter)

        verify {
            chapter.start()
        }

        Assert.assertTrue(mediaContent.chapterList.size == 1)
    }

    @Test
    fun testMedia_ChapterEndSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        val chapter = mockk<Chapter>()
        every { chapter.start() } just Runs
        every { chapter.end() } just Runs

        media.startChapter(chapter)
        media.endChapter()

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any(), any())
            chapter.start()
            mockMediaSessionDispatcher.track(MediaEvent.CHAPTER_START, any(), any())
            chapter.end()
            mockMediaSessionDispatcher.track(MediaEvent.CHAPTER_COMPLETE, any(), any())
        }

        Assert.assertTrue(mediaContent.chapterList.size == 1)
    }

    @Test
    fun testMedia_ChapterEndFail_WhenNoChaptersAdded() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        val chapter = mockk<Chapter>()
        every { chapter.start() } just Runs
        every { chapter.end() } just Runs

        media.endChapter()

        verify {
            chapter wasNot Called
        }

        Assert.assertTrue(mediaContent.chapterList.size == 0)
    }

    @Test
    fun testMedia_ChapterSkipSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        val chapter = mockk<Chapter>()
        every { chapter.start() } just Runs

        media.startChapter(chapter)
        media.skipChapter()

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any(), any())
            chapter.start()
            mockMediaSessionDispatcher.track(MediaEvent.CHAPTER_START, any(), any())
            mockMediaSessionDispatcher.track(MediaEvent.CHAPTER_SKIP, any(), any())
        }

        // similar to end - chapter gets removed from adList
        Assert.assertTrue(mediaContent.chapterList.size == 1)
    }

    @Test
    fun testMedia_ChapterSkipFail_WhenNoAdsAdded() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        media.skipChapter()

        Assert.assertTrue(mediaContent.chapterList.size == 0)
    }

    @Test
    fun testMedia_StartBufferSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        media.startBuffer()

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any(), any())
            mockMediaSessionDispatcher.track(MediaEvent.BUFFER_START, any(), any())
        }
    }

    @Test
    fun testMedia_StartBufferFail_NoSessionStarted() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)

        media.startBuffer()

        verify { mockMediaSessionDispatcher wasNot Called }
    }

    @Test
    fun testMedia_EndBufferSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        media.startBuffer()
        media.endBuffer()

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any(), any())
            mockMediaSessionDispatcher.track(MediaEvent.BUFFER_START, any(), any())
            mockMediaSessionDispatcher.track(MediaEvent.BUFFER_COMPLETE, any(), any())
        }
    }

    @Test
    fun testMedia_EndBufferFail_NoSessionStarted() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)

        media.startBuffer()
        media.endChapter()

        verify { mockMediaSessionDispatcher wasNot called }
    }

    @Test
    fun testMedia_PlayPauseStopSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        media.play()
        media.pause()

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any(), any())
            mockMediaSessionDispatcher.track(MediaEvent.PLAY, any(), any())
            mockMediaSessionDispatcher.track(MediaEvent.PAUSE, any(), any())
        }
    }

    @Test
    fun testMedia_PlayPauseStopFail_NoSessionStarted() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)

        media.play()
        media.pause()

        verify { mockMediaSessionDispatcher wasNot Called }
    }

    @Test
    fun testMedia_SeekSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        media.startSeek(10)
        media.endSeek(25)

        // 2 track for startSession + startSeek
        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any(), any())
            mockMediaSessionDispatcher.track(MediaEvent.SEEK_START, any(), any())
            mockMediaSessionDispatcher.track(MediaEvent.SEEK_COMPLETE, any(), any())
        }
    }

    @Test
    fun testMedia_SeekFail_NoSessionStarted() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)

        media.startSeek(10)
        media.endSeek(15)

        verify { mockMediaSessionDispatcher wasNot Called }
    }

    @Test
    fun testMedia_UpdateBitrateSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockQoE,
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        media.updateBitrate(4)

        verify {
            mockQoE.bitrate = 4
            mockMediaSessionDispatcher.track(MediaEvent.BITRATE_CHANGE, any(), any())
        }
    }

    @Test
    fun testMedia_UpdateDroppedFramesSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockQoE,
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        media.updateDroppedFrames(8)

        verify {
            mockQoE.droppedFrames = 8
        }
    }

    @Test
    fun testMedia_UpdatePlaybackSpeedSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockQoE,
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        media.updatePlaybackSpeed(10.0)

        verify {
            mockQoE.playbackSpeed = 10.0
        }
    }

    @Test
    fun testMedia_UpdatePlayerStateSuccess_FromNull() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockQoE,
                TrackingType.SIGNIFICANT)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        media.updatePlayerState(PlayerState.MUTE)

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any(), any())
            mediaContent.state = PlayerState.MUTE
            mockMediaSessionDispatcher.track(MediaEvent.PLAYER_STATE_START, any(), any())
        }
    }

    @Test
    fun testMedia_UpdatePlayerStateSuccess_FromInitialState() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockQoE,
                TrackingType.SIGNIFICANT,
                PlayerState.FULLSCREEN)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        media.updatePlayerState(PlayerState.MUTE)

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any(), any())
            mockMediaSessionDispatcher.track(MediaEvent.PLAYER_STATE_STOP, any(), any())
            mediaContent.state = PlayerState.MUTE
            mockMediaSessionDispatcher.track(MediaEvent.PLAYER_STATE_START, any(), any())
        }
    }

    @Test
    fun testMedia_sessionIsBackgroundedInitialState() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockQoE,
                TrackingType.SIGNIFICANT,
                PlayerState.FULLSCREEN)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)

        media.startSession(mediaContent)

        Assert.assertEquals(false, media.currentSession?.isBackgrounded)
    }

    @Test
    fun testMedia_BackgroundTrackingDisabled_isBackgroundedDefaultSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockQoE,
                TrackingType.SIGNIFICANT,
                PlayerState.FULLSCREEN)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)

        media.startSession(mediaContent)

        Assert.assertEquals(false, media.currentSession?.isBackgrounded)

        verify(exactly = 1) {
            mockMediaSessionDispatcher.track(any(), any())
        }
    }

    @Test
    fun testMedia_BackgroundTrackingDisabled_EndSessionSuccess() = runBlocking {
        config.mediaBackgroundSessionEndInterval = 10000L //ten seconds
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockQoE,
                TrackingType.SIGNIFICANT,
                PlayerState.FULLSCREEN)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)

        media.startSession(mediaContent)

        Assert.assertEquals(false, media.currentSession?.isBackgrounded)

        media.onActivityPaused(testActivity)

        delay(11000)

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any())
            media.currentSession?.isBackgrounded = true
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_END, any())
        }
    }

    @Test
    fun testMedia_BackgroundTrackingDisabled_ResumeSuccess() = runBlocking {
        config.mediaBackgroundSessionEndInterval = 10000L //ten seconds
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockQoE,
                TrackingType.SIGNIFICANT,
                PlayerState.FULLSCREEN)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)

        media.startSession(mediaContent)

        Assert.assertEquals(false, media.currentSession?.isBackgrounded)

        media.onActivityPaused(testActivity)

        Assert.assertEquals(true, media.currentSession?.isBackgrounded)

        delay(9000)

        media.onActivityResumed(testActivity)

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any())
            media.currentSession?.isBackgrounded = false
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_RESUME, any())
        }
    }


    // need a better name
    @Test
    fun testMedia_BackgroundTrackingEnabled_onActivityPaused_isBackgroundedFalse() {
        config.mediaBackgroundSessionEnabled = true
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockQoE,
                TrackingType.SIGNIFICANT,
                PlayerState.FULLSCREEN)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)

        media.startSession(mediaContent)
        media.onActivityPaused(testActivity)

        Assert.assertEquals(false, media.currentSession?.isBackgrounded)

        verify (exactly = 1) {
            mockMediaSessionDispatcher.track(any(), any())
        }
    }

    @Test
    fun testMedia_BackgroundTrackingEnabled_onActivityResume_NoMediaUpdates() = runBlocking {
        config.mediaBackgroundSessionEnabled = true
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockQoE,
                TrackingType.SIGNIFICANT,
                PlayerState.FULLSCREEN)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)

        media.startSession(mediaContent)
        media.onActivityPaused(testActivity)
        media.onActivityResumed(testActivity)

        Assert.assertEquals(false, media.currentSession?.isBackgrounded)

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any())
        }

        verify (exactly = 0) {
            media.resumeSession()
        }
    }

}

private class TestActivity : Activity()