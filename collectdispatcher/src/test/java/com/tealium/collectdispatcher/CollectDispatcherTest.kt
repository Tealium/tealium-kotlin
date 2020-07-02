//package com.tealium.collectdispatcher
//
//import android.app.Application
//import android.util.Log
//import com.tealium.core.Encoder
//import com.tealium.core.Environment
//import com.tealium.core.NetworkClient
//import com.tealium.core.TealiumConfig
//import com.tealium.core.network.NetworkClient
//import com.tealium.dispatcher.ViewDispatch
//import io.mockk.*
//import io.mockk.impl.annotations.MockK
//import junit.framework.Assert.assertEquals
//import kotlinx.coroutines.runBlocking
//import org.junit.Before
//import org.junit.Test
//import java.io.File
//
//class CollectDispatcherTest {
//
//    @MockK
//    lateinit var mockEncoder: Encoder
//
//    @MockK
//    lateinit var mockClient: NetworkClient
//
//    @MockK
//    lateinit var context: Application
//
//    @MockK
//    lateinit var mockFile: File
//
//    lateinit var config: TealiumConfig
//    lateinit var collectDispatcher: CollectDispatcher

//    @Before
//    fun setUp() {
//        MockKAnnotations.init(this)
//        every { mockClient.networkClientListener = any() } just Runs
//        mockkConstructor(TealiumConfig::class)
//        every { context.filesDir } returns mockFile
//        every { anyConstructed<TealiumConfig>().tealiumDirectory.mkdir() } returns mockk()
//        config = TealiumConfig(context, "test", "profile", Environment.QA)
//        collectDispatcher = CollectDispatcher(config, mockEncoder, mockClient, null)
//        mockkStatic(Log::class)
//        every { Log.v(any(), any()) } returns 0
//    }
//
//    @Test
//    fun collectDispatcherHasValidDefaultCollectUrl() {
//        assertEquals(Collect.COLLECT_URL.value, collectDispatcher.urlString)
//    }
//
//    @Test
//    fun overrideCollectUrlIsSet() {
//        config.optionalConfigFlags[COLLECT_OVERRIDE_URL] = "test_url"
//        assertEquals("test_url", collectDispatcher.urlString)
//    }
//
//    @Test
//    fun sendDispatchCallsNetworkClient() = runBlocking {
//        val viewDispatch = ViewDispatch("test_view")
//        every { mockEncoder.encode(viewDispatch) } returns "key=value"
//        every { mockEncoder.encode(config) } returns "account=test"
//        coEvery { mockClient.post("key=value&account=test", Collect.COLLECT_URL.value) } just Runs
//
//        excludeRecords { mockClient.networkClientListener = any() }
//
//        runBlocking {
//            collectDispatcher.send(viewDispatch)
//        }
//
//        coVerify {
//            mockClient.post("key=value&account=test", Collect.COLLECT_URL.value)
//        }
//
//        confirmVerified(mockClient)
//    }
//}