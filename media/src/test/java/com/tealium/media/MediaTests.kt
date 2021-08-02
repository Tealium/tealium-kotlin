package com.tealium.media

import android.app.Activity
import android.app.Application
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.media.segments.Ad
import com.tealium.media.segments.AdBreak
import com.tealium.media.segments.Chapter
import com.tealium.media.sessions.IntervalMilestoneSession
import com.tealium.media.sessions.IntervalSession
import com.tealium.media.sessions.MilestoneSession
import com.tealium.media.sessions.FullPlaybackSession
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assert.assertEquals
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
    fun testMedia_StartSessionWithFullPlaybackEventSession() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.FULL_PLAYBACK)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        Assert.assertTrue(media.currentSession is FullPlaybackSession)
    }

    @Test
    fun testMedia_StartSessionWithIntervalSession() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.INTERVAL)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        Assert.assertTrue(media.currentSession is IntervalSession)
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
    fun testMedia_StartSessionWithIntervalMilestoneSession() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.INTERVAL_MILESTONE)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        Assert.assertTrue(media.currentSession is IntervalMilestoneSession)
    }

    @Test
    fun testMedia_AdBreakStartSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.FULL_PLAYBACK)

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
    fun testMedia_AdBreakMetadataTrackSuccess() {
        mediaContent = MediaContent("test_media",
            StreamType.PODCAST,
            MediaType.AUDIO,
            mockQoE,
            TrackingType.FULL_PLAYBACK)

        val media = Media(mockTealiumContext)
        media.startSession(mediaContent)

        val adBreak = AdBreak(metadata = mapOf("adBreak_key1" to "value1", "adBreak_key2" to "value2"))

        media.startAdBreak(adBreak)

        verify {
            mockTealiumContext.track(match {
                it["tealium_event"] == MediaEvent.SESSION_START
            })
            mockTealiumContext.track(match {
                it["tealium_event"] == MediaEvent.ADBREAK_START
                it["adBreak_key1"] == "value1"
                it["adBreak_key2"] == "value2"
            })
        }
    }

    @Test
    fun testMedia_AdBreakEndSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.FULL_PLAYBACK)

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
                TrackingType.FULL_PLAYBACK)

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
                TrackingType.FULL_PLAYBACK)

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
    fun testMedia_AdMetadataTrackSuccess() {
        mediaContent = MediaContent("test_media",
            StreamType.PODCAST,
            MediaType.AUDIO,
            mockQoE,
            TrackingType.FULL_PLAYBACK)

        val media = Media(mockTealiumContext)
        media.startSession(mediaContent)

        val ad = Ad(metadata = mapOf("ad_key1" to "value1", "ad_key2" to "value2"))

        media.startAd(ad)

        verify {
            mockTealiumContext.track(match {
                it["tealium_event"] == MediaEvent.SESSION_START
            })
            mockTealiumContext.track(match {
                it["tealium_event"] == MediaEvent.AD_START
                it["ad_key1"] == "value1"
                it["ad_key2"] == "value2"
            })
        }
    }

    @Test
    fun testMedia_AdEndSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.FULL_PLAYBACK)

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
                TrackingType.FULL_PLAYBACK)

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
                TrackingType.FULL_PLAYBACK)

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
                TrackingType.FULL_PLAYBACK)

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
                TrackingType.FULL_PLAYBACK)

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
                TrackingType.FULL_PLAYBACK)

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
                TrackingType.FULL_PLAYBACK)

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
    fun testMedia_ChapterMetadataTrackSuccess() {
        mediaContent = MediaContent("test_media",
            StreamType.PODCAST,
            MediaType.AUDIO,
            mockQoE,
            TrackingType.FULL_PLAYBACK)

        val media = Media(mockTealiumContext)
        media.startSession(mediaContent)

        val chapter = Chapter("testChapter", metadata = mapOf("chapter_key1" to "value1", "chapter_key2" to "value2"))

        media.startChapter(chapter)

        verify {
            mockTealiumContext.track(match {
                it["tealium_event"] == MediaEvent.SESSION_START
            })
            mockTealiumContext.track(match {
                it["tealium_event"] == MediaEvent.CHAPTER_START
                it["chapter_key1"] == "value1"
                it["chapter_key2"] == "value2"
            })
        }
    }

    @Test
    fun testMedia_ChapterEndSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.FULL_PLAYBACK)

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
                TrackingType.FULL_PLAYBACK)

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
                TrackingType.FULL_PLAYBACK)

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
                TrackingType.FULL_PLAYBACK)

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
                TrackingType.FULL_PLAYBACK)

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
                TrackingType.FULL_PLAYBACK)

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
                TrackingType.FULL_PLAYBACK)

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
                TrackingType.FULL_PLAYBACK)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)

        media.startBuffer()
        media.endChapter()

        verify { mockMediaSessionDispatcher wasNot called }
    }

    @Test
    fun testMedia_ValidPlayEventWithData() {
        mediaContent = MediaContent("test_media",
            mockk(),
            mockk(),
            mockk(),
            TrackingType.FULL_PLAYBACK)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        val testData = mapOf(
            "play_key1" to "value1",
            "play_key2" to "value2",
            "play_key3" to "value3"
        )

        media.play(testData)

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.PLAY, mediaContent, customData = match {
                it.containsKey("play_key1")
                it.containsKey("play_key2")
                it.containsKey("play_key3")
            })
        }
    }

    @Test
    fun testMedia_PlayPauseStopSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.FULL_PLAYBACK)

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
    fun testMedia_ValidPauseEventWithData() {
        mediaContent = MediaContent("test_media",
            mockk(),
            mockk(),
            mockk(),
            TrackingType.FULL_PLAYBACK)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        val testData = mapOf(
            "pause_key1" to "value1",
            "pause_key2" to "value2",
            "pause_key3" to "value3"
        )

        media.play()
        media.pause(testData)

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.PAUSE, mediaContent, customData = match {
                it.containsKey("pause_key1")
                it.containsKey("pause_key2")
                it.containsKey("pause_key3")
            })
        }
    }

    @Test
    fun testMedia_PlayPauseStopFail_NoSessionStarted() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.FULL_PLAYBACK)

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
                TrackingType.FULL_PLAYBACK)

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
    fun testMedia_ValidSeekStartEventWithData() {
        mediaContent = MediaContent("test_media",
            mockk(),
            mockk(),
            mockk(),
            TrackingType.FULL_PLAYBACK)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        val testData = mapOf(
            "seek_start_key1" to "value1",
            "seek_start_key2" to "value2",
            "seek_start_key3" to "value3"
        )

        media.startSeek(10, testData)

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SEEK_START, mediaContent, customData = match {
                it.containsKey("seek_start_key1")
                it.containsKey("seek_start_key2")
                it.containsKey("seek_start_key3")
            })
        }
    }

    @Test
    fun testMedia_ValidSeekEndEventWithData() {
        mediaContent = MediaContent("test_media",
            mockk(),
            mockk(),
            mockk(),
            TrackingType.FULL_PLAYBACK)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        val testData = mapOf(
            "seek_end_key1" to "value1",
            "seek_end_key2" to "value2",
            "seek_end_key3" to "value3"
        )

        media.startSeek(10, testData)

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SEEK_START, mediaContent, customData = match {
                it.containsKey("seek_end_key1")
                it.containsKey("seek_end_key2")
                it.containsKey("seek_end_key3")
            })
        }
    }

    @Test
    fun testMedia_SeekFail_NoSessionStarted() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.FULL_PLAYBACK)

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
                TrackingType.FULL_PLAYBACK)

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
                TrackingType.FULL_PLAYBACK)

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
                TrackingType.FULL_PLAYBACK)

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
                TrackingType.FULL_PLAYBACK)

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
                TrackingType.FULL_PLAYBACK,
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
                TrackingType.FULL_PLAYBACK,
                PlayerState.FULLSCREEN)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)

        media.startSession(mediaContent)

        assertEquals(false, media.currentSession?.isBackgrounded)
    }

    @Test
    fun testMedia_BackgroundTrackingDisabled_isBackgroundedDefaultSuccess() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockQoE,
                TrackingType.FULL_PLAYBACK,
                PlayerState.FULLSCREEN)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)

        media.startSession(mediaContent)

        assertEquals(false, media.currentSession?.isBackgrounded)

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
                TrackingType.FULL_PLAYBACK,
                PlayerState.FULLSCREEN)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)

        media.startSession(mediaContent)

        assertEquals(false, media.currentSession?.isBackgrounded)

        media.onActivityResumed()
        media.onActivityStopped(testActivity, false)

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
                TrackingType.FULL_PLAYBACK,
                PlayerState.FULLSCREEN)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)

        media.startSession(mediaContent)

        assertEquals(false, media.currentSession?.isBackgrounded)

        media.onActivityResumed()
        media.onActivityStopped(testActivity, false)

        assertEquals(true, media.currentSession?.isBackgrounded)

        delay(9000)
        media.onActivityResumed(testActivity)

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any())
            media.currentSession?.isBackgrounded = false
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_RESUME, any())
        }
    }


    // TODO better name?
    @Test
    fun testMedia_BackgroundTrackingEnabled_onActivityStopped_isBackgroundedFalse() {
        config.mediaBackgroundSessionEnabled = true
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockQoE,
                TrackingType.FULL_PLAYBACK,
                PlayerState.FULLSCREEN)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)

        media.startSession(mediaContent)
        media.onActivityStopped(testActivity, false)

        assertEquals(false, media.currentSession?.isBackgrounded)

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
                TrackingType.FULL_PLAYBACK,
                PlayerState.FULLSCREEN)

        val media = Media(mockTealiumContext, mockMediaSessionDispatcher)

        media.startSession(mediaContent)
        media.onActivityStopped(testActivity, false)
        media.onActivityResumed(testActivity)

        assertEquals(false, media.currentSession?.isBackgrounded)

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any())
        }

        verify (exactly = 0) {
            media.resumeSession()
        }
    }

    @Test
    fun timeMillis() {
        assertEquals(10.555, Media.timeMillisToSeconds(10555), 0.0)
    }
}

private class TestActivity : Activity()