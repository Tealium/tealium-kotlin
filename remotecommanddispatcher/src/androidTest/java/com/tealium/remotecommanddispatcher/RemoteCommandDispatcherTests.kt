package com.tealium.remotecommanddispatcher

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.Environment
import com.tealium.core.JsonUtils
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.network.NetworkClient
import com.tealium.dispatcher.EventDispatch
import io.mockk.*
import io.mockk.impl.annotations.MockK
import junit.framework.Assert
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
    lateinit var response: Response

    lateinit var context: Application
    lateinit var config: TealiumConfig
    private val tealiumContext = mockk<TealiumContext>()
    lateinit var jsonRemoteCommand: RemoteCommand
    lateinit var webViewRemoteCommand: RemoteCommand

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        context = ApplicationProvider.getApplicationContext()

        mockkConstructor(RemoteCommand::class)
        every { anyConstructed<RemoteCommand>().invoke(any()) } just Runs

        config = TealiumConfig(context, "test", "profile", Environment.DEV)
        every { tealiumContext.config } returns config
    }

    @Test
    fun validAddAndProcessJsonRemoteCommand() {
        val remoteCommandDispatcher = RemoteCommandDispatcher(tealiumContext, mockk(), mockNetworkClient)
        val jsonCommand = mockk<RemoteCommand>()
        every { jsonCommand.commandId } returns "123"
        every { jsonCommand.type } returns RemoteCommandType.JSON
        every { jsonCommand.filename } returns null
        every { jsonCommand.remoteUrl } returns null
        every { jsonCommand.invoke(any()) } just Runs

        remoteCommandDispatcher.add(jsonCommand)
        val dispatch = EventDispatch("event_test", mapOf("key1" to "value1", "key2" to "value2"))
        val request = RemoteCommandRequest.jsonRequest(jsonCommand, JsonUtils.jsonFor(dispatch.payload()))
        remoteCommandDispatcher.processJsonRequest(request)

        verify { jsonCommand.invoke(any()) }
    }

    @Test
    fun validAddAndProcessWebViewRemoteCommand() {
        val remoteCommandDispatcher = RemoteCommandDispatcher(tealiumContext, mockk(), mockNetworkClient)
        val webViewCommand = mockk<RemoteCommand>()
        every { webViewCommand.commandId } returns "testWebViewCommand"
        every { webViewCommand.type } returns RemoteCommandType.WEBVIEW
        every { webViewCommand.invoke(any()) } just Runs

        remoteCommandDispatcher.add(webViewCommand)
        val request = RemoteCommandRequest.tagManagementRequest("tealium://testWebViewCommand?request={\"config\":{\"response_id\":\"123\"}, \"payload\":{\"hello\": \"world\"}}")
        remoteCommandDispatcher.processTagManagementRequest(request)

        verify { webViewCommand.invoke(any()) }
    }

    @Test
    fun httpRemoteCommandValid() {
        val remoteCommand = object : RemoteCommand("_http", description = "Perform a native HTTP operation") {
            override fun onInvoke(response: Response) { // invoke block
            }
        }

        val httpRemoteCommand = HttpRemoteCommand(mockNetworkClient)

        Assert.assertEquals(remoteCommand.commandId, httpRemoteCommand.commandId)
        Assert.assertEquals(remoteCommand.description, httpRemoteCommand.description)
    }

}