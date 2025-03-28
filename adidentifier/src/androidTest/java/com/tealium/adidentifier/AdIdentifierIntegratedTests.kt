package com.tealium.adidentifier

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.*
import com.tealium.core.persistence.DataLayer
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class AdIdentifierIntegratedTests {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    lateinit var tealiumContext: TealiumContext
    lateinit var config: TealiumConfig
    lateinit var dataLayer: DataLayer

    @Before
    fun setUp() {
        config =
            TealiumConfig(
                application,
                "test",
                "test",
                Environment.DEV,
                collectors = mutableSetOf()
            )

        dataLayer = mockk(relaxed = true)
        tealiumContext = TealiumContext(
            config,
            "someTestId",
            mockk(),
            dataLayer,
            mockk(),
            mockk(),
            mockk()
        )
    }

    @Test
    fun extension_ModuleFactoryReturnsModule() = runBlocking {
        config.modules.add(AdIdentifier)
        val tealium = Tealium.create("test", config)
        delay(1500)
        Assert.assertNotNull(tealium.adIdentifier)
    }
}