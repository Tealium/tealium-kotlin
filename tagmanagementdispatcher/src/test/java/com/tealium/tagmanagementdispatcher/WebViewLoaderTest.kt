package com.tealium.tagmanagementdispatcher

import android.app.Application
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.messaging.AfterDispatchSendCallbacks
import com.tealium.core.network.Connectivity
import com.tealium.core.network.HttpClient
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class WebViewLoaderTest {

    @MockK
    lateinit var mockContext: Application

    @MockK
    lateinit var mockFile: File

    @MockK
    lateinit var mockDispatchSendCallbacks: AfterDispatchSendCallbacks

    @RelaxedMockK
    lateinit var mockSettings: WebSettings

    @RelaxedMockK
    lateinit var mockWebView: WebView

    @RelaxedMockK
    lateinit var mockTealiumContext: TealiumContext

    @RelaxedMockK
    lateinit var mockHttpClient: HttpClient

    @RelaxedMockK
    lateinit var mockConnectivity: Connectivity

    lateinit var config: TealiumConfig
    lateinit var webViewLoader: WebViewLoader
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")


    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(mainThreadSurrogate)

        mockkConstructor(TealiumConfig::class)
        every { mockContext.filesDir } returns mockFile
        every { anyConstructed<TealiumConfig>().tealiumDirectory.mkdir() } returns mockk()
        every { anyConstructed<TealiumConfig>().tealiumDirectory.absolutePath } returns ""

        config = TealiumConfig(mockContext, "test", "profile", Environment.QA)
        every { config.application.applicationContext } returns mockContext
        every { mockTealiumContext.config } returns config
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

        } just Runs

        every { mockConnectivity.isConnected() } returns true

        mockkStatic(CookieManager::class)
        every { CookieManager.getInstance() } returns mockk<CookieManager>()
        every { CookieManager.getInstance().setAcceptCookie(any()) } just Runs
        every { CookieManager.getInstance().setAcceptThirdPartyCookies(any(), any()) } just Runs
    }

    @Test
    fun webViewLoadedSuccess() {
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity)
        Thread.sleep(50)

        Assert.assertTrue(webViewLoader.isWebViewLoaded.get())
    }

    @Test
    fun webViewLoadedFailure() {
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity)
        Thread.sleep(50)
        webViewLoader.isWebViewLoaded.set(PageStatus.LOADED_ERROR)

        Assert.assertFalse(webViewLoader.isWebViewLoaded.get())
    }

    @Test
    fun newSession_IsRegisteredWhenWebViewIsLoaded() = runBlocking {
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity)
        webViewLoader.isWebViewLoaded.set(true)
        webViewLoader.onSessionStarted(12345L)

        coVerify(exactly = 1, timeout = 500) {
           mockHttpClient.get("https://tags.tiqcdn.com/utag/tiqapp/utag.v.js?a=test/profile/12345&cb=12345")
        }
    }

    @Test
    fun newSession_IsNotRegisteredWhenWebViewIsNotLoaded() = runBlocking {
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity)
        webViewLoader.isWebViewLoaded.set(false)
        webViewLoader.onSessionStarted(12345L)

        coVerify(exactly = 0, timeout = 100) {
            mockHttpClient.get(any())
        }
    }

    @Test
    fun newSession_IsNotRegisteredWhenSessionIdIsInvalid() = runBlocking {
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity)
        webViewLoader.isWebViewLoaded.set(true)
        webViewLoader.onSessionStarted(WebViewLoader.INVALID_SESSION_ID)

        coVerify(exactly = 0, timeout = 100) {
            mockHttpClient.get("https://tags.tiqcdn.com/utag/tiqapp/utag.v.js?a=test/profile/12345&cb=12345")
        }
    }

    @Test
    fun newSession_IsNotRegisteredWhenNoConnectivity() {
        every { mockConnectivity.isConnected() } returns false
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity)
        webViewLoader.isWebViewLoaded.set(true)
        webViewLoader.onSessionStarted(WebViewLoader.INVALID_SESSION_ID)

        coVerify(exactly = 0, timeout = 100) {
            mockHttpClient.get("https://tags.tiqcdn.com/utag/tiqapp/utag.v.js?a=test/profile/12345&cb=12345")
        }
    }

    @Test
    fun createSessionUrlIsCorrect() {
        Assert.assertEquals("https://tags.tiqcdn.com/utag/tiqapp/utag.v.js?a=test/profile/12345&cb=12345",
                WebViewLoader.createSessionUrl(config, 12345L))
    }
}