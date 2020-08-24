package com.tealium.remotecommanddispatcher

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.network.NetworkClient
import com.tealium.dispatcher.TealiumEvent
import com.tealium.internal.tagbridge.RemoteCommand
import com.tealium.remotecommanddispatcher.remotecommands.HttpRemoteCommand
import com.tealium.remotecommanddispatcher.remotecommands.JsonRemoteCommand
import io.mockk.*
import io.mockk.impl.annotations.MockK
import junit.framework.Assert
import org.junit.Before
import org.junit.Test

class RemoteCommandDispatcherTests {

    @MockK
    lateinit var mockNetworkClient: NetworkClient

    @MockK
    lateinit var mockRemoteCommandConfig: RemoteCommandConfig

    lateinit var context: Application
    lateinit var config: TealiumConfig
    private val tealiumContext = mockk<TealiumContext>()

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
        val jsonCommand = mockk<JsonRemoteCommand>()
        every { jsonCommand.commandName } returns "123"
        every { jsonCommand.filename } returns null
        every { jsonCommand.remoteUrl } returns null
        every { jsonCommand.invoke(any()) } just Runs
        every { jsonCommand.remoteCommandConfigRetriever?.remoteCommandConfig } returns mockRemoteCommandConfig
        every { mockRemoteCommandConfig.mappings } returns mapOf("testkey" to "testValue")
        every { mockRemoteCommandConfig.apiCommands?.get("event_test") } returns "test_command"

        remoteCommandDispatcher.add(jsonCommand)
        val dispatch = TealiumEvent("event_test", mapOf("key1" to "value1", "key2" to "value2"))
        remoteCommandDispatcher.onProcessRemoteCommand(dispatch)

        verify { jsonCommand.invoke(any()) }
    }

    @Test
    fun validAddAndProcessWebViewRemoteCommand() {
        val remoteCommandDispatcher = RemoteCommandDispatcher(tealiumContext, mockk(), mockNetworkClient)
        val webViewCommand = spyk<RemoteCommand>(object : RemoteCommand("testWebViewCommand", null) {
            override fun onInvoke(response: Response) { // invoke block
            }
        })

        remoteCommandDispatcher.add(webViewCommand)
        remoteCommandDispatcher.onRemoteCommandSend("tealium://testWebViewCommand?request={\"config\":{\"response_id\":\"123\"}, \"payload\":{\"hello\": \"world\"}}")

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
}