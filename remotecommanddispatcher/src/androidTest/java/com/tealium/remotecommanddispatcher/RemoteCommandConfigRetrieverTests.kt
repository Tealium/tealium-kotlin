package com.tealium.remotecommanddispatcher

import android.app.Application
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.Environment
import com.tealium.core.Loader
import com.tealium.core.TealiumConfig
import com.tealium.core.network.NetworkClient
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.io.File

class RemoteCommandConfigRetrieverTests {
    @MockK
    lateinit var mockNetworkClient: NetworkClient

    @MockK
    lateinit var mockLoader: Loader

    @MockK
    lateinit var mockScope: CoroutineScope

    @MockK
    lateinit var mockFile: File

    lateinit var context: Application
    lateinit var config: TealiumConfig

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        context = ApplicationProvider.getApplicationContext()

        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        mockNetworkClient = mockk()

        config = TealiumConfig(context, "test", "profile", Environment.DEV)
    }

    @Test
    fun remoteCommandConfigValidLoadFromAsset() {
        every { mockLoader.loadFromAsset(any()) } returns null
        coEvery { mockNetworkClient.get(any()) } returns null
        // initialize RemoteCommandConfigRetriever
        val configRetriever = RemoteCommandConfigRetriever(config, "testCommandId", filename = "testFileName", client = mockNetworkClient, loader = mockLoader, backgroundScope = mockScope)
        val config = configRetriever.remoteCommandConfig

        verify {
            mockLoader.loadFromAsset(any())
        }
    }

    @Test
    fun remoteCommandConfigValidLoadFromCache() = runBlocking {
        every { mockLoader.loadFromFile(any()) } returns null
        coEvery { mockNetworkClient.get(any()) } returns null
        // initialize RemoteCommandConfigRetriever
        val configRetriever = RemoteCommandConfigRetriever(config, "testCommandId", remoteUrl = "testRemoteUrl", client = mockNetworkClient, loader = mockLoader, backgroundScope = this)
        val config = configRetriever.remoteCommandConfig

        verify {
            mockLoader.loadFromFile(any())
        }
    }
}