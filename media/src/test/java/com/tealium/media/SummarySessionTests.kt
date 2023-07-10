package com.tealium.media

import com.tealium.core.TealiumContext
import com.tealium.media.segments.Ad
import com.tealium.media.segments.AdBreak
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SummarySessionTests {

    @RelaxedMockK
    lateinit var mockContext: TealiumContext

    @RelaxedMockK
    lateinit var mockMediaSessionDispatcher: MediaSessionDispatcher

    private lateinit var mediaContent: MediaContent
    private lateinit var ad: Ad
    private lateinit var adBreak: AdBreak

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        ad = mockk()
        every { ad.uuid } returns "1"

        adBreak = mockk()
        every { adBreak.start() } just Runs
    }

    @Test
    fun testSummarySession_SessionStart() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SUMMARY
        )

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        verify { mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any(), any()) }

        Assert.assertNotNull(mediaContent.summary)
        Assert.assertNotNull(mediaContent.summary?.sessionStartTime)
    }

    @Test
    fun testSummarySession_PlayIncrement() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SUMMARY
        )

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)
        media.play()

        Assert.assertEquals(1, mediaContent.summary?.plays)
    }

    @Test
    fun testSummarySession_PauseIncrement() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SUMMARY
        )

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)
        media.play()
        media.pause()

        Assert.assertEquals(1, mediaContent.summary?.plays)
        Assert.assertEquals(1, mediaContent.summary?.pauses)
    }

    @Test
    fun testSummarySession_StopIncrement() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SUMMARY
        )

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)
        media.play()

        Assert.assertEquals(1, mediaContent.summary?.plays)
    }

    @Test
    fun testSummarySession_StartChapterIncrement() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SUMMARY
        )

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)
        media.play()
        media.startChapter(mockk())

        Assert.assertEquals(1, mediaContent.summary?.plays)
        Assert.assertEquals(1, mediaContent.summary?.chapterStarts)
    }

    @Test
    fun testSummarySession_SkipChapterIncrement() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SUMMARY
        )

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)
        media.play()
        media.startChapter(mockk())
        media.skipChapter()

        Assert.assertEquals(1, mediaContent.summary?.plays)
        Assert.assertEquals(1, mediaContent.summary?.chapterStarts)
        Assert.assertEquals(1, mediaContent.summary?.chapterSkips)
    }

    @Test
    fun testSummarySession_EndChapterIncrement() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SUMMARY
        )

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)
        media.play()
        media.startChapter(mockk())
        media.endChapter()

        Assert.assertEquals(1, mediaContent.summary?.plays)
        Assert.assertEquals(1, mediaContent.summary?.chapterStarts)
        Assert.assertEquals(1, mediaContent.summary?.chapterEnds)
    }

    @Test
    fun testSummarySession_StartAdIncrement() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SUMMARY
        )

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)
        media.play()
        media.startAd(ad)

        Assert.assertEquals(1, mediaContent.summary?.plays)
        Assert.assertEquals(1, mediaContent.summary?.ads)
        Assert.assertEquals(1, mediaContent.summary?.adUuids?.size)
    }

    @Test
    fun testSummarySession_SkipAdIncrement() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SUMMARY
        )

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)
        media.play()
        media.startAd(ad)
        media.skipAd()

        Assert.assertEquals(1, mediaContent.summary?.adSkips)
    }

    @Test
    fun testSummarySession_IncrementTotalAdTime_OnAdSkip() = runBlocking {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SUMMARY
        )

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)
        media.play()

        Assert.assertEquals(0.0, mediaContent.summary?.totalAdTime)

        media.startAd(ad)
        delay(500)
        media.skipAd()

        Assert.assertTrue(mediaContent.summary?.totalAdTime!! > 0.0)
    }

    @Test
    fun testSummarySession_IncrementTotalAdTime_OnEndAd() = runBlocking {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SUMMARY
        )

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)
        media.play()

        Assert.assertEquals(0.0, mediaContent.summary?.totalAdTime)

        media.startAd(ad)
        delay(500)
        media.endAd()

        Assert.assertTrue(mediaContent.summary?.totalAdTime!! > 0)
    }

    @Test
    fun testSummarySession_EndAdIncrement() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SUMMARY
        )

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)
        media.play()
        media.startAd(ad)
        media.endAd()

        Assert.assertEquals(1, mediaContent.summary?.adEnds)
    }

    @Test
    fun testSummarySession_StartBuffer_CaptureStartTime() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SUMMARY
        )

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        Assert.assertEquals(0.0, mediaContent.summary?.totalBufferTime)

        media.startBuffer()

        Assert.assertNotNull(mediaContent.summary?.bufferStartTime)
    }

    @Test
    fun testSummarySession_IncrementTotalBufferTime() = runBlocking {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SUMMARY
        )

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        Assert.assertEquals(0.0, mediaContent.summary?.totalBufferTime)

        media.startBuffer()
        delay(1000)
        media.endBuffer()

        Assert.assertTrue(mediaContent.summary?.totalBufferTime!! > 0.0)
    }

    @Test
    fun testSummarySession_StartSeek_CaptureStartTime() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SUMMARY
        )

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        Assert.assertEquals(0.0, mediaContent.summary?.totalBufferTime)

        media.startSeek(0)

        Assert.assertNotNull(mediaContent.summary?.seekStartTime)
    }

    @Test
    fun testSummarySession_IncrementTotalSeekTime() = runBlocking {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SUMMARY
        )

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        Assert.assertEquals(0.0, mediaContent.summary?.totalBufferTime)

        media.startSeek(0)
        delay(1000)
        media.endSeek(0)

        Assert.assertTrue(mediaContent.summary?.totalSeekTime!! > 0.0)
    }

    @Test
    fun testSummarySession_FinalizeSummary_OnSessionEnd() = runBlocking {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.SUMMARY
        )

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)
        media.play()
        media.startAdBreak(adBreak)

        media.startBuffer()
        delay(1000)
        media.endBuffer()

        media.startChapter(mockk())
        media.pause()
        media.play()

        // Ad count: 2
        media.startAd(ad)
        media.skipAd()
        media.startAd(ad)
        media.endAd()

        media.startSeek(0)
        delay(1000)
        media.endSeek(0)

        media.endChapter()
        media.endContent()

        Assert.assertNotNull(mediaContent.summary?.sessionStartTime)

        Assert.assertEquals(2, mediaContent.summary?.plays)
        Assert.assertEquals(1, mediaContent.summary?.pauses)
        Assert.assertEquals(2, mediaContent.summary?.ads)
        Assert.assertEquals(1, mediaContent.summary?.adSkips)
        Assert.assertEquals(2, mediaContent.summary?.adEnds)
        Assert.assertEquals(1, mediaContent.summary?.chapterStarts)
        Assert.assertEquals(1, mediaContent.summary?.chapterEnds)


        Assert.assertTrue(mediaContent.summary?.totalBufferTime!! > 0.0)
        Assert.assertTrue(mediaContent.summary?.totalAdTime!! > 0.0)
        Assert.assertTrue(mediaContent.summary?.totalSeekTime!! > 0.0)
        Assert.assertEquals(1, mediaContent.summary?.chapterEnds)

        media.endSession()

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_END, mediaContent)
        }
    }
}