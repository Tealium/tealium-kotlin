package com.tealium.crashreporter

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class CrashReporterInstrumentedTests {

    val application = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun extension_ModuleFactoryReturnsModule() = runBlocking {
        val config = TealiumConfig(
            application, "tealiummobile", "test", Environment.DEV,
            collectors = mutableSetOf()
        )
        config.modules.add(CrashReporter)
        val tealium = Tealium.create("test", config)
        delay(1500)
        assertNotNull(tealium.crashReporter)
    }
}