package com.tealium.core

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.persistence.DataLayer
import com.tealium.core.persistence.Expiry
import io.mockk.*
import junit.framework.Assert.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class TealiumTests {

    lateinit var tealium: Tealium

    val application = ApplicationProvider.getApplicationContext<Application>()
    val configWithNoModules = TealiumConfig(application,
            "test",
            "test",
            Environment.DEV)

    @Before
    fun setUp() {
        tealium = Tealium("name", configWithNoModules)
    }

    @Test
    fun testVisitorIdIsGenerated() {
        assertNotNull(tealium.visitorId)
        assertEquals(32, tealium.visitorId.length)
        assertEquals(tealium.visitorId, tealium.dataLayer.getString("tealium_visitor_id"))
    }

    @Test
    fun testCallbackGetsExecuted() {
        val block: Tealium.() -> Unit = mockk(relaxed = true)
        every { block(hint(Tealium::class).any()) } just Runs

        val tealium = Tealium("name", configWithNoModules, block)

        verify(timeout = 1000) {
            block(tealium)
        }
    }

    @Test
    fun testHandleDeepLink() {
        var mockDataLayer = mockk<DataLayer>(relaxed = true)
        tealium._dataLayer = mockDataLayer
        val builder = Uri.Builder()
        val queryParams = hashMapOf<String, Any>("_dfgsdftwet" to "sdgfdgfdfg",
                "_fbclid" to "1234567",
                "tealium" to "cdh",
                "bool" to true,
                "int" to 123)
        var uri = builder.scheme("https")
                .authority("tealium.com")
                .path("/")

        queryParams.forEach { entry ->
            uri.appendQueryParameter(entry.key, entry.value.toString())
        }
        val builtURI = uri.build()
        runBlocking {
            tealium.handleDeepLink(builtURI)
            delay(100)
        }

        verify(exactly = 1) { mockDataLayer.putString("deep_link_url", "${builtURI.toString()}", Expiry.SESSION) }
    }

    @Test
    fun testHandleDeepLinkDoesNotRunFromActivityResumeIfDisabled() {
        var config = configWithNoModules
        config.deepLinkTrackingEnabled = false
        var tealium = Tealium("name", config)
        var mockDataLayer = mockk<DataLayer>(relaxed = true)
        tealium._dataLayer = mockDataLayer
        val builder = Uri.Builder()
        val queryParams = hashMapOf<String, Any>("_dfgsdftwet" to "sdgfdgfdfg",
                "_fbclid" to "1234567",
                "tealium" to "cdh",
                "bool" to true,
                "int" to 123)
        var uri = builder.scheme("https")
                .authority("tealium.com")
                .path("/")

        queryParams.forEach { entry ->
            uri.appendQueryParameter(entry.key, entry.value.toString())
        }
        var activity: Activity = mockk()
        var intent = Intent()
        val builtURI = uri.build()
        intent.data = builtURI
        every { activity.intent } returns intent
        runBlocking {
            tealium.onActivityResumed(activity)
            delay(100)
        }

        verify(exactly = 0) { mockDataLayer.putString("deep_link_url", "${builtURI.toString()}", Expiry.SESSION) }
    }


    @Test
    fun testHandleDeepLinkFromActivityResume() {
        var mockDataLayer = mockk<DataLayer>(relaxed = true)
        tealium._dataLayer = mockDataLayer
        val builder = Uri.Builder()
        val queryParams = hashMapOf<String, Any>("_dfgsdftwet" to "sdgfdgfdfg",
                "_fbclid" to "1234567",
                "tealium" to "cdh",
                "bool" to true,
                "int" to 123)
        var uri = builder.scheme("https")
                .authority("tealium.com")
                .path("/")

        queryParams.forEach { entry ->
            uri.appendQueryParameter(entry.key, entry.value.toString())
        }
        var activity: Activity = mockk()
        var intent = Intent()
        val builtURI = uri.build()
        intent.data = builtURI
        every { activity.intent } returns intent
        runBlocking {
            tealium.onActivityResumed(activity)
            delay(100)
        }

        verify(exactly = 1) { mockDataLayer.putString("deep_link_url", "${builtURI.toString()}", Expiry.SESSION) }
    }

    @Test
    fun testHandleJoinTraceFromActivityResume() {
        var mockDataLayer = mockk<DataLayer>(relaxed = true)
        tealium._dataLayer = mockDataLayer
        val builder = Uri.Builder()
        val traceId = "abc123"
        val queryParams = hashMapOf<String, Any>("tealium_trace_id" to traceId)
        var uri = builder.scheme("https")
                .authority("tealium.com")
                .path("/")

        queryParams.forEach { entry ->
            uri.appendQueryParameter(entry.key, entry.value.toString())
        }
        var activity: Activity = mockk()
        var intent = Intent()
        val builtURI = uri.build()
        intent.data = builtURI
        every { activity.intent } returns intent
        runBlocking {
            tealium.onActivityResumed(activity)
            delay(100)
        }

        verify(exactly = 1) { mockDataLayer.putString("cp.trace_id", "$traceId", Expiry.SESSION) }
    }

    @Test
    fun testHandleJoinTraceDoesNotRunFromActivityResumeIfDisabled() {
        var config = configWithNoModules
        config.qrTraceEnabled = false
        var tealium = Tealium("name", config)
        var mockDataLayer = mockk<DataLayer>(relaxed = true)
        tealium._dataLayer = mockDataLayer
        val builder = Uri.Builder()
        val traceId = "abc123"
        val queryParams = hashMapOf<String, Any>("tealium_trace_id" to traceId)
        var uri = builder.scheme("https")
                .authority("tealium.com")
                .path("/")

        queryParams.forEach { entry ->
            uri.appendQueryParameter(entry.key, entry.value.toString())
        }
        var activity: Activity = mockk()
        var intent = Intent()
        val builtURI = uri.build()
        intent.data = builtURI
        every { activity.intent } returns intent
        runBlocking {
            tealium.onActivityResumed(activity)
            delay(100)
        }

        verify(exactly = 0) { mockDataLayer.putString("cp.trace_id", "$traceId", Expiry.SESSION) }
    }

    @Test
    fun testJoinTrace() {
        var mockDataLayer = mockk<DataLayer>(relaxed = true)
        tealium._dataLayer = mockDataLayer
        tealium.joinTrace("abc123")
        verify(exactly = 1) { mockDataLayer.putString("cp.trace_id", "abc123", Expiry.SESSION) }
    }

    @Test
    fun testLeaveTrace() {
        var mockDataLayer = mockk<DataLayer>(relaxed = true)
        tealium._dataLayer = mockDataLayer
        tealium.leaveTrace()
        verify(exactly = 1) { mockDataLayer.remove("cp.trace_id") }
    }

    @Test
    fun testKillVisitorSessionNotCalledIfLeaveTraceParameterFalse() {
        var mockTealium = spyk(tealium)
        mockTealium.leaveTrace(false)
        verify(exactly = 0) { mockTealium.killVisitorSession() }
    }

    @Test
    fun testKillVisitorSessionIsCalledIfLeaveTraceParamTrue() {
        var mockTealium = spyk(tealium)
        mockTealium.leaveTrace(true)
        verify(exactly = 1) { mockTealium.killVisitorSession() }
    }

    @Test
    fun testKillVisitorSessionTriggersTrackCall() {
        var mockTealium = spyk(tealium)
        mockTealium.killVisitorSession()
        verify(exactly = 1) {
            mockTealium.track(
                    withArg { dispatch ->
                        assertEquals("kill_visitor_session", dispatch.get("tealium_event_name"))
                        assertEquals("kill_visitor_session", dispatch.get("event"))
                    })
        }
    }

}