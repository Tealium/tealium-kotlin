package com.tealium.remotecommanddispatcher

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.network.NetworkClient
import com.tealium.dispatcher.TealiumEvent
import com.tealium.remotecommanddispatcher.remotecommands.HttpRemoteCommand
import com.tealium.remotecommanddispatcher.remotecommands.JsonRemoteCommand
import com.tealium.remotecommands.RemoteCommand
import com.tealium.remotecommands.RemoteCommandRequest
import io.mockk.*
import io.mockk.impl.annotations.MockK
import junit.framework.Assert
import org.junit.Before
import org.junit.Test

class RemoteCommandDispatcherTests {

    @MockK
    lateinit var mockNetworkClient: NetworkClient

    @MockK
    lateinit var mockRemoteCommandConfigRetriever: RemoteCommandConfigRetriever


    lateinit var context: Application
    lateinit var config: TealiumConfig
    private val tealiumContext = mockk<TealiumContext>()
    lateinit var mockRemoteCommandConfig: RemoteCommandConfig

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        context = ApplicationProvider.getApplicationContext()

        mockkConstructor(RemoteCommand::class)
        every { anyConstructed<RemoteCommand>().invoke(any()) } just Runs

        mockRemoteCommandConfig = RemoteCommandConfig(mapOf("testkey" to "testValue"), mapOf("testkey" to "testValue"), mapOf("event_test" to "testValue"))
        config = TealiumConfig(context, "test", "profile", Environment.DEV)
        every { tealiumContext.config } returns config

    }

    @Test
    fun validAddAndProcessJsonRemoteCommand() {
        val remoteCommandDispatcher = RemoteCommandDispatcher(tealiumContext, mockNetworkClient)
        val remoteCommand = spyk<RemoteCommand>(object : RemoteCommand("123", "testing Json") {
            override fun onInvoke(response: Response) { // invoke block
            }
        })

        every { remoteCommand.commandName } returns "123"

        mockkConstructor(JsonRemoteCommand::class)
        every { anyConstructed<JsonRemoteCommand>().id } returns "123"
        every { anyConstructed<JsonRemoteCommand>().filename } returns "remotecommand.json"
        every { anyConstructed<JsonRemoteCommand>().remoteUrl } returns null
        every { anyConstructed<JsonRemoteCommand>().remoteCommandConfigRetriever } returns mockRemoteCommandConfigRetriever
        every { mockRemoteCommandConfigRetriever?.remoteCommandConfig } returns mockRemoteCommandConfig
//        every { mockRemoteCommandConfig.mappings } returns mapOf("testkey" to "testValue")
//        every { mockRemoteCommandConfig.apiCommands?.get("event_test") } returns "test_command"

        remoteCommandDispatcher.add(remoteCommand, "remotecommand.json")
        val dispatch = TealiumEvent("event_test", mapOf("key1" to "value1", "key2" to "value2"))
        remoteCommandDispatcher.onProcessRemoteCommand(dispatch)

        verify { remoteCommand.invoke(any()) }
    }

    @Test
    fun validAddAndProcessWebViewRemoteCommand() {
        val remoteCommandDispatcher = RemoteCommandDispatcher(tealiumContext, mockNetworkClient)
        val webViewCommand = spyk<RemoteCommand>(object : RemoteCommand("testWebViewCommand", null) {
            override fun onInvoke(response: Response) { // invoke block
            }
        })

        remoteCommandDispatcher.add(webViewCommand)
        remoteCommandDispatcher.onRemoteCommandSend(RemoteCommandRequest(createResponseHandler(), "tealium://testWebViewCommand?request={\"config\":{\"response_id\":\"123\"}, \"payload\":{\"hello\": \"world\"}}"))

        verify { webViewCommand.invoke(any()) }
    }

    @Test
    fun httpRemoteCommandValid() {
        val remoteCommand = object : RemoteCommand("_http", "Perform a native HTTP operation") {
            override fun onInvoke(response: Response) { // invoke block
            }
        }

        val httpRemoteCommand = HttpRemoteCommand(mockNetworkClient)

        Assert.assertEquals(remoteCommand.commandName, httpRemoteCommand.commandName)
        Assert.assertEquals(remoteCommand.description, httpRemoteCommand.description)
    }

    private fun createResponseHandler(): RemoteCommand.ResponseHandler {
        return RemoteCommand.ResponseHandler {
            // do nothing
        }
    }
}