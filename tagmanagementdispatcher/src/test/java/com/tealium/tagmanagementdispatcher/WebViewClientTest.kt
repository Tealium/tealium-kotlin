package com.tealium.tagmanagementdispatcher

import android.app.Application
import android.net.Uri
import android.webkit.*
import com.tealium.core.Logger
import com.tealium.core.Tealium
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.messaging.AfterDispatchSendCallbacks
import com.tealium.core.network.Connectivity
import com.tealium.core.network.HttpClient
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.lang.ref.WeakReference
import java.util.*

class WebViewClientTest {

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

    @RelaxedMockK
    lateinit var mockErrorResponse: WebResourceResponse

    lateinit var webViewLoader: WebViewLoader
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")
    private val mockWebViewProvider: () -> WebView = { mockWebView }

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

        every { mockErrorResponse.statusCode } returns 404
        every { mockErrorResponse.reasonPhrase } returns "error"

        mockkConstructor(WebView::class)
        every {
            anyConstructed<WebView>().settings
        } returns mockSettings
        every {
            mockSettings.databaseEnabled = any()
            mockSettings.javaScriptEnabled = any()
            mockSettings.domStorageEnabled = any()
            mockSettings.cacheMode = any()
            anyConstructed<WebView>().loadUrl(any())
        } just Runs

        every { mockConnectivity.isConnected() } returns true

        mockkStatic(CookieManager::class)
        every { CookieManager.getInstance() } returns mockk<CookieManager>()
        every { CookieManager.getInstance().setAcceptCookie(any()) } just Runs
        every { CookieManager.getInstance().setAcceptThirdPartyCookies(any(), any()) } just Runs

        mockkObject(Logger)
    }

    @Test
    fun webView_Init_LoadedSuccess() = runBlocking {
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity)
        webViewLoader.webViewInitialized.await()
        webViewLoader.webViewClient.onPageFinished(mockWebView, "testUrl")

        assertEquals(PageStatus.LOADED_SUCCESS, webViewLoader.webViewStatus.get())
    }

    @Test
    fun webView_Init_DoesNotLoadUrlWithoutConnectivity() = runBlocking {
        every { mockConnectivity.isConnected() } returns false

        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity, mockWebViewProvider)
        webViewLoader.webViewInitialized.await()

        // called on init too, but check anyway
        webViewLoader.loadUrlToWebView()

        assertEquals(PageStatus.INITIALIZED, webViewLoader.webViewStatus.get())
        verify(exactly = 0, timeout = 1000) {
            webViewLoader.webView.loadUrl(any())
        }
    }

    @Test
    fun webView_Init_DoesNotLoadUrlWithoutConnectivity_ButDoesWhenConnectivityReturns() = runBlocking {
        every { mockConnectivity.isConnected() } returns false

        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity, mockWebViewProvider)
        webViewLoader.webViewInitialized.await()
        assertEquals(PageStatus.INITIALIZED, webViewLoader.webViewStatus.get())

        every { mockConnectivity.isConnected() } returns true
        webViewLoader.loadUrlToWebView()

        assertEquals(PageStatus.LOADING, webViewLoader.webViewStatus.get())
        verify(exactly = 1, timeout = 5000) {
            webViewLoader.webView.loadUrl(any())
        }
    }

    @Test
    fun webViewClient_LoadFailure_WhenHttpErrorOnTagManagementUrl() = runBlocking {
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity)
        webViewLoader.webViewInitialized.await()
        val mockResourceRequest: WebResourceRequest = mockk()
        val mockUri: Uri = mockk()
        every { mockUri.toString() } returns "some-other-url"
        every { mockResourceRequest.url } returns mockUri

        webViewLoader.webViewClient.onReceivedHttpError(mockWebView, mockResourceRequest, mockErrorResponse)
        assertNotEquals(PageStatus.LOADED_ERROR, webViewLoader.webViewStatus.get())

        every { mockUri.toString() } returns "testUrl"
        webViewLoader.webViewClient.onReceivedHttpError(mockWebView, mockResourceRequest, mockErrorResponse)
        assertEquals(PageStatus.LOADED_ERROR, webViewLoader.webViewStatus.get())
    }

    @Test
    fun webViewClient_LoadFailure_WhenResourceFails() = runBlocking {
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity)
        webViewLoader.webViewInitialized.await()
        webViewLoader.webViewClient.onReceivedError(mockWebView, 404, "", "")

        // any errors should not be overwritten even though the load has finished
        webViewLoader.webViewClient.onPageFinished(mockWebView, "")

        assertEquals(PageStatus.LOADED_ERROR, webViewLoader.webViewStatus.get())
    }

    @Test
    fun webViewClient_LoadFailure_WhenResourceFails2() {
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity)
        val mockRequest: WebResourceRequest = mockk()
        every { mockRequest.url } returns null
        val mockError: WebResourceError = mockk()
        every { mockError.errorCode } returns 404
        every { mockError.description } returns ""
        webViewLoader.webViewClient.onReceivedError(webViewLoader.webView, mockRequest, mockError)

        // any errors should not be overwritten even though the load has finished
        webViewLoader.webViewClient.onPageFinished(mockWebView, "")

        assertEquals(PageStatus.LOADED_ERROR, webViewLoader.webViewStatus.get())
    }

    @Test
    fun webViewClient_LoadFailure_WhenResourceFails_ExcludesFavicon() = runBlocking {
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity, mockWebViewProvider)
        webViewLoader.webViewInitialized.await()
        webViewLoader.webViewClient.onReceivedError(mockWebView, 404, "", "test/favicon.ico")
        delay(50)
        // remains "loading" as no onPageFinished called
        assertEquals(PageStatus.LOADING, webViewLoader.webViewStatus.get())
    }

    @Test
    fun webViewClient_LoadFailure_WhenSslError() = runBlocking {
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity)
        webViewLoader.webViewInitialized.await()
        webViewLoader.webViewClient.onReceivedSslError(mockWebView, null, null)

        assertEquals(PageStatus.LOADED_ERROR, webViewLoader.webViewStatus.get())
    }

    @Test
    fun webViewClient_ResourceLoad_GetsLogged() {
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity)
        webViewLoader.webViewClient.onLoadResource(mockWebView, "myResource")

        verify {
            Logger.dev(any(), match {
                it.startsWith("Loaded Resource")
            })
        }
    }

    @Test
    fun webViewClient_InterceptsOnlyWhenFavicon() = runBlocking {
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity)

        assertNotNull(webViewLoader.webViewClient.shouldInterceptRequest(mockWebView, "test/favicon.ico"))

        assertNull(webViewLoader.webViewClient.shouldInterceptRequest(mockWebView, "test/favicon"))
        assertNull(webViewLoader.webViewClient.shouldInterceptRequest(mockWebView, "www.someresource.com"))
    }

    @Test
    fun webViewClient_InterceptsOnlyWhenFavicon2() = runBlocking {
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity)
        val mockResourceRequest: WebResourceRequest = mockk()
        val mockUri: Uri = mockk()
        every { mockUri.toString() } returns "test/favicon.ico"
        every { mockResourceRequest.url } returns mockUri

        assertNotNull(webViewLoader.webViewClient.shouldInterceptRequest(mockWebView, mockResourceRequest))

        every { mockUri.toString() } returns "test/favicon"
        assertNull(webViewLoader.webViewClient.shouldInterceptRequest(mockWebView, mockResourceRequest))
        every { mockUri.toString() } returns "www.someresource.com"
        assertNull(webViewLoader.webViewClient.shouldInterceptRequest(mockWebView, mockResourceRequest))
    }

    @Test
    fun webViewClient_ShouldOverrideUrlLoading_ForRemoteCommands() = runBlocking {
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity)
        webViewLoader.webViewClient.shouldOverrideUrlLoading(mockWebView, "${TagManagementRemoteCommand.PREFIX}myRemoteCommandName")
        // should not override
        webViewLoader.webViewClient.shouldOverrideUrlLoading(mockWebView, "http://myRemoteCommandName")

        coVerify(exactly = 1, timeout = 1500) {
            mockDispatchSendCallbacks.sendRemoteCommand(match {
                it.commandId == "myRemoteCommandName".toLowerCase(Locale.ROOT)
            })
        }
    }

    @Test
    fun webViewClient_ShouldOverrideUrlLoading_ForRemoteCommands2() = runBlocking {
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity)
        val mockResourceRequest: WebResourceRequest = mockk()
        val mockUri: Uri = mockk()
        every { mockUri.toString() } returns "${TagManagementRemoteCommand.PREFIX}myRemoteCommandName"
        every { mockResourceRequest.url } returns mockUri
        webViewLoader.webViewClient.shouldOverrideUrlLoading(mockWebView, mockResourceRequest)

        // should not override
        every { mockUri.toString() } returns "http://myRemoteCommandName"
        webViewLoader.webViewClient.shouldOverrideUrlLoading(mockWebView, mockResourceRequest)

        verify(exactly = 1, timeout = 1500) {
            mockDispatchSendCallbacks.sendRemoteCommand(match {
                it.commandId == "myRemoteCommandName".toLowerCase(Locale.ROOT)
            })
        }
    }

    @Test
    fun webViewClient_CrashRecreatesWebView() = runBlocking {
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity)
        webViewLoader.webViewInitialized.await()
        val originalWebView = webViewLoader.webView

        webViewLoader.webViewClient.onRenderProcessGone(mockWebView, null)
        webViewLoader.webViewInitialized.await()

        assertNotSame(originalWebView, webViewLoader.webView)
        verify(exactly = 1) {
            mockWebView.destroy()
        }
    }

    @Test
    fun webViewClient_DestroyWebViewOnInstanceShutdown() = runBlocking {
        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity, mockWebViewProvider)
        delay(1000)

        webViewLoader.onInstanceShutdown("testName", WeakReference(Tealium::class.java.cast(null)))
        delay(1000)

        verify(exactly = 1, timeout = 1000) {
            mockWebView.destroy()
        }
    }

    @Test
    fun webViewClient_DestroyWebViewHandlesExceptionOnInstanceShutdown() = runBlocking {
        every { mockWebView.destroy() } throws RuntimeException("Test WebView destruction exception")

        webViewLoader = WebViewLoader(mockTealiumContext, "testUrl", mockDispatchSendCallbacks, mockConnectivity, mockWebViewProvider)
        delay(1000)

        webViewLoader.onInstanceShutdown("testName", WeakReference(Tealium::class.java.cast(null)))
        delay(1000)

        verify(exactly = 1) {
            mockWebView.destroy()
        }
        verify {
            Logger.dev(any(), match {
                it.startsWith("Error destroying WebView on shutdown:")
            })
        }
    }
}