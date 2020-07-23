package com.tealium.remotecommanddispatcher

import android.app.Application
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.network.NetworkClient
import com.tealium.dispatcher.EventDispatch
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test

class RemoteCommandDispatcherTests {

    @MockK
    lateinit var mockNetworkClient: NetworkClient

    @MockK
    lateinit var mockRemoteCommandRequest: RemoteCommandRequest

    @MockK
    lateinit var mockRemoteCommandConfigRetriever: RemoteCommandConfigRetriever

    @MockK
    lateinit var mockResponse: Response

    lateinit var context: Application
    lateinit var config: TealiumConfig
    private val tealiumContext = mockk<TealiumContext>()
    lateinit var jsonRemoteCommand: RemoteCommand
    lateinit var webViewRemoteCommand: RemoteCommand

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        context = ApplicationProvider.getApplicationContext()

        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0

        config = TealiumConfig(context, "test", "profile", Environment.DEV)
        every { tealiumContext.config } returns config
        jsonRemoteCommand = object : RemoteCommand("testJsonCommand", "Testing JSON Remote Command", RemoteCommandType.JSON, filename = "test.json") {
            override fun onInvoke(response: Response) { // invoke block...
            }
        }

        webViewRemoteCommand = object : RemoteCommand("testWebViewCommand", "Testing WebView Remote Command", RemoteCommandType.WEBVIEW) {
            override fun onInvoke(response: Response) { //invoke block...
            }
        }

        //every { jsonRemoteCommand.invoke(mockRemoteCommandRequest) } just Runs
    }

    @Test
    fun validAddAndProcessJsonRemoteCommand() {
        val remoteCommandDispatcher = RemoteCommandDispatcher(tealiumContext, mockk(), mockNetworkClient)
        val jsonCommand = object : RemoteCommand("testJsonCommand", "Testing JSON Remote Command", RemoteCommandType.JSON, filename = "test.json") {
            override fun onInvoke(response: Response) { // invoke block...
            }
        }
        val dispatch = EventDispatch("event_test", mapOf("key1" to "value1", "key2" to "value2"))

        remoteCommandDispatcher.add(jsonCommand)
        remoteCommandDispatcher.onProcessRemoteCommand(dispatch)

        mockkStatic(RemoteCommandRequest::class)
        every { RemoteCommandRequest }

        verify { jsonCommand.invoke(mockRemoteCommandRequest) }
    }

    @Test
    fun validAddAndProcessWebViewRemoteCommand() {
        val remoteCommandDispatcher = RemoteCommandDispatcher(tealiumContext, mockk(), mockNetworkClient)
        val webViewCommand = object : RemoteCommand("testWebViewCommand", "Testing WebView Remote Command", RemoteCommandType.WEBVIEW) {
            override fun onInvoke(response: Response) { //invoke block...
            }
        }
        remoteCommandDispatcher.add(webViewCommand)
        remoteCommandDispatcher.onRemoteCommandSend("tealium://testWebViewCommand?request={\"config\":{\"response_id\":\"123\"}, \"payload\":{\"hello\": \"world\"}}")

        verify { webViewCommand.onInvoke(any()) }
    }

}