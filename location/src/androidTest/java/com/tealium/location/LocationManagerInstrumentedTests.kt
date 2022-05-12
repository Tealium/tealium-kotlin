package com.tealium.location

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.Environment
import com.tealium.core.Tealium
import com.tealium.core.TealiumConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class LocationManagerInstrumentedTests {

    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun modules_DoesNotReturnNull() = runBlocking {
        val config = TealiumConfig(
            application, "tealiummobile", "test", Environment.DEV,
            collectors = mutableSetOf()
        )
        config.collectors.add(LocationManager)
        val tealium = Tealium.create("test", config)
        delay(1500)
        assertNotNull(tealium.location)
    }
}