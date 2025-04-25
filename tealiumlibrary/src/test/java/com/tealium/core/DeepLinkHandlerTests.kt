package com.tealium.core

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import com.tealium.core.persistence.DataLayer
import com.tealium.core.persistence.Expiry
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.TealiumEvent
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21, 28])
class DeepLinkHandlerTests {

    @RelaxedMockK
    lateinit var mockApplication: Application

    @RelaxedMockK
    lateinit var mockDataLayer: DataLayer

    @RelaxedMockK
    lateinit var mockContext: TealiumContext

    @RelaxedMockK
    lateinit var mockTealium: Tealium

    @RelaxedMockK
    lateinit var mockActivity: Activity
    lateinit var intent: Intent

    lateinit var configWithNoModules: TealiumConfig

    lateinit var builder: Uri.Builder
    val queryParams = mapOf(
        "_dfgsdftwet" to "sdgfdgfdfg",
        "_fbclid" to "1234567",
        "tealium" to "cdh",
        "bool" to true,
        "int" to 123
    )
    lateinit var uri: Uri

    val configWithQRTraceDisabled
        get(): TealiumConfig {
            val config = configWithNoModules
            config.qrTraceEnabled = false
            return config
        }

    val configWithQRTraceEnabled
        get(): TealiumConfig {
            val config = configWithNoModules
            config.qrTraceEnabled = true
            return config
        }

    val configWithDeepLinkingDisabled
        get(): TealiumConfig {
            val config = configWithNoModules
            config.deepLinkTrackingEnabled = false
            return config
        }

    val configWithDeepLinkingEnabled
        get(): TealiumConfig {
            val config = configWithNoModules
            config.deepLinkTrackingEnabled = true
            return config
        }

    val backgroundScope = CoroutineScope(kotlinx.coroutines.Dispatchers.Default)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        configWithNoModules = TealiumConfig(
            mockApplication,
            "test",
            "test",
            Environment.DEV
        )

        every { mockContext.dataLayer } returns mockDataLayer
        every { mockContext.tealium } returns mockTealium

