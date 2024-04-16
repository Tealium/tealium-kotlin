package com.tealium.remotecommanddispatcher

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.remotecommands.RemoteCommand
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemoteCommandsManagerTests {

    private val testCommandName = "test-command"

    @MockK
    private lateinit var mockCommand: RemoteCommand

    private lateinit var config: TealiumConfig
    private lateinit var context: Application

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        context = ApplicationProvider.getApplicationContext()
        config = TealiumConfig(context, "test", "test", Environment.DEV)

        every { mockCommand.commandName } returns testCommandName
    }

    @Test
    fun add_Prefers_RemoteUrls_To_Asset_WhenBothAreProvided() {
        val manager = RemoteCommandsManager(config)

        manager.add(mockCommand, "test-file.json", "test-url.com")

        assertTrue(manager.getRemoteCommandConfigRetriever(testCommandName) is UrlRemoteCommandConfigRetriever)
    }

}