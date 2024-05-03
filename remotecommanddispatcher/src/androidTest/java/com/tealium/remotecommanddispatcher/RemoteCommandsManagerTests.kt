package com.tealium.remotecommanddispatcher

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.remotecommands.RemoteCommand
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemoteCommandsManagerTests {

    private val testCommandName = "test-command"

    private lateinit var mockCommand: RemoteCommand

    private lateinit var config: TealiumConfig
    private lateinit var context: Application
    private lateinit var manager: RemoteCommandsManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        config = TealiumConfig(context, "test", "test", Environment.DEV)

        mockCommand = mockCommand()

        manager = RemoteCommandsManager(config)
    }

    @Test
    fun add_Creates_AssetProvider_When_Only_AssetName_Provided() {
        manager.add(mockCommand, "test-file.json", null)

        assertTrue(manager.getRemoteCommandConfigRetriever(testCommandName) is AssetRemoteCommandConfigRetriever)
    }

    @Test
    fun add_Creates_UrlProvider_When_Only_Url_Provided() {
        manager.add(mockCommand, null, "localhost")

        assertTrue(manager.getRemoteCommandConfigRetriever(testCommandName) is UrlRemoteCommandConfigRetriever)
    }

    @Test
    fun add_Prefers_RemoteUrls_To_Asset_WhenBothAreProvided() {
        manager.add(mockCommand, "test-file.json", "test-url.com")

        assertTrue(manager.getRemoteCommandConfigRetriever(testCommandName) is UrlRemoteCommandConfigRetriever)
    }

    @Test
    fun add_Adds_WebView_Command_When_NoUrl_Or_File_Provided() {
        manager.add(mockCommand)

        assertTrue(manager.getJsonRemoteCommands().isEmpty())
        assertEquals(mockCommand, manager.getRemoteCommand(mockCommand.commandName))
    }

    @Test
    fun remove_Removes_Command() {
        manager.add(mockCommand)

        assertEquals(mockCommand, manager.getRemoteCommand(mockCommand.commandName))

        manager.remove(mockCommand.commandName)

        assertNull(manager.getRemoteCommand(mockCommand.commandName))
    }

    @Test
    fun remove_Removes_Command_Config_Retriever() {
        manager.add(mockCommand, "test-file.json")

        assertNotNull(manager.getRemoteCommandConfigRetriever(mockCommand.commandName))

        manager.remove(mockCommand.commandName)

        assertNull(manager.getRemoteCommandConfigRetriever(mockCommand.commandName))
    }

    @Test
    fun removeAll_Removes_All_Commands() {
        manager.add(mockCommand)

        assertEquals(mockCommand, manager.getRemoteCommand(mockCommand.commandName))

        manager.removeAll()

        assertNull(manager.getRemoteCommand(mockCommand.commandName))
        assertTrue(manager.getJsonRemoteCommands().isEmpty())
    }

    @Test
    fun removeAll_Removes_All_Command_Config_Retrievers() {
        manager.add(mockCommand, "test-file.json")

        assertNotNull(manager.getRemoteCommandConfigRetriever(mockCommand.commandName))

        manager.removeAll()

        assertNull(manager.getRemoteCommandConfigRetriever(mockCommand.commandName))
    }

    @Test
    fun getJsonRemoteCommand_ReturnsOnly_JsonRemoteCommands() {
        val webViewCommand = mockCommand("webview-command")
        manager.add(webViewCommand)
        manager.add(mockCommand, "test-file.json")

        assertEquals(1, manager.getJsonRemoteCommands().size)
        assertEquals(mockCommand, manager.getJsonRemoteCommands()[0])

        assertEquals(webViewCommand, manager.getRemoteCommand(webViewCommand.commandName))
    }

    private fun mockCommand(name: String = testCommandName): RemoteCommand {
        val command = mockk<RemoteCommand>()
        every { command.commandName } returns name
        return command
    }
}