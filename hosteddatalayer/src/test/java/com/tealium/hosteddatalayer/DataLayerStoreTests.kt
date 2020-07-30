package com.tealium.hosteddatalayer

import com.tealium.core.TealiumConfig
import com.tealium.core.network.Connectivity
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DataLayerStoreTests {

    @MockK
    lateinit var mockConfig: TealiumConfig
    @MockK
    lateinit var mockDirectory: File
    @MockK
    lateinit var mockConnectivity: Connectivity


    lateinit var store: DataLayerStore

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        // reasonable defaults
        every {  }
        every { mockConnectivity.isConnected() } returns true
        every { mockConnectivity.isConnectedWifi() } returns true


        store = DataLayerStore()
    }

    @Test
    fun test


    // TODO - test all DAO methods
    // TODO - test resize + max cache
    // TODO - test expiry
}