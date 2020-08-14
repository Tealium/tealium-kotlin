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
import com.tealium.dispatcher.TealiumEvent
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.setMain
import org.json.JSONStringer
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File
import java.net.URL

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

    lateinit var config: TealiumConfig
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Before
    fun setup() {
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
    fun tagManagementDispatcherHasValidDefaultUrl() {
        val tagManagementDispatcher = TagManagementDispatcher(mockTealiumContext, mockDispatchSendCallbacks)
        val url = URL(tagManagementDispatcher.urlString)

        Assert.assertEquals("https", url.protocol)
        Assert.assertEquals("tags.tiqcdn.com", url.authority)
        Assert.assertEquals("/utag/${config.accountName}/${config.profileName}/${config.environment.environment}/mobile.html", url.path)
        Assert.assertEquals("/utag/${config.accountName}/${config.profileName}/${config.environment.environment}/mobile.html", url.path)

        val queryParams = url.query.split("&").associate { param ->
            Pair(param.split("=")[0], param.split("=")[1])
        }
        Assert.assertEquals("android", queryParams[DeviceCollectorConstants.DEVICE_PLATFORM])
        Assert.assertEquals(/* Build.VERSION.RELEASE */"null", queryParams[DeviceCollectorConstants.DEVICE_OS_VERSION])
        Assert.assertEquals(BuildConfig.VERSION_NAME, queryParams[CoreConstant.LIBRARY_VERSION])
        Assert.assertEquals("true", queryParams["sdk_session_count"])
    }

    @Test
    fun overrideTagManagementUrlIsSet() {
        val tagManagementDispatcher = TagManagementDispatcher(mockTealiumContext, mockDispatchSendCallbacks)
        config.options["override_tag_management_url"] = "test_url"
        Assert.assertEquals("test_url", tagManagementDispatcher.urlString)
    }

    @Test
    fun webViewLoadedShouldNotQueueDispatch() {
        val tagManagementDispatcher = TagManagementDispatcher(mockTealiumContext, mockDispatchSendCallbacks)
        tagManagementDispatcher.webViewLoader.isWebViewLoaded.set(true)
        val dispatch = TealiumEvent("", emptyMap())
        val result = tagManagementDispatcher.shouldQueue(dispatch)
        Assert.assertFalse(result)
    }

    @Test
    fun webViewNotLoadedShouldQueueDispatch() {
        val tagManagementDispatcher = TagManagementDispatcher(mockTealiumContext, mockDispatchSendCallbacks)
        val dispatch = TealiumEvent("", emptyMap())
        tagManagementDispatcher.webViewLoader.isWebViewLoaded.set(false)
        val result = tagManagementDispatcher.shouldQueue(dispatch)
        Assert.assertTrue(result)
    }

    @Test
    fun dispatchReadyCallRemoteCommandTags() {
        val tagManagementDispatcher = TagManagementDispatcher(mockTealiumContext, mockDispatchSendCallbacks)
        mockkConstructor(JSONStringer::class)
        every { anyConstructed<JSONStringer>().toString() } returns ""
        val dispatch = TealiumEvent("test", mapOf("key" to "value"))
        tagManagementDispatcher.onDispatchReady(dispatch)

        coVerify { tagManagementDispatcher.callRemoteCommandTags(dispatch) }
    }

    @Test
    fun dispatchReadyCreateWebViewClient() {
        val tagManagementDispatcher = TagManagementDispatcher(mockTealiumContext, mockDispatchSendCallbacks)
        tagManagementDispatcher.webViewLoader = mockWebViewLoader
        every { mockWebViewLoader.isWebViewLoaded.get() } returns false
        every { mockWebViewLoader.createWebViewClient() } returns mockk()

        val dispatch = TealiumEvent("test", mapOf("key" to "value"))
        tagManagementDispatcher.onDispatchReady(dispatch)

        verify { tagManagementDispatcher.webViewLoader.createWebViewClient() }
    }
}