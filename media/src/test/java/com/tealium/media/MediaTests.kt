package com.tealium.media

import com.tealium.core.TealiumContext
import com.tealium.media.segments.Ad
import com.tealium.media.segments.AdBreak
import com.tealium.media.segments.Chapter
import com.tealium.media.sessions.HeartbeatMilestoneSession
import com.tealium.media.sessions.HeartbeatSession
import com.tealium.media.sessions.MilestoneSession
import com.tealium.media.sessions.SignificantEventsSession
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class MediaTests {

    @RelaxedMockK
    lateinit var mockContext: TealiumContext

    @RelaxedMockK
    lateinit var mockMediaSessionDispatcher: MediaSessionDispatcher

    @RelaxedMockK
    lateinit var mockQoE: QoE

    private lateinit var mediaContent: MediaContent

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun testMedia_StartSessionWithSignificantEventSession() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockContext, mockMediaSessionDispatcher)
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

        val media = Media(mockContext, mockMediaSessionDispatcher)
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

        val media = Media(mockContext, mockMediaSessionDispatcher)
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

        val media = Media(mockContext, mockMediaSessionDispatcher)
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

        val media = Media(mockContext, mockMediaSessionDispatcher)
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

        val media = Media(mockContext, mockMediaSessionDispatcher)
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

        val media = Media(mockContext, mockMediaSessionDispatcher)
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

        val media = Media(mockContext, mockMediaSessionDispatcher)
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

        val media = Media(mockContext, mockMediaSessionDispatcher)
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

        val media = Media(mockContext, mockMediaSessionDispatcher)
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

        val media = Media(mockContext, mockMediaSessionDispatcher)
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

        val media = Media(mockContext, mockMediaSessionDispatcher)
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

        val media = Media(mockContext, mockMediaSessionDispatcher)
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

        val media = Media(mockContext, mockMediaSessionDispatcher)
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

        val media = Media(mockContext, mockMediaSessionDispatcher)
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

        val media = Media(mockContext, mockMediaSessionDispatcher)
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

        val media = Media(mockContext, mockMediaSessionDispatcher)
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

        val media = Media(mockContext, mockMediaSessionDispatcher)
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

        val media = Media(mockContext, mockMediaSessionDispatcher)
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

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        media.startBuffer()

        verify { mockMediaSessionDispatcher.track(any(), any(), any()) }
    }

    @Test
    fun testMedia_StartBufferFail_NoSessionStarted() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockContext, mockMediaSessionDispatcher)

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

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        media.startBuffer()
        media.endBuffer()

        verify(exactly = 3) { mockMediaSessionDispatcher.track(any(), any(), any()) }
    }

    @Test
    fun testMedia_EndBufferFail_NoSessionStarted() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockContext, mockMediaSessionDispatcher)

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

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        media.play()
        media.pause()
        media.stop()

        verify(exactly = 4) { mockMediaSessionDispatcher.track(any(), any(), any()) }
    }

    @Test
    fun testMedia_PlayPauseStopFail_NoSessionStarted() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockContext, mockMediaSessionDispatcher)

        media.play()
        media.pause()
        media.stop()

        verify { mockMediaSessionDispatcher wasNot Called }
    }

    @Test
    fun testMedia_SeekSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        media.startSeek()

        // 2 track for startSession + startSeek
        verify(exactly = 2) { mockMediaSessionDispatcher.track(any(), any(), any()) }

        media.endSeek()

        // 3 track for startSession + startSeek + endSeek
        verify(exactly = 3) { mockMediaSessionDispatcher.track(any(), any(), any()) }
    }

    @Test
    fun testMedia_SeekFail_NoSessionStarted() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SIGNIFICANT)

        val media = Media(mockContext, mockMediaSessionDispatcher)

        media.startSeek()
        media.endSeek()

        verify { mockMediaSessionDispatcher wasNot Called }
    }

    @Test
    fun testMedia_UpdateBitrateSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockQoE,
                TrackingType.SIGNIFICANT)

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        media.updateBitrate(4)

        verify {
            mockQoE.bitrate = 4
            mockMediaSessionDispatcher.track(any(), any(), any())
        }
    }

    @Test
    fun testMedia_UpdateDroppedFramesSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockQoE,
                TrackingType.SIGNIFICANT)

        val media = Media(mockContext, mockMediaSessionDispatcher)
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

        val media = Media(mockContext, mockMediaSessionDispatcher)
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

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        media.updatePlayerState(PlayerState.MUTE)

        verify {
            mediaContent.state = PlayerState.MUTE
            mockMediaSessionDispatcher.track(any(), any(), any())
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

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        media.updatePlayerState(PlayerState.MUTE)

        verify {
            mockMediaSessionDispatcher.track(any(), any(), any())
            mediaContent.state = PlayerState.MUTE
            mockMediaSessionDispatcher.track(any(), any(), any())
        }
    }
}