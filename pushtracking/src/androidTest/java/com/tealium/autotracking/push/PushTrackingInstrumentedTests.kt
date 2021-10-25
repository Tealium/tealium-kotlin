package com.tealium.autotracking.push

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.*
import com.tealium.core.messaging.EventRouter
import com.tealium.core.messaging.MessengerService
import com.tealium.core.network.HttpClient
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

import org.junit.Test

import org.junit.Assert.*
import org.junit.Before

class PushTrackingInstrumentedTests {

    @RelaxedMockK
    lateinit var mockTealium: Tealium

    @RelaxedMockK
    lateinit var mockEventRouter: EventRouter

    lateinit var tealiumContext: TealiumContext
    val application = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        val config = TealiumConfig(application, "account", "profile", Environment.DEV)
        val messengerService = MessengerService(mockEventRouter, CoroutineScope(Dispatchers.IO))
        tealiumContext = TealiumContext(config, "visitor-1", Logger, mockk(), HttpClient(config), messengerService, mockTealium)
    }

    @Test
    fun factory_ReturnsNewInstance() {
        val pushTracking1 = PushTracking.create(tealiumContext)
        val pushTracking2 = PushTracking.create(tealiumContext)

        assertNotNull(pushTracking1)
        assertNotNull(pushTracking2)
        assertNotSame(pushTracking1, pushTracking2)
    }

    @Test
    fun extension_ReturnsModule() = runBlocking {
        val config = TealiumConfig(application, "tealiummobile", "test", Environment.DEV)
        config.modules.add(Modules.PushTracking)
        val tealium = Tealium.create("test", config)

        delay(1500)
        assertNotNull(tealium.pushTracking)
    }
}