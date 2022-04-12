package com.tealium.lifecycle

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.Environment
import com.tealium.core.Tealium
import com.tealium.core.TealiumConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class LifecycleInstrumentedTest {

    val application = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun modules_AreNotNull() = runBlocking {
        val config = TealiumConfig(
            application, "tealiummobile", "test", Environment.DEV,
            collectors = mutableSetOf()
        )
        config.modules.add(Lifecycle)
        val tealium = Tealium.create("test", config)
        delay(1500)
        assertNotNull(tealium.lifecycle)
    }
}