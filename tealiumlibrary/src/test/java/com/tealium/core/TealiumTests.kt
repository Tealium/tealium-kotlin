package com.tealium.core

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.settings.LibrarySettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TealiumTests {
    lateinit var tealium: Tealium
    val application = ApplicationProvider.getApplicationContext<Application>()
    val configWithNoModules = TealiumConfig(
        application,
        "test",
        "test",
        Environment.DEV
    )

    @Before
    fun setUp() {
        tealium = Tealium.create("name", configWithNoModules)
    }

    @After
    fun tearDown() {
        Tealium.names().forEach {
            Tealium[it]?.dataLayer?.clear()
            Tealium.destroy(it)
        }
    }

    @Test
    fun testConfig_LogLevel_Uses_CachedLibrarySettings_When_Present() {
        val mockLoader = mockk<JsonLoader>()
        mockkObject(JsonLoader.Companion)
        every { JsonLoader.Companion.getInstance(any()) } returns mockLoader
        every { mockLoader.loadFromFile(any()) } returns """
            { "log_level": "qa" }
        """.trimIndent()
        every { mockLoader.loadFromAsset(any()) } returns null

        Tealium.create(
            "loglevel", TealiumConfig(
                application,
                configWithNoModules.accountName,
                configWithNoModules.profileName,
                Environment.PROD
            ).apply { useRemoteLibrarySettings = true }
        )
        Assert.assertEquals(Logger.logLevel, LogLevel.QA)

        unmockkObject(JsonLoader.Companion)
    }

    @Test
    fun testConfig_LogLevel_Uses_AssetLibrarySettings_When_Present() {
        val mockLoader = mockk<JsonLoader>()
        mockkObject(JsonLoader)
        every { JsonLoader.getInstance(any()) } returns mockLoader
        every { mockLoader.loadFromFile(any()) } returns null
        every { mockLoader.loadFromAsset(any()) } returns """
            { "log_level": "qa" }
        """.trimIndent()

        Tealium.create(
            "loglevel", TealiumConfig(
                application,
                configWithNoModules.accountName,
                configWithNoModules.profileName,
                Environment.PROD
            ).apply { useRemoteLibrarySettings = false }
        )
        Assert.assertEquals(Logger.logLevel, LogLevel.QA)

        unmockkObject(JsonLoader)
    }

    @Test
    fun testConfig_LogLevel_Uses_OverrideDefaultLibrarySettings_When_Present() {
        Tealium.create(
            "loglevel", TealiumConfig(
                application,
                configWithNoModules.accountName,
                configWithNoModules.profileName,
                Environment.PROD
            ).apply { overrideDefaultLibrarySettings = LibrarySettings(logLevel = LogLevel.DEV) }
        )
        Assert.assertEquals(Logger.logLevel, LogLevel.DEV)
    }

    @Test
    fun testConfig_LogLevel_IsNotOverriddenByRemote_When_Config_IsSet() = runBlocking {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                    { "log_level": "qa" }
                """.trimIndent()
            )
        )

        mockWebServer.start()
        val url = mockWebServer.url("/tealium-settings.json")

        Tealium.create(
            "loglevel", TealiumConfig(
                application,
                configWithNoModules.accountName,
                configWithNoModules.profileName,
                Environment.PROD
            ).apply {
                logLevel = LogLevel.DEV
                useRemoteLibrarySettings = true
                overrideLibrarySettingsUrl = url.toString()
            }
        )
        Assert.assertEquals(Logger.logLevel, LogLevel.DEV)
        delay(2500)
        Assert.assertEquals(Logger.logLevel, LogLevel.DEV)
    }

}