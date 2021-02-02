package com.tealium.media

import com.tealium.core.TealiumContext
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SummarySessionTests {
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
        Assert.assertNotNull(mediaContent.summary?.sessionStart)
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
        media.stop()

        Assert.assertEquals(1, mediaContent.summary?.plays)
        Assert.assertEquals(1, mediaContent.summary?.stops)
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
        media.startAd(mockk())

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
        media.startAd(mockk())
        media.skipAd()

        Assert.assertEquals(1, mediaContent.summary?.adSkips)
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
        media.startAd(mockk())
        media.endAd()

        Assert.assertEquals(1, mediaContent.summary?.adEnds)
    }

}