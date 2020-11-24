package com.tealium.core

import com.tealium.dispatcher.Dispatch
import io.mockk.*
import org.junit.Test

class TealiumContextTests {

    @Test
    fun testContextTrackCallsTealiumInstance() {
        val tealium = mockk<Tealium>()
        every { tealium.track(any()) } just Runs

        val context = TealiumContext(mockk(),
                "",
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                tealium)

        val dispatch = mockk<Dispatch>(relaxed = true)
        context.track(dispatch)
        verify {
            tealium.track(dispatch)
        }
    }
}