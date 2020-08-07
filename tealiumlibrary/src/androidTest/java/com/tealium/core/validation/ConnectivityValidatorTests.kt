package com.tealium.core.validation

import com.tealium.core.network.ConnectivityRetriever
import com.tealium.core.settings.LibrarySettings
import com.tealium.dispatcher.Event
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConnectivityValidatorTests {

    private val dispatch = Event("", emptyMap())
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