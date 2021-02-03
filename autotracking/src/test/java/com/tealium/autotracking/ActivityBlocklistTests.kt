package com.tealium.autotracking

import android.content.SharedPreferences
import com.tealium.autotracking.internal.ActivityBlocklist
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
import org.robolectric.annotation.Config
import java.net.URL

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21, 28])
class ActivityBlocklistTests {

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
        every { mockConfig.autoTrackingBlocklistUrl } returns null
        every { mockConfig.autoTrackingBlocklistFilename } returns null

        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockConnectivity.isConnected() } returns true

        mockkObject(ActivityBlocklist)
        every { ActivityBlocklist.getSharedPreferencesName(any()) } returns ""
        every { ActivityBlocklist.getSharedPreferencesName(any()) } returns ""
    }

    @Test
    fun noBlocklist_IsBlocklisted_AlwaysReturnsFalse() {
        val activityBlocklist = ActivityBlocklist(mockConfig, mockLoader, mockSharedPreferences, mockConnectivity)

        assertFalse(activityBlocklist.isBlocklisted("TestActivity"))
        assertFalse(activityBlocklist.isBlocklisted("testactivity"))
        assertFalse(activityBlocklist.isBlocklisted("Test"))
        assertFalse(activityBlocklist.isBlocklisted("Activity"))
        verify {
            mockLoader wasNot Called
        }
    }

    @Test
    fun loadFromAsset_WhenFileNameSupplied() {
        val filename = "blocklist.json"
        every { mockConfig.autoTrackingBlocklistFilename } returns filename
        every { mockLoader.loadFromAsset(filename) } returns """
            ["Test"]
        """.trimIndent()
        val activityBlocklist = ActivityBlocklist(mockConfig, mockLoader, mockSharedPreferences, mockConnectivity)

        assertTrue(activityBlocklist.isBlocklisted("TestActivity"))
        assertTrue(activityBlocklist.isBlocklisted("testactivity"))
        assertTrue(activityBlocklist.isBlocklisted("Test"))

        assertFalse(activityBlocklist.isBlocklisted("OtherActivity"))

        verify {
            mockLoader.loadFromAsset(filename)
        }
        verify(exactly = 0) {
            mockLoader.loadFromUrl(any())
        }
    }

    @Test
    fun loadFromUrl_WhenUrlSupplied() = runBlocking {
        val url = "http://tags.tiqcdn.com/blocklist.json"
        every { mockConfig.autoTrackingBlocklistUrl } returns url
        every { mockLoader.loadFromUrl(any()) } returns JSONArray("[\"Test\"]")

        val activityBlocklist = ActivityBlocklist(mockConfig, mockLoader, mockSharedPreferences, mockConnectivity)
        delay(500)

        assertTrue(activityBlocklist.isBlocklisted("TestActivity"))
        assertTrue(activityBlocklist.isBlocklisted("testactivity"))
        assertTrue(activityBlocklist.isBlocklisted("Test"))

        assertFalse(activityBlocklist.isBlocklisted("OtherActivity"))

        verify {
            mockLoader.loadFromUrl(URL(url))
        }
        verify(exactly = 0) {
            mockLoader.loadFromAsset(any())
        }
    }

    @Test
    fun loadFromUrl_TriesCacheFirst_WhenUrlSupplied() = runBlocking {
        val url = "http://tags.tiqcdn.com/blocklist.json"
        every { mockConfig.autoTrackingBlocklistUrl } returns url
        every { mockLoader.loadFromUrl(any()) } returns null // wont update cached blocklist
        every { mockSharedPreferences.getStringSet("cached_blocklist", any()) } returns setOf("test")

        val activityBlocklist = ActivityBlocklist(mockConfig, mockLoader, mockSharedPreferences, mockConnectivity)
        delay(500) // fetch from url is on separate thread.

        assertTrue(activityBlocklist.isBlocklisted("TestActivity"))
        assertTrue(activityBlocklist.isBlocklisted("testactivity"))
        assertTrue(activityBlocklist.isBlocklisted("Test"))

        assertFalse(activityBlocklist.isBlocklisted("OtherActivity"))

        verify {
            mockSharedPreferences.getStringSet("cached_blocklist", any())
            mockLoader.loadFromUrl(URL(url))
        }
    }

    @Test
    fun loadFromUrl_TriesCacheFirst_AndUpdatesFromUrl_WhenUrlSupplied() = runBlocking {
        val url = "http://tags.tiqcdn.com/blocklist.json"
        every { mockConfig.autoTrackingBlocklistUrl } returns url
        every { mockLoader.loadFromUrl(any()) } coAnswers {
            delay(500)
            JSONArray("[\"Test\"]")
        }
        every { mockSharedPreferences.getStringSet("cached_blocklist", any()) } returns setOf("before")
        every { mockSharedPreferences.getLong("cached_blocklist_next_refresh", any()) } returns -1

        val activityBlocklist = ActivityBlocklist(mockConfig, mockLoader, mockSharedPreferences, mockConnectivity)
        assertTrue(activityBlocklist.isBlocklisted("BeforeActivity"))
        assertFalse(activityBlocklist.isBlocklisted("TestActivity"))

        delay(1000) // fetch from url is on separate thread.

        assertFalse(activityBlocklist.isBlocklisted("BeforeActivity"))
        assertTrue(activityBlocklist.isBlocklisted("TestActivity"))
    }

    @Test
    fun loadFromUrl_WontFetch_WhenRefreshNotPassed() = runBlocking {
        val url = "http://tags.tiqcdn.com/blocklist.json"
        every { mockConfig.autoTrackingBlocklistUrl } returns url
        every { mockLoader.loadFromUrl(any()) } returns JSONArray("[\"Test\"]")

        every { mockSharedPreferences.getStringSet("cached_blocklist", any()) } returns setOf("before")
        every { mockSharedPreferences.getLong("cached_blocklist_next_refresh", any()) } returns Long.MAX_VALUE

        val activityBlocklist = ActivityBlocklist(mockConfig, mockLoader, mockSharedPreferences, mockConnectivity)
        assertTrue(activityBlocklist.isBlocklisted("BeforeActivity"))
        assertFalse(activityBlocklist.isBlocklisted("TestActivity"))

        verify(exactly = 0, timeout = 500) {
            mockLoader.loadFromUrl(any())
        }
    }

    @Test
    fun loadFromUrl_WontFetch_MoreThanOnce() = runBlocking {
        val url = "http://tags.tiqcdn.com/blocklist.json"
        every { mockConfig.autoTrackingBlocklistUrl } returns url
        every { mockLoader.loadFromUrl(any()) } returns JSONArray("[\"Test\"]")

        every { mockSharedPreferences.getStringSet("cached_blocklist", any()) } returns setOf("before")
        every { mockSharedPreferences.getLong("cached_blocklist_next_refresh", any()) } returnsMany listOf(Long.MIN_VALUE, Long.MAX_VALUE)

        val activityBlocklist = ActivityBlocklist(mockConfig, mockLoader, mockSharedPreferences, mockConnectivity)
        activityBlocklist.isBlocklisted("BeforeActivity")
        activityBlocklist.isBlocklisted("BeforeActivity")
        activityBlocklist.isBlocklisted("BeforeActivity")

        verify(exactly = 1, timeout = 1000) {
            mockLoader.loadFromUrl(any())
        }
    }
}