package com.tealium.remotecommanddispatcher

import android.app.Application
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.network.NetworkClient
import com.tealium.dispatcher.TealiumEvent
import com.tealium.remotecommanddispatcher.remotecommands.HttpRemoteCommand
import com.tealium.remotecommands.RemoteCommand
import com.tealium.remotecommands.RemoteCommandRequest
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class RemoteCommandDispatcherTests {

    @MockK
    lateinit var mockNetworkClient: NetworkClient

    @MockK
    lateinit var mockRemoteCommandsManager: CommandsManager

    @MockK
    lateinit var context: Application

    @MockK
    lateinit var mockFile: File

    lateinit var config: TealiumConfig
    private val tealiumContext = mockk<TealiumContext>()
    lateinit var remoteCommandConfigRetriever: RemoteCommandConfigRetriever
    lateinit var remoteCommandConfig: RemoteCommandConfig


    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { context.filesDir } returns mockFile

        config = mockk()
        every { config.application } returns context
        every { tealiumContext.config } returns config

        remoteCommandConfigRetriever = mockk()
        remoteCommandConfig = RemoteCommandConfig(mapOf("testkey" to "testValue"), mapOf("testkey" to "testValue"), mapOf("event_test" to "testValue"))
    }

    @Test
    fun validAddAndProcessJsonRemoteCommand() {
        val remoteCommandDispatcher = RemoteCommandDispatcher(tealiumContext, mockNetworkClient, mockRemoteCommandsManager)
        val remoteCommand = spyk(TestCommand())

        every { mockRemoteCommandsManager.add(any(), any(), any()) } just Runs
        every { mockRemoteCommandsManager.getRemoteCommandConfigRetriever(any()) } returns remoteCommandConfigRetriever
        every { mockRemoteCommandsManager.getJsonRemoteCommands() } returns listOf(remoteCommand)
        every { remoteCommandConfigRetriever.remoteCommandConfig } returns remoteCommandConfig

        remoteCommandDispatcher.add(remoteCommand, "remotecommand.json")
        val dispatch = TealiumEvent("event_test", mapOf("key1" to "value1", "key2" to "value2"))
        remoteCommandDispatcher.onProcessRemoteCommand(dispatch)

        verify { remoteCommand.onInvoke(any()) }
    }

    @Test
    fun validAddAndProcessWebViewRemoteCommand() {
        val remoteCommandDispatcher = RemoteCommandDispatcher(tealiumContext, mockNetworkClient)
        val webViewCommand = spyk(TestCommand())

        remoteCommandDispatcher.add(webViewCommand)
        remoteCommandDispatcher.onRemoteCommandSend(RemoteCommandRequest(createResponseHandler(), "tealium://test?request={\"config\":{\"response_id\":\"123\"}, \"payload\":{\"hello\": \"world\"}}"))

        verify { webViewCommand.onInvoke(any()) }
    }

    @Test
    fun httpRemoteCommandValid() {
        val remoteCommand = object : RemoteCommand("_http", "Perform a native HTTP operation") {
            override fun onInvoke(response: Response) { // invoke block
            }
        }

        val httpRemoteCommand = HttpRemoteCommand(mockNetworkClient)

        assertEquals(remoteCommand.commandName, httpRemoteCommand.commandName)
        assertEquals(remoteCommand.description, httpRemoteCommand.description)
    }

    @Test
    fun onDispatchSend_Triggers_CommandConfig_Refresh() = runBlocking {
        val remoteCommandDispatcher = RemoteCommandDispatcher(tealiumContext, mockNetworkClient, mockRemoteCommandsManager)
        every { mockRemoteCommandsManager.getJsonRemoteCommands() } returns emptyList()
        every { mockRemoteCommandsManager.refreshConfig() } just Runs

        remoteCommandDispatcher.onDispatchSend(mockk())

        verify {
            mockRemoteCommandsManager.refreshConfig()
        }
    }

    private fun createResponseHandler(): RemoteCommand.ResponseHandler {
        return RemoteCommand.ResponseHandler {
            // do nothing
        }
    }
}

open class TestCommand : RemoteCommand("test", "description") {
    public override fun onInvoke(p0: Response?) {

    }
}