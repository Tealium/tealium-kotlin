package com.tealium.hosteddatalayer

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.Environment
import com.tealium.core.Tealium
import com.tealium.core.TealiumConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class ModuleTests {

    val application = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun extension_ModuleFactoryReturnsModule() = runBlocking {
        val config = TealiumConfig(application, "tealiummobile", "test", Environment.DEV)
        config.modules.add(HostedDataLayer
        )
        val tealium = Tealium.create("test", config)
        delay(1500)
        assertNotNull(tealium.hostedDataLayer)
    }
}