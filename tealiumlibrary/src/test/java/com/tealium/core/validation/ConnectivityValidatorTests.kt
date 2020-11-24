package com.tealium.core.validation

import com.tealium.core.network.ConnectivityRetriever
import com.tealium.core.settings.LibrarySettings
import com.tealium.dispatcher.TealiumEvent
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConnectivityValidatorTests {

    private val dispatch = TealiumEvent("", emptyMap())
    private lateinit var connectivityRetriever: ConnectivityRetriever
    private lateinit var librarySettings: LibrarySettings

    @Before
    fun setUp() {
        connectivityRetriever = mockk()
        librarySettings = mockk()
    }

    @Test
    fun testShouldQueueWhenNoConnectivity() {
        every { connectivityRetriever.isConnected() } returns false
        every { librarySettings.wifiOnly } returns false

        val connectivityValidator = ConnectivityValidator(connectivityRetriever, librarySettings)
        assertTrue(connectivityValidator.shouldQueue(dispatch))
        assertFalse(connectivityValidator.shouldDrop(dispatch))
    }

    @Test
    fun testShouldNotQueueWhenHasConnectivity() {
        every { connectivityRetriever.isConnected() } returns true
        every { librarySettings.wifiOnly } returns false

        val connectivityValidator = ConnectivityValidator(connectivityRetriever, librarySettings)
        assertFalse(connectivityValidator.shouldQueue(dispatch))
        assertFalse(connectivityValidator.shouldDrop(dispatch))
    }

    @Test
    fun testShouldQueueWhenNoWifiConnectivity() {
        every { connectivityRetriever.isConnected() } returns true
        every { connectivityRetriever.isConnectedWifi() } returns false
        every { librarySettings.wifiOnly } returns true

        val connectivityValidator = ConnectivityValidator(connectivityRetriever, librarySettings)
        assertTrue(connectivityValidator.shouldQueue(dispatch))
        assertFalse(connectivityValidator.shouldDrop(dispatch))
    }

    @Test
    fun testShouldNotQueueWhenHasWifiConnectivity() {
        every { connectivityRetriever.isConnected() } returns true
        every { connectivityRetriever.isConnectedWifi() } returns true
        every { librarySettings.wifiOnly } returns true

        val connectivityValidator = ConnectivityValidator(connectivityRetriever, librarySettings)
        assertFalse(connectivityValidator.shouldQueue(dispatch))
        assertFalse(connectivityValidator.shouldDrop(dispatch))
    }
}