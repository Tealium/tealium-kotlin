package com.tealium.visitorservice

import com.tealium.core.ModuleFactory
import com.tealium.core.Modules
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.messaging.MessengerService
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VisitorServiceTest {

    @MockK
    lateinit var mockContext: TealiumContext

    @MockK
    lateinit var mockVisitorProfileManager: VisitorProfileManager

    @MockK
    lateinit var mockConfig: TealiumConfig

    @MockK
    lateinit var mockMessengerService: MessengerService

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { mockContext.config } returns mockConfig
        every { mockContext.visitorId } returns "visitor1"
        every { mockContext.events } returns mockMessengerService
        every { mockMessengerService.subscribe(any()) } just Runs
    }

    @Test
    fun visitorService_VisitorProfile_TakenFromManager() {
        val visitorProfile = VisitorProfile(totalEventCount = 100)
        every { mockVisitorProfileManager.visitorProfile } returns visitorProfile
        val visitorService = VisitorService(mockContext, mockVisitorProfileManager)

        assertEquals(visitorProfile, visitorService.visitorProfile)
        assertEquals(visitorProfile.totalEventCount, visitorService.visitorProfile.totalEventCount)
    }

    @Test
    fun visitorService_SubscribesManagerToEvents() {
        val visitorManager: VisitorManager = mockk()
        val visitorService = VisitorService(mockContext, visitorManager)

        verify {
            mockMessengerService.subscribe(visitorManager)
        }
    }

    @Test
    fun visitorService_RequestsUpdatedProfile() = runBlocking {
        every { mockMessengerService.send(any<RequestVisitorProfileMessenger>()) } just Runs
        VisitorService(mockContext, mockVisitorProfileManager).requestVisitorProfile()

        verify {
            mockMessengerService.send(any<RequestVisitorProfileMessenger>())
        }
    }

    @Test
    fun visitorService_ModulesReferencesFactory() {
        assertSame(VisitorService, Modules.VisitorService)
        assertTrue(VisitorService is ModuleFactory)
    }
}