        intent = Intent()
        every { mockActivity.intent } returns intent
        intent.action = Intent.ACTION_VIEW
    }

    fun setupUriBuilder() {
        builder = Uri.Builder().scheme("https")
            .authority("tealium.com")
            .path("/")
        queryParams.forEach { entry ->
            builder.appendQueryParameter(entry.key, entry.value.toString())
        }
        uri = builder.build()
    }

    @Test
    fun testHandleDeepLink() {
        every { mockContext.config } returns configWithDeepLinkingEnabled

        val deepLinkHandler = DeepLinkHandler(mockContext, backgroundScope)
        setupUriBuilder()

        runBlocking {
            deepLinkHandler.handleDeepLink(uri)
            delay(100)
        }

        verify(exactly = 1) {
            mockDataLayer.putString(
                "deep_link_url",
                uri.toString(),
                Expiry.SESSION
            )
        }
    }

    @Test
    fun testHandleDeepLinkDoesNotRunFromActivityResumeIfDisabled() {
        every { mockContext.config } returns configWithDeepLinkingDisabled
        val deepLinkHandler = DeepLinkHandler(mockContext, backgroundScope)
        setupUriBuilder()

        intent.data = uri
        runBlocking {
            deepLinkHandler.handleActivityResumed(mockActivity)
            delay(100)
        }
        verify(exactly = 0) {
            mockDataLayer.putString(
                "deep_link_url",
                uri.toString(),
                Expiry.SESSION
            )
        }
    }

    @Test
    fun testHandleDeepLinkSendsDeepLinkEventIfEnabled() {
        every { mockContext.config } returns configWithDeepLinkingEnabled
        mockContext.config.sendDeepLinkEvent = true
        val deepLinkHandler = DeepLinkHandler(mockContext, backgroundScope)
        setupUriBuilder()

        runBlocking {
            deepLinkHandler.handleDeepLink(uri)
            delay(100)
        }

        verify(exactly = 1) {
            mockContext.track(
                withArg { dispatch ->
                    Assert.assertEquals("deep_link", dispatch["tealium_event"])
                }
            )
        }
    }

    @Test
    fun testHandleDeepLinkDoesNotSendDeepLinkEventIfDisabled() {
        every { mockContext.config } returns configWithDeepLinkingEnabled
        mockContext.config.sendDeepLinkEvent = false
        val deepLinkHandler = DeepLinkHandler(mockContext, backgroundScope)
        setupUriBuilder()

        runBlocking {
            deepLinkHandler.handleDeepLink(uri)
            delay(100)
        }

        verify(exactly = 0) {
            mockContext.track(
                withArg { dispatch ->
                    Assert.assertEquals("deep_link", dispatch["tealium_event"])
                }
            )
        }
    }

    @Test
    fun testHandleDeepLinkFromActivityResume() {
        every { mockContext.config } returns configWithDeepLinkingEnabled
        val deepLinkHandler = DeepLinkHandler(mockContext, backgroundScope)
        setupUriBuilder()

        intent.data = uri
        runBlocking {
            deepLinkHandler.handleActivityResumed(mockActivity)
            delay(100)
        }

        verify(exactly = 1) {
            mockDataLayer.putString(
                "deep_link_url",
                uri.toString(),
                Expiry.SESSION
            )
        }
    }

    @Test
    fun testHandleDeepLinkIgnoresIncorrectActions() {
        every { mockContext.config } returns configWithDeepLinkingEnabled
        val deepLinkHandler = DeepLinkHandler(mockContext, backgroundScope)
        setupUriBuilder()

        intent.data = uri
        intent.action = Intent.ACTION_TRANSLATE
        runBlocking {
            deepLinkHandler.handleActivityResumed(mockActivity)
            delay(100)
        }

        verify(inverse = true) {
            mockDataLayer.putString(
                "deep_link_url",
                uri.toString(),
                Expiry.SESSION
            )
        }
    }

    @Test
    fun testHandleDeepLinkIgnoresNullActions() {
        every { mockContext.config } returns configWithDeepLinkingEnabled
        val deepLinkHandler = DeepLinkHandler(mockContext, backgroundScope)
        setupUriBuilder()

        intent.data = uri
        intent.action = null
        runBlocking {
            deepLinkHandler.handleActivityResumed(mockActivity)
            delay(100)
        }

        verify(inverse = true) {
            mockDataLayer.putString(
                "deep_link_url",
                uri.toString(),
                Expiry.SESSION
            )
        }
    }

    @Test
    fun testHandleDeepLinkIgnoresEmptyUris() {
        every { mockContext.config } returns configWithDeepLinkingEnabled
        val deepLinkHandler = DeepLinkHandler(mockContext, backgroundScope)
        setupUriBuilder()

        intent.data = Uri.parse("")
        runBlocking {
            deepLinkHandler.handleActivityResumed(mockActivity)
            delay(100)
        }

        verify(inverse = true) {
            mockDataLayer.putString(
                "deep_link_url",
                any(),
                Expiry.SESSION
            )
            mockDataLayer.remove(
                any()
            )
        }
    }

    @Test
    fun testHandleDeepLinkIgnoresOpaqueUris() {
        every { mockContext.config } returns configWithDeepLinkingEnabled
        val deepLinkHandler = DeepLinkHandler(mockContext, backgroundScope)

        val opaqueUri = Uri.Builder().scheme("tealium").opaquePart("test").build()
        assertTrue(opaqueUri.isOpaque)
        runBlocking {
            deepLinkHandler.handleDeepLink(opaqueUri)
            delay(100)
        }

        verify {
            mockDataLayer wasNot Called
        }
    }

    @Test
    fun testOnActivityResumedIgnoresOpaqueUris() {
        every { mockContext.config } returns configWithDeepLinkingEnabled
        val deepLinkHandler = DeepLinkHandler(mockContext, backgroundScope)
        setupUriBuilder()

        val opaqueUri = Uri.Builder().scheme("tealium").opaquePart("test").build()
        assertTrue(opaqueUri.isOpaque)
        intent.data = opaqueUri
        runBlocking {
            deepLinkHandler.handleActivityResumed(mockActivity)
            delay(100)
        }

        verify {
            mockDataLayer wasNot Called
        }
    }

    @Test
    fun testHandleJoinTraceFromActivityResume() {
        every { mockContext.config } returns configWithQRTraceEnabled
        val deepLinkHandler = DeepLinkHandler(mockContext, backgroundScope)
        setupUriBuilder()

        val traceId = "abc123"
        builder.appendQueryParameter("tealium_trace_id", traceId)

        intent.data = builder.build()
        runBlocking {
            deepLinkHandler.handleActivityResumed(mockActivity)
            delay(100)
        }

        verify(exactly = 1) {
            mockDataLayer.putString(
                Dispatch.Keys.TRACE_ID,
                traceId,
                Expiry.SESSION
            )
        }
    }

    @Test
    fun testHandleLeaveTraceFromActivityResume() {
        every { mockContext.config } returns configWithQRTraceEnabled
        val deepLinkHandler = DeepLinkHandler(mockContext, backgroundScope)
        setupUriBuilder()

        val traceId = "abc123"
        val queryParams = mapOf<String, Any>(
            "tealium_trace_id" to traceId,
            DeepLinkHandler.LEAVE_TRACE_QUERY_PARAM to "true"
        )

        queryParams.forEach { entry ->
            builder.appendQueryParameter(entry.key, entry.value.toString())
        }

        intent.data = builder.build()
        runBlocking {
            deepLinkHandler.handleActivityResumed(mockActivity)
            delay(100)
        }
        verify(exactly = 1) { mockDataLayer.remove(Dispatch.Keys.TRACE_ID) }
    }

    @Test
    fun testKillVisitorSessionFromActivityResume() {
        every { mockContext.config } returns configWithQRTraceEnabled
        every { mockContext.track(TealiumEvent("kill_visitor_session")) } just Runs
        setupUriBuilder()

        runBlocking {
            val deepLinkHandler = DeepLinkHandler(mockContext, backgroundScope)

            val traceId = "abc123"
            val queryParams = mapOf<String, Any>(
                "tealium_trace_id" to traceId,
                DeepLinkHandler.KILL_VISITOR_SESSION to "true"
            )

            queryParams.forEach { entry ->
                builder.appendQueryParameter(entry.key, entry.value.toString())
            }

            intent.data = builder.build()
            runBlocking {
                deepLinkHandler.handleActivityResumed(mockActivity)
                delay(100)
            }

            verify(exactly = 1) {
                mockContext.track(
                    withArg { dispatch ->
                        Assert.assertEquals("kill_visitor_session", dispatch["tealium_event"])
                        Assert.assertEquals("kill_visitor_session", dispatch["event"])
                    })
            }
        }
    }

    @Test
    fun testHandleJoinTraceDoesNotRunFromActivityResumeIfDisabled() {
        every { mockContext.config } returns configWithQRTraceDisabled
        val deepLinkHandler = DeepLinkHandler(mockContext, backgroundScope)
        setupUriBuilder()

        val traceId = "abc123"
        builder.appendQueryParameter("tealium_trace_id", traceId)

        intent.data = builder.build()
        runBlocking {
            deepLinkHandler.handleActivityResumed(mockActivity)
            delay(100)
        }

        verify(exactly = 0) { mockDataLayer.putString("tealium_trace_id", traceId, Expiry.SESSION) }
    }

    @Test
    fun testJoinTrace() {
        every { mockContext.config } returns configWithQRTraceEnabled
        val deepLinkHandler = DeepLinkHandler(mockContext, backgroundScope)
        deepLinkHandler.joinTrace("abc123")
        verify(exactly = 1) { mockDataLayer.putString("cp.trace_id", "abc123", Expiry.SESSION) }
    }

    @Test
    fun testLeaveTrace() {
        every { mockContext.config } returns configWithQRTraceEnabled
        val deepLinkHandler = DeepLinkHandler(mockContext, backgroundScope)
        deepLinkHandler.leaveTrace()
        verify(exactly = 1) { mockDataLayer.remove("cp.trace_id") }
    }

    @Test
    fun testKillVisitorSessionTriggersTrackCall() {
        every { mockContext.config } returns configWithQRTraceEnabled
        every { mockContext.track(TealiumEvent("kill_visitor_session")) } just Runs
        val deepLinkHandler = DeepLinkHandler(mockContext, backgroundScope)
        deepLinkHandler.killTraceVisitorSession()

        verify(exactly = 1) {
            mockContext.track(
                withArg { dispatch ->
                    Assert.assertEquals("kill_visitor_session", dispatch["tealium_event"])
                    Assert.assertEquals("kill_visitor_session", dispatch["event"])
                })
        }
    }

    @Test
    fun testRemoveOldDeepLinkDataRemovesAllOldParams() {
        every { mockContext.config } returns configWithQRTraceEnabled
        every { mockDataLayer.keys() } returns listOf(
            "tealium_key1",
            "other_key",
            "${Dispatch.Keys.DEEP_LINK_QUERY_PREFIX}_test1",
            "${Dispatch.Keys.DEEP_LINK_QUERY_PREFIX}_test2"
        )

        val deepLinkHandler = DeepLinkHandler(mockContext, backgroundScope)
        deepLinkHandler.removeOldDeepLinkData()

        verify {
            mockDataLayer.remove("${Dispatch.Keys.DEEP_LINK_QUERY_PREFIX}_test1")
            mockDataLayer.remove("${Dispatch.Keys.DEEP_LINK_QUERY_PREFIX}_test2")
        }
        verify(inverse = true) {
            mockDataLayer.remove("tealium_key1")
            mockDataLayer.remove("other_key")
        }
    }

    @Test
    fun testNewDeepLinkAddsQueryParamsToDatalayer() {
        every { mockContext.config } returns configWithDeepLinkingEnabled

        val deepLinkHandler = DeepLinkHandler(mockContext, backgroundScope)
        setupUriBuilder()

        runBlocking {
            deepLinkHandler.handleDeepLink(uri)
            delay(100)
        }

        verify {
            mockDataLayer.putString(
                "deep_link_url",
                uri.toString(),
                Expiry.SESSION
            )
        }

        queryParams.forEach {
            verify {
                mockDataLayer.putString(
                    "deep_link_param_" + it.key,
                    it.value.toString(),
                    Expiry.SESSION
                )
            }
        }
    }

    @Test
    fun testNewDeepLinkReplacesOldDeeplinkData() {
        every { mockContext.config } returns configWithQRTraceEnabled
        every { mockDataLayer.keys() } returns listOf(
            "tealium_key1",
            "other_key",
            "${Dispatch.Keys.DEEP_LINK_QUERY_PREFIX}_test1",
            "${Dispatch.Keys.DEEP_LINK_QUERY_PREFIX}_test2"
        )

        setupUriBuilder()

        runBlocking {
            val deepLinkHandler = DeepLinkHandler(mockContext, backgroundScope)
            deepLinkHandler.handleDeepLink(builder.build())
            delay(100)
        }

        verify {
            mockDataLayer.remove("${Dispatch.Keys.DEEP_LINK_QUERY_PREFIX}_test1")
            mockDataLayer.remove("${Dispatch.Keys.DEEP_LINK_QUERY_PREFIX}_test2")
        }
        queryParams.forEach {
            verify {
                mockDataLayer.putString(
                    "deep_link_param_" + it.key,
                    it.value.toString(),
                    Expiry.SESSION
                )
            }
        }
        verify(inverse = true) {
            mockDataLayer.remove("tealium_key1")
            mockDataLayer.remove("other_key")
        }
    }

    @Test
    fun testSameDeepLinkDoesNotReplaceData() {
        every { mockContext.config } returns configWithQRTraceEnabled
        every { mockDataLayer.keys() } returns listOf(
            "tealium_key1",
            "other_key",
            "${Dispatch.Keys.DEEP_LINK_QUERY_PREFIX}_test1",
            "${Dispatch.Keys.DEEP_LINK_QUERY_PREFIX}_test2"
        )

        setupUriBuilder()

        every { mockDataLayer.getString(Dispatch.Keys.DEEP_LINK_URL) } returns uri.toString()

        runBlocking {
            val deepLinkHandler = DeepLinkHandler(mockContext, backgroundScope)
            deepLinkHandler.handleDeepLink(builder.build())
            delay(100)
        }

        verify(inverse = true) {
            mockDataLayer.putString(Dispatch.Keys.DEEP_LINK_URL, any(), any())
            mockDataLayer.remove("${Dispatch.Keys.DEEP_LINK_QUERY_PREFIX}_test1")
            mockDataLayer.remove("${Dispatch.Keys.DEEP_LINK_QUERY_PREFIX}_test2")
            mockDataLayer.remove("tealium_key1")
            mockDataLayer.remove("other_key")
        }
        queryParams.forEach {
            verify(inverse = true) {
                mockDataLayer.putString(
                    "deep_link_param_" + it.key,
                    it.value.toString(),
                    Expiry.SESSION
                )
            }
        }
    }
}