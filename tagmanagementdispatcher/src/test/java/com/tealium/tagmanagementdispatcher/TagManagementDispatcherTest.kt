package com.tealium.tagmanagementdispatcher

import android.app.Application
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.messaging.AfterDispatchSendCallbacks
import com.tealium.core.network.Connectivity
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.TealiumEvent
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.setMain
import org.json.JSONStringer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.net.URL

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21, 29])
class TagManagementDispatcherTest {

    @MockK
    lateinit var mockContext: Application

    @MockK
    lateinit var mockFile: File

    @MockK
    lateinit var mockDispatchSendCallbacks: AfterDispatchSendCallbacks

    @MockK
    lateinit var mockWebViewLoader: WebViewLoader

    @RelaxedMockK
    lateinit var mockSettings: WebSettings

    @RelaxedMockK
    lateinit var mockTealiumContext: TealiumContext

    @RelaxedMockK
    lateinit var mockConnectivity: Connectivity

    lateinit var config: TealiumConfig
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(mainThreadSurrogate)

        mockkConstructor(TealiumConfig::class)
        every { mockContext.filesDir } returns mockFile
        every { mockContext.resources } returns mockk(relaxed = true)
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
            mockSettings.cacheMode = any()

        } just Runs

        every { mockConnectivity.isConnected() } returns true
        every { mockConnectivity.isConnectedWifi() } returns true

        mockkStatic(CookieManager::class)
        every { CookieManager.getInstance() } returns mockk<CookieManager>()
        every { CookieManager.getInstance().setAcceptCookie(any()) } just Runs
        every { CookieManager.getInstance().setAcceptThirdPartyCookies(any(), any()) } just Runs
    }

    @Test
    fun tagManagementDispatcherHasValidDefaultUrl() {
        val tagManagementDispatcher = TagManagementDispatcher(mockTealiumContext, mockDispatchSendCallbacks, mockConnectivity)
        val url = URL(tagManagementDispatcher.urlString)

        assertEquals("https", url.protocol)
        assertEquals("tags.tiqcdn.com", url.authority)
        assertEquals("/utag/${config.accountName}/${config.profileName}/${config.environment.environment}/mobile.html", url.path)
        assertEquals("/utag/${config.accountName}/${config.profileName}/${config.environment.environment}/mobile.html", url.path)

        val queryParams = url.query.split("&").associate { param ->
            Pair(param.split("=")[0], param.split("=")[1])
        }
        assertEquals("android", queryParams[Dispatch.Keys.DEVICE_PLATFORM])
        assertEquals(Build.VERSION.RELEASE, queryParams[Dispatch.Keys.DEVICE_OS_VERSION])
        assertEquals(BuildConfig.VERSION_NAME, queryParams[Dispatch.Keys.LIBRARY_VERSION])
        assertEquals("true", queryParams["sdk_session_count"])
    }

    @Test
    fun overrideTagManagementUrlIsSet() {
        val tagManagementDispatcher = TagManagementDispatcher(mockTealiumContext, mockDispatchSendCallbacks, mockConnectivity)
        config.options["override_tag_management_url"] = "test_url"
        assertEquals("test_url", tagManagementDispatcher.urlString)
    }

    @Test
    fun webViewLoadedShouldNotQueueDispatch() {
        val tagManagementDispatcher = TagManagementDispatcher(mockTealiumContext, mockDispatchSendCallbacks, mockConnectivity)
        val dispatch = TealiumEvent("", emptyMap())
        tagManagementDispatcher.webViewLoader.webViewStatus.set(PageStatus.LOADED_SUCCESS)
        val result = tagManagementDispatcher.shouldQueue(dispatch)
        assertFalse(result)
    }

    @Test
    fun webViewNotLoadedShouldQueueDispatch() {
        val tagManagementDispatcher = TagManagementDispatcher(mockTealiumContext, mockDispatchSendCallbacks, mockConnectivity)
        val dispatch = TealiumEvent("", emptyMap())
        tagManagementDispatcher.webViewLoader.webViewStatus.set(PageStatus.LOADED_ERROR)
        assertTrue(tagManagementDispatcher.shouldQueue(dispatch))

        tagManagementDispatcher.webViewLoader.webViewStatus.set(PageStatus.INIT)
        assertTrue(tagManagementDispatcher.shouldQueue(dispatch))
        tagManagementDispatcher.webViewLoader.webViewStatus.set(PageStatus.LOADING)
        assertTrue(tagManagementDispatcher.shouldQueue(dispatch))
    }

    @Test
    fun webViewFailedShouldQueueByDefault() {
        val dispatch = TealiumEvent("", emptyMap())
        mockkConstructor(WebViewLoader::class)
        every { anyConstructed<WebViewLoader>().hasReachedMaxErrors() } returns true

        val tagManagementDispatcher = TagManagementDispatcher(mockTealiumContext, mockDispatchSendCallbacks, mockConnectivity)
        tagManagementDispatcher.webViewLoader.webViewStatus.set(PageStatus.LOADED_ERROR)
        assertTrue(tagManagementDispatcher.shouldQueue(dispatch))

        tagManagementDispatcher.webViewLoader.webViewStatus.set(PageStatus.LOADED_SUCCESS)
        assertTrue(tagManagementDispatcher.shouldQueue(dispatch))
    }

    @Test
    fun webViewFailedShouldNotQueueWhenConfigured() {
        val dispatch = TealiumEvent("", emptyMap())
        mockkConstructor(WebViewLoader::class)
        config.shouldQueueOnLoadFailure = false
        every { anyConstructed<WebViewLoader>().hasReachedMaxErrors() } returns true

        val tagManagementDispatcher = TagManagementDispatcher(mockTealiumContext, mockDispatchSendCallbacks, mockConnectivity)
        tagManagementDispatcher.webViewLoader.webViewStatus.set(PageStatus.LOADED_ERROR)
        assertFalse(tagManagementDispatcher.shouldQueue(dispatch))

        tagManagementDispatcher.webViewLoader.webViewStatus.set(PageStatus.LOADED_SUCCESS)
        assertFalse(tagManagementDispatcher.shouldQueue(dispatch))
    }

    @Test
    fun shouldNeverDropDispatch() {
        val tagManagementDispatcher = TagManagementDispatcher(mockTealiumContext, mockDispatchSendCallbacks, mockConnectivity)
        val dispatch = TealiumEvent("", emptyMap())

        assertFalse(tagManagementDispatcher.shouldDrop(dispatch))
    }

    @Test
    fun dispatchReadyCallRemoteCommandTags() {
        val tagManagementDispatcher = TagManagementDispatcher(mockTealiumContext, mockDispatchSendCallbacks, mockConnectivity)
        val dispatch = TealiumEvent("test", mapOf("key" to "value"))
        tagManagementDispatcher.webViewLoader = mockWebViewLoader
        every { mockWebViewLoader.webViewStatus.get() } returns PageStatus.LOADED_SUCCESS
        tagManagementDispatcher.onDispatchReady(dispatch)

        coVerify { tagManagementDispatcher.callRemoteCommandTags(dispatch) }
    }

    @Test
    fun dispatchReadyCreateWebViewClient() {
        val tagManagementDispatcher = TagManagementDispatcher(mockTealiumContext, mockDispatchSendCallbacks, mockConnectivity)
        tagManagementDispatcher.webViewLoader = mockWebViewLoader
        every { mockWebViewLoader.webViewStatus.get() } returns PageStatus.LOADED_ERROR
        every { mockWebViewLoader.loadUrlToWebView() } just Runs
        every { mockWebViewLoader.isTimedOut() } returns true

        val dispatch = TealiumEvent("test", mapOf("key" to "value"))
        tagManagementDispatcher.onDispatchReady(dispatch)

        coVerify { mockWebViewLoader.loadUrlToWebView() }
    }

    @Test
    fun dispatchReady_InitialisesWebView_WhenNotInitialized() {
        val tagManagementDispatcher = TagManagementDispatcher(mockTealiumContext, mockDispatchSendCallbacks, mockConnectivity)
        tagManagementDispatcher.webViewLoader = mockWebViewLoader
        every { mockWebViewLoader.webViewStatus.get() } returns PageStatus.INIT
        every { mockWebViewLoader.initializeWebView() } returns mockk()

        val dispatch = TealiumEvent("test", mapOf("key" to "value"))
        tagManagementDispatcher.onDispatchReady(dispatch)

        coVerify { mockWebViewLoader.initializeWebView() }
    }

    @Test
    fun dispatchReady_LoadsUrl_WhenInitialized_AndTimedOut() {
        val tagManagementDispatcher = TagManagementDispatcher(mockTealiumContext, mockDispatchSendCallbacks, mockConnectivity)
        tagManagementDispatcher.webViewLoader = mockWebViewLoader
        every { mockWebViewLoader.webViewStatus.get() } returns PageStatus.INITIALIZED
        every { mockWebViewLoader.loadUrlToWebView() } just Runs
        every { mockWebViewLoader.isTimedOut() } returns true

        val dispatch = TealiumEvent("test", mapOf("key" to "value"))
        tagManagementDispatcher.onDispatchReady(dispatch)

        coVerify { mockWebViewLoader.loadUrlToWebView() }
    }

    @Test
    fun dispatchReady_DoesNothing_WhenInitialized_AndNotTimedOut() {
        val tagManagementDispatcher = TagManagementDispatcher(mockTealiumContext, mockDispatchSendCallbacks, mockConnectivity)
        tagManagementDispatcher.webViewLoader = mockWebViewLoader
        every { mockWebViewLoader.webViewStatus.get() } returns PageStatus.INITIALIZED
        every { mockWebViewLoader.loadUrlToWebView() } just Runs
        every { mockWebViewLoader.isTimedOut() } returns false

        val dispatch = TealiumEvent("test", mapOf("key" to "value"))
        tagManagementDispatcher.onDispatchReady(dispatch)

        coVerify(exactly = 0) { mockWebViewLoader.loadUrlToWebView() }
    }

    @Test
    fun dispatchReady_LoadsUrl_WhenLoadedError_AndTimedOut() {
        val tagManagementDispatcher = TagManagementDispatcher(mockTealiumContext, mockDispatchSendCallbacks, mockConnectivity)
        tagManagementDispatcher.webViewLoader = mockWebViewLoader
        every { mockWebViewLoader.webViewStatus.get() } returns PageStatus.LOADED_ERROR
        every { mockWebViewLoader.loadUrlToWebView() } just Runs
        every { mockWebViewLoader.isTimedOut() } returns true

        val dispatch = TealiumEvent("test", mapOf("key" to "value"))
        tagManagementDispatcher.onDispatchReady(dispatch)

        coVerify { mockWebViewLoader.loadUrlToWebView() }
    }

    @Test
    fun dispatchReady_DoesNothing_WhenLoadedError_AndNotTimedOut() {
        val tagManagementDispatcher = TagManagementDispatcher(mockTealiumContext, mockDispatchSendCallbacks, mockConnectivity)
        tagManagementDispatcher.webViewLoader = mockWebViewLoader
        every { mockWebViewLoader.webViewStatus.get() } returns PageStatus.LOADED_ERROR
        every { mockWebViewLoader.loadUrlToWebView() } just Runs
        every { mockWebViewLoader.isTimedOut() } returns false

        val dispatch = TealiumEvent("test", mapOf("key" to "value"))
        tagManagementDispatcher.onDispatchReady(dispatch)

        coVerify(exactly = 0) { mockWebViewLoader.loadUrlToWebView() }
    }

    @Test
    fun dispatchReady_DoesNothing_WhenInitializing() {
        val tagManagementDispatcher = TagManagementDispatcher(mockTealiumContext, mockDispatchSendCallbacks, mockConnectivity)
        tagManagementDispatcher.webViewLoader = mockWebViewLoader
        every { mockWebViewLoader.webViewStatus.get() } returns PageStatus.INITIALIZING
        every { mockWebViewLoader.loadUrlToWebView() } just Runs

        val dispatch = TealiumEvent("test", mapOf("key" to "value"))
        tagManagementDispatcher.onDispatchReady(dispatch)

        coVerify(exactly = 0) {
            mockWebViewLoader.loadUrlToWebView()
            mockWebViewLoader.initializeWebView()
        }
    }

    @Test
    fun dispatchReady_DoesNotLoadUrl_IfLoading() {
        val tagManagementDispatcher = TagManagementDispatcher(mockTealiumContext, mockDispatchSendCallbacks, mockConnectivity)
        tagManagementDispatcher.webViewLoader = mockWebViewLoader
        every { mockWebViewLoader.webViewStatus.get() } returns PageStatus.LOADING
        every { mockWebViewLoader.loadUrlToWebView() } just Runs

        val dispatch = TealiumEvent("test", mapOf("key" to "value"))
        tagManagementDispatcher.onDispatchReady(dispatch)

        coVerify(exactly = 0) { mockWebViewLoader.loadUrlToWebView() }
    }

    @Test
    fun callRemoteCommandTags_DoesCall_WhenRemoteApiEnabled() {
        config.remoteApiEnabled = true
        val tagManagementDispatcher = TagManagementDispatcher(mockTealiumContext, mockDispatchSendCallbacks, mockConnectivity)
        val mockWebView: WebView = mockk()
        every { mockWebViewLoader.webView } returns mockWebView
        tagManagementDispatcher.webViewLoader = mockWebViewLoader
        every { mockWebViewLoader.webViewStatus.get() } returns PageStatus.LOADED_SUCCESS
        every { mockWebViewLoader.loadUrlToWebView() } just Runs

        tagManagementDispatcher.callRemoteCommandTags(TealiumEvent("test"))

        verify(exactly = 1, timeout = 1000) {
            mockWebView.evaluateJavascript(any(), any())
        }
    }

    @Test
    fun callRemoteCommandTags_DoesNotCall_WhenRemoteApiDisabled() {
        config.remoteApiEnabled = false
        val tagManagementDispatcher = TagManagementDispatcher(mockTealiumContext, mockDispatchSendCallbacks, mockConnectivity)
        val mockWebView: WebView = mockk()
        every { mockWebViewLoader.webView } returns mockWebView
        tagManagementDispatcher.webViewLoader = mockWebViewLoader
        every { mockWebViewLoader.webViewStatus.get() } returns PageStatus.LOADED_SUCCESS
        every { mockWebViewLoader.loadUrlToWebView() } just Runs

        tagManagementDispatcher.callRemoteCommandTags(TealiumEvent("test"))

        verify(exactly = 0, timeout = 1000) {
            mockWebView.evaluateJavascript(any(), any())
        }
    }
}