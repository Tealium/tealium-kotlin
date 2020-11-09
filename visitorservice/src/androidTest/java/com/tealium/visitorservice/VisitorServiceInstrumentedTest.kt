package com.tealium.visitorservice

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.*
import com.tealium.core.messaging.EventRouter
import com.tealium.core.messaging.MessengerService
import com.tealium.core.network.HttpClient
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Before
import org.junit.Test

class VisitorServiceInstrumentedTest {

    lateinit var tealiumContext: TealiumContext
    val application = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        val config = TealiumConfig(application, "account", "profile", Environment.DEV)
        val mockEventRouter: EventRouter = mockk()
        every { mockEventRouter.subscribe(any()) } just Runs
        val messengerService = MessengerService(mockEventRouter, CoroutineScope(Dispatchers.IO))
        tealiumContext = TealiumContext(config, "visitor-1", Logger, mockk(), HttpClient(config), messengerService, mockk())
    }

    @Test
    fun factory_ReturnsNewInstance() {
        val visitorService1 = VisitorService.create(tealiumContext)
        val visitorService2 = VisitorService.create(tealiumContext)

        assertNotNull(visitorService1)
        assertNotNull(visitorService2)
        assertNotSame(visitorService1, visitorService2)
    }

    @Test
    fun extension_ReturnsModule() = runBlocking {
        val config = TealiumConfig(application, "tealiummobile", "test", Environment.DEV)
        config.modules.add(Modules.VisitorService)
        val tealium = Tealium.create("test", config)

        delay(1500)
        assertNotNull(tealium.visitorService)
    }
}