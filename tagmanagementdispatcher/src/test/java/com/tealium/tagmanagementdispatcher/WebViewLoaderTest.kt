package com.tealium.tagmanagementdispatcher

import android.app.Application
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.messaging.AfterDispatchSendCallbacks
import com.tealium.core.network.ConnectivityRetriever
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.setMain
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File

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

    lateinit var config: TealiumConfig
    lateinit var webViewLoader: WebViewLoader
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")


    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(mainThreadSurrogate)

        mockkConstructor(TealiumConfig::class)
        every { mockContext.filesDir } returns mockFile
        every { anyConstructed<TealiumConfig>().tealiumDirectory.mkdir() } returns mockk()
        every { anyConstructed<TealiumConfig>().tealiumDirectory.absolutePath } returns ""

        config = TealiumConfig(mockContext, "test", "profile", Environment.QA)
        every { config.application.applicationContext } returns mockContext
        every { mockTealiumContext.config } returns config

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

        mockkConstructor(ConnectivityRetriever::class)
        every { anyConstructed<ConnectivityRetriever>().isConnected() } returns true

        mockkStatic(CookieManager::class)
        every { CookieManager.getInstance() } returns mockk<CookieManager>()
        every { CookieManager.getInstance().setAcceptCookie(any()) } just Runs
        every { CookieManager.getInstance().setAcceptThirdPartyCookies(any(), any()) } just Runs
    }

    @Test
    fun webViewLoadedSuccess() {
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks)
        Thread.sleep(50)

        Assert.assertTrue(webViewLoader.isWebViewLoaded.get())
    }

    @Test
    fun webViewLoadedFailure() {
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks)
        Thread.sleep(50)
        webViewLoader.isWebViewLoaded.set(PageStatus.LOADED_ERROR)

        Assert.assertFalse(webViewLoader.isWebViewLoaded.get())
    }
}