package com.tealium.autotracking

import android.content.SharedPreferences
import com.tealium.autotracking.internal.ActivityBlacklist
import com.tealium.core.Loader
import com.tealium.core.TealiumConfig
import com.tealium.core.network.Connectivity
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URL

@RunWith(RobolectricTestRunner::class)
class ActivityBlacklistTests {

    @RelaxedMockK
    lateinit var mockConfig: TealiumConfig

    @RelaxedMockK
    lateinit var mockLoader: Loader

    @RelaxedMockK
    lateinit var mockSharedPreferences: SharedPreferences

    @RelaxedMockK
    lateinit var mockEditor: SharedPreferences.Editor

    @RelaxedMockK
    lateinit var mockConnectivity: Connectivity

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        // Default to neither in use
        every { mockConfig.autoTrackingBlacklistUrl } returns null
        every { mockConfig.autoTrackingBlacklistFilename } returns null

        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockConnectivity.isConnected() } returns true

        mockkObject(ActivityBlacklist)
        every { ActivityBlacklist.getSharedPreferencesName(any()) } returns ""
        every { ActivityBlacklist.getSharedPreferencesName(any()) } returns ""
    }

    @Test
    fun noBlackList_IsBlackListed_AlwaysReturnsFalse() {
        val activityBlacklist = ActivityBlacklist(mockConfig, mockLoader, mockSharedPreferences, mockConnectivity)

        assertFalse(activityBlacklist.isBlacklisted("TestActivity"))
        assertFalse(activityBlacklist.isBlacklisted("testactivity"))
        assertFalse(activityBlacklist.isBlacklisted("Test"))
        assertFalse(activityBlacklist.isBlacklisted("Activity"))
        verify {
            mockLoader wasNot Called
        }
    }

    @Test
    fun loadFromAsset_WhenFileNameSupplied() {
        val filename = "blacklist.json"
        every { mockConfig.autoTrackingBlacklistFilename } returns filename
        every { mockLoader.loadFromAsset(filename) } returns """
            ["Test"]
        """.trimIndent()
        val activityBlacklist = ActivityBlacklist(mockConfig, mockLoader, mockSharedPreferences, mockConnectivity)

        assertTrue(activityBlacklist.isBlacklisted("TestActivity"))
        assertTrue(activityBlacklist.isBlacklisted("testactivity"))
        assertTrue(activityBlacklist.isBlacklisted("Test"))

        assertFalse(activityBlacklist.isBlacklisted("OtherActivity"))

        verify {
            mockLoader.loadFromAsset(filename)
        }
        verify(exactly = 0) {
            mockLoader.loadFromUrl(any())
        }
    }

    @Test
    fun loadFromUrl_WhenUrlSupplied() = runBlocking {
        val url = "http://tags.tiqcdn.com/blacklist.json"
        every { mockConfig.autoTrackingBlacklistUrl } returns url
        every { mockLoader.loadFromUrl(any()) } returns JSONArray("[\"Test\"]")

        val activityBlacklist = ActivityBlacklist(mockConfig, mockLoader, mockSharedPreferences, mockConnectivity)
        delay(500)

        assertTrue(activityBlacklist.isBlacklisted("TestActivity"))
        assertTrue(activityBlacklist.isBlacklisted("testactivity"))
        assertTrue(activityBlacklist.isBlacklisted("Test"))

        assertFalse(activityBlacklist.isBlacklisted("OtherActivity"))

        verify {
            mockLoader.loadFromUrl(URL(url))
        }
        verify(exactly = 0) {
            mockLoader.loadFromAsset(any())
        }
    }

    @Test
    fun loadFromUrl_TriesCacheFirst_WhenUrlSupplied() = runBlocking {
        val url = "http://tags.tiqcdn.com/blacklist.json"
        every { mockConfig.autoTrackingBlacklistUrl } returns url
        every { mockLoader.loadFromUrl(any()) } returns null // wont update cached blacklist
        every { mockSharedPreferences.getStringSet("cached_blacklist", any()) } returns setOf("test")

        val activityBlacklist = ActivityBlacklist(mockConfig, mockLoader, mockSharedPreferences, mockConnectivity)
        delay(500) // fetch from url is on separate thread.

        assertTrue(activityBlacklist.isBlacklisted("TestActivity"))
        assertTrue(activityBlacklist.isBlacklisted("testactivity"))
        assertTrue(activityBlacklist.isBlacklisted("Test"))

        assertFalse(activityBlacklist.isBlacklisted("OtherActivity"))

        verify {
            mockSharedPreferences.getStringSet("cached_blacklist", any())
            mockLoader.loadFromUrl(URL(url))
        }
    }

    @Test
    fun loadFromUrl_TriesCacheFirst_AndUpdatesFromUrl_WhenUrlSupplied() = runBlocking {
        val url = "http://tags.tiqcdn.com/blacklist.json"
        every { mockConfig.autoTrackingBlacklistUrl } returns url
        every { mockLoader.loadFromUrl(any()) } coAnswers {
            delay(500)
            JSONArray("[\"Test\"]")
        }
        every { mockSharedPreferences.getStringSet("cached_blacklist", any()) } returns setOf("before")
        every { mockSharedPreferences.getLong("cached_blacklist_next_refresh", any()) } returns -1

        val activityBlacklist = ActivityBlacklist(mockConfig, mockLoader, mockSharedPreferences, mockConnectivity)
        assertTrue(activityBlacklist.isBlacklisted("BeforeActivity"))
        assertFalse(activityBlacklist.isBlacklisted("TestActivity"))

        delay(1000) // fetch from url is on separate thread.

        assertFalse(activityBlacklist.isBlacklisted("BeforeActivity"))
        assertTrue(activityBlacklist.isBlacklisted("TestActivity"))
    }

    @Test
    fun loadFromUrl_WontFetch_WhenRefreshNotPassed() = runBlocking {
        val url = "http://tags.tiqcdn.com/blacklist.json"
        every { mockConfig.autoTrackingBlacklistUrl } returns url
        every { mockLoader.loadFromUrl(any()) } returns JSONArray("[\"Test\"]")

        every { mockSharedPreferences.getStringSet("cached_blacklist", any()) } returns setOf("before")
        every { mockSharedPreferences.getLong("cached_blacklist_next_refresh", any()) } returns Long.MAX_VALUE

        val activityBlacklist = ActivityBlacklist(mockConfig, mockLoader, mockSharedPreferences, mockConnectivity)
        assertTrue(activityBlacklist.isBlacklisted("BeforeActivity"))
        assertFalse(activityBlacklist.isBlacklisted("TestActivity"))

        verify(exactly = 0, timeout = 500) {
            mockLoader.loadFromUrl(any())
        }
    }

    @Test
    fun loadFromUrl_WontFetch_MoreThanOnce() = runBlocking {
        val url = "http://tags.tiqcdn.com/blacklist.json"
        every { mockConfig.autoTrackingBlacklistUrl } returns url
        every { mockLoader.loadFromUrl(any()) } returns JSONArray("[\"Test\"]")

        every { mockSharedPreferences.getStringSet("cached_blacklist", any()) } returns setOf("before")
        every { mockSharedPreferences.getLong("cached_blacklist_next_refresh", any()) } returnsMany listOf(Long.MIN_VALUE, Long.MAX_VALUE)

        val activityBlacklist = ActivityBlacklist(mockConfig, mockLoader, mockSharedPreferences, mockConnectivity)
        activityBlacklist.isBlacklisted("BeforeActivity")
        activityBlacklist.isBlacklisted("BeforeActivity")
        activityBlacklist.isBlacklisted("BeforeActivity")

        verify(exactly = 1, timeout = 1000) {
            mockLoader.loadFromUrl(any())
        }
    }
}