package com.tealium.tagmanagementdispatcher

import android.app.Application
import android.os.SystemClock
import android.webkit.*
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.messaging.AfterDispatchSendCallbacks
import com.tealium.core.network.Connectivity
import com.tealium.core.network.HttpClient
import com.tealium.core.settings.LibrarySettings
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.*
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class WebViewLoaderTest {

    @MockK
    lateinit var mockContext: Application

    @MockK
    lateinit var mockFile: File

    @RelaxedMockK
    lateinit var mockDispatchSendCallbacks: AfterDispatchSendCallbacks

    @RelaxedMockK
    lateinit var mockSettings: WebSettings

    @RelaxedMockK
    lateinit var mockWebView: WebView

    @RelaxedMockK
    lateinit var mockTealiumContext: TealiumContext

    @RelaxedMockK
    lateinit var mockTealiumConfig: TealiumConfig

    @RelaxedMockK
    lateinit var mockHttpClient: HttpClient

    @RelaxedMockK
    lateinit var mockConnectivity: Connectivity

    lateinit var webViewLoader: WebViewLoader
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(mainThreadSurrogate)

        every { mockContext.filesDir } returns mockFile

        every { mockTealiumConfig.application } returns mockContext
        every { mockTealiumConfig.accountName } returns "test"
        every { mockTealiumConfig.profileName } returns "profile"
        every { mockContext.applicationContext } returns mockContext
        every { mockTealiumContext.config } returns mockTealiumConfig
        every { mockTealiumContext.httpClient } returns mockHttpClient

        mockkConstructor(WebView::class)
        every {
            anyConstructed<WebView>().settings
        } returns mockSettings
        every {
            mockSettings.databaseEnabled = any()
            mockSettings.javaScriptEnabled = any()
            mockSettings.domStorageEnabled = any()
            mockSettings.setAppCacheEnabled(any())
            mockSettings.setAppCachePath(any())
            anyConstructed<WebView>().loadUrl(any())
        } just Runs

        every { mockConnectivity.isConnected() } returns true

        mockkStatic(CookieManager::class)
        every { CookieManager.getInstance() } returns mockk<CookieManager>()
        every { CookieManager.getInstance().setAcceptCookie(any()) } just Runs
        every { CookieManager.getInstance().setAcceptThirdPartyCookies(any(), any()) } just Runs

        mockkStatic(SystemClock::class)
    }

    @Test
    fun loadUrlToWebView_OnlyExecutesOnce() = runBlocking {
        every { mockConnectivity.isConnected() } returns false // cancels the initial load
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity)
        delay(50)
        webViewLoader.webView = mockWebView
        every { mockConnectivity.isConnected() } returns true

        // 10 concurrent coroutines trying to load url.
        // in reality there should only be two (main + tealium background) in contention.
        (1..10).map {
            CoroutineScope(Dispatchers.IO).async {
                println("loading ($it)")
                webViewLoader.loadUrlToWebView()
            }
        }.awaitAll()

        verify(exactly = 1, timeout = 100) {
            mockWebView.loadUrl(any())
        }
    }

    @Test
    fun librarySettingsUpdated_ChangesTimeout() = runBlocking {
        every { mockConnectivity.isConnected() } returns false // cancels the initial load
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity)
        delay(50)

        every { SystemClock.elapsedRealtime() } returns 10000L // up for 10s
        val librarySettings = LibrarySettings(refreshInterval = 1)
        webViewLoader.lastUrlLoadTimestamp = SystemClock.elapsedRealtime()
        webViewLoader.onLibrarySettingsUpdated(librarySettings)
        assertFalse(webViewLoader.isTimedOut())
        every { SystemClock.elapsedRealtime() } returns 11000L // up for 11s
        assertTrue(webViewLoader.isTimedOut())
    }

    @Test
    fun isTimedOut() = runBlocking {
        every { mockConnectivity.isConnected() } returns false // cancels the initial load
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity)
        delay(50)

        // defaults should be true
        every { SystemClock.elapsedRealtime() } returns 10000L // up for 10s
        assertTrue(webViewLoader.isTimedOut())
        webViewLoader.lastUrlLoadTimestamp = SystemClock.elapsedRealtime() - 100
        assertTrue(webViewLoader.isTimedOut())
        webViewLoader.lastUrlLoadTimestamp = SystemClock.elapsedRealtime() - 1
        assertTrue(webViewLoader.isTimedOut())

        webViewLoader.lastUrlLoadTimestamp = SystemClock.elapsedRealtime() + 100
        assertFalse(webViewLoader.isTimedOut())
        webViewLoader.lastUrlLoadTimestamp = SystemClock.elapsedRealtime() + 1
        assertFalse(webViewLoader.isTimedOut())
    }

    @Test
    fun newSession_IsRegisteredWhenWebViewIsLoaded() = runBlocking {
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity)
        webViewLoader.webViewStatus.set(PageStatus.LOADED_SUCCESS)
        webViewLoader.onSessionStarted(12345L)

        coVerify(exactly = 1, timeout = 500) {
            mockHttpClient.get("https://tags.tiqcdn.com/utag/tiqapp/utag.v.js?a=test/profile/12345&cb=12345")
        }
    }

    @Test
    fun newSession_IsNotRegisteredWhenWebViewIsNotLoaded() = runBlocking {
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity)
        webViewLoader.webViewStatus.set(PageStatus.LOADED_ERROR)
        webViewLoader.onSessionStarted(12345L)
        webViewLoader.webViewStatus.set(PageStatus.LOADING)
        webViewLoader.onSessionStarted(12345L)
        webViewLoader.webViewStatus.set(PageStatus.INIT)
        webViewLoader.onSessionStarted(12345L)

        coVerify(exactly = 0, timeout = 100) {
            mockHttpClient.get(any())
        }
    }

    @Test
    fun newSession_IsNotRegisteredWhenSessionIdIsInvalid() = runBlocking {
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity)
        webViewLoader.webViewStatus.set(PageStatus.LOADED_SUCCESS)
        webViewLoader.onSessionStarted(WebViewLoader.INVALID_SESSION_ID)

        coVerify(exactly = 0, timeout = 100) {
            mockHttpClient.get("https://tags.tiqcdn.com/utag/tiqapp/utag.v.js?a=test/profile/12345&cb=12345")
        }
    }

    @Test
    fun newSession_IsNotRegisteredWhenNoConnectivity() {
        every { mockConnectivity.isConnected() } returns false
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity)
        webViewLoader.webViewStatus.set(PageStatus.LOADED_SUCCESS)
        webViewLoader.onSessionStarted(WebViewLoader.INVALID_SESSION_ID)

        coVerify(exactly = 0, timeout = 100) {
            mockHttpClient.get("https://tags.tiqcdn.com/utag/tiqapp/utag.v.js?a=test/profile/12345&cb=12345")
        }
    }

    @Test
    fun createSessionUrlIsCorrect() {
        assertEquals("https://tags.tiqcdn.com/utag/tiqapp/utag.v.js?a=test/profile/12345&cb=12345",
                WebViewLoader.createSessionUrl(mockTealiumConfig, 12345L))
    }
}