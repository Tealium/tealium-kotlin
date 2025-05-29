package com.tealium.core.consent

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.DispatcherFactory
import com.tealium.core.Environment
import com.tealium.core.Tealium
import com.tealium.core.TealiumConfig
import com.tealium.core.awaitCreateTealium
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.Dispatcher
import com.tealium.dispatcher.TealiumEvent
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ConsentLoggingTests {

    @RelaxedMockK
    lateinit var dispatcherFactory: DispatcherFactory

    @RelaxedMockK
    lateinit var dispatcher: Dispatcher

    lateinit var tealium: Tealium
    lateinit var application: Application

    @Before
    fun setUp() = runBlocking {
        MockKAnnotations.init(this@ConsentLoggingTests)

        application = ApplicationProvider.getApplicationContext()
        every { dispatcher.name } returns "dispatcher"
        every { dispatcher.enabled } returns true
        every { dispatcherFactory.create(any(), any()) } returns dispatcher

        val config = TealiumConfig(
            application,
            "test",
            "test",
            Environment.DEV,
            dispatchers = mutableSetOf(dispatcherFactory)
        ).apply {
            consentManagerLoggingEnabled = true
            consentManagerPolicy = ConsentPolicy.GDPR
        }
        tealium = awaitCreateTealium("test", config)
    }

    @Test
    fun dispatcher_Receives_Consent_Granted_Event_When_Consent_Granted() = runTest {
        tealium.consentManager.userConsentStatus = ConsentStatus.CONSENTED

        coVerify(timeout = 1000) {
            dispatcher.onDispatchSend(match { it[Dispatch.Keys.TEALIUM_EVENT] == ConsentManagerConstants.GRANT_FULL_CONSENT })
        }
    }

    @Test
    fun dispatcher_Receives_Partial_Consent_Granted_Event_When_Consent_Partially_Granted() = runTest {
        tealium.consentManager.userConsentCategories = setOf(ConsentCategory.CDP)

        coVerify(timeout = 1000) {
            dispatcher.onDispatchSend(match { it[Dispatch.Keys.TEALIUM_EVENT] == ConsentManagerConstants.GRANT_PARTIAL_CONSENT })
        }
    }

    @Test
    fun dispatcher_Receives_Decline_Consent_Event_When_Consent_Declined() = runTest {
        tealium.consentManager.userConsentStatus = ConsentStatus.NOT_CONSENTED

        coVerify(timeout = 1000) {
            dispatcher.onDispatchSend(match { it[Dispatch.Keys.TEALIUM_EVENT] == ConsentManagerConstants.DECLINE_CONSENT })
        }
    }

    @Test
    fun dispatcher_Receives_Other_Queued_Events_When_Consent_Granted() = runTest {
        val queuedEvent = TealiumEvent("other_event")
        tealium.track(queuedEvent)

        tealium.consentManager.userConsentStatus = ConsentStatus.CONSENTED

        coVerify(timeout = 1000) {
            dispatcher.onDispatchSend(match { it.id == queuedEvent.id })
        }
    }

    @Test
    fun dispatcher_Receives_Other_Queued_Events_When_Consent_Partially_Granted() = runTest {
        val queuedEvent = TealiumEvent("other_event")
        tealium.track(queuedEvent)

        tealium.consentManager.userConsentCategories = setOf(ConsentCategory.CDP)

        coVerify(timeout = 1000) {
            dispatcher.onDispatchSend(match { it.id == queuedEvent.id })
        }
    }

    @Test
    fun dispatcher_Does_Not_Receive_Other_Queued_Events_When_Consent_Declined() = runTest {
        val queuedEvent = TealiumEvent("other_event")
        tealium.track(queuedEvent)

        tealium.consentManager.userConsentStatus = ConsentStatus.NOT_CONSENTED

        coVerify(timeout = 1000, inverse = true) {
            dispatcher.onDispatchSend(match { it.id == queuedEvent.id })
        }
    }
}

