package com.tealium.core.dispatcher

import android.util.Log
import com.tealium.core.Collector
import com.tealium.core.settings.LibrarySettingsManager
import com.tealium.core.Logger
import com.tealium.core.Transformer
import com.tealium.core.consent.ConsentManager
import com.tealium.core.consent.ConsentStatus
import com.tealium.core.messaging.DispatchRouter
import com.tealium.core.messaging.EventDispatcher
import com.tealium.core.messaging.EventRouter
import com.tealium.core.network.Connectivity
import com.tealium.core.settings.Batching
import com.tealium.core.settings.LibrarySettings
import com.tealium.core.persistence.DispatchStorage
import com.tealium.core.validation.BatchingValidator
import com.tealium.core.validation.ConnectivityValidator
import com.tealium.core.validation.DispatchValidator
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.Dispatcher
import com.tealium.dispatcher.TealiumEvent
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
class DispatchRouterTests {

    private lateinit var dispatchRouter: DispatchRouter
    private lateinit var eventRouter: EventRouter

    @RelaxedMockK
    private lateinit var collector: Collector

    @RelaxedMockK
    private lateinit var transformer: Transformer

    @RelaxedMockK
    private lateinit var validator: DispatchValidator
    @RelaxedMockK
    private lateinit var validator2: DispatchValidator
    @RelaxedMockK
    private lateinit var batchingValidator: BatchingValidator
    @RelaxedMockK
    private lateinit var connectivityValidator: ConnectivityValidator

    @RelaxedMockK
    private lateinit var dispatcher: Dispatcher

    @RelaxedMockK
    private lateinit var dispatchStore: DispatchStorage

    @MockK
    private lateinit var librarySettingsManager: LibrarySettingsManager

    @MockK
    private lateinit var connectivity: Connectivity

    @MockK
    private lateinit var consentManager: ConsentManager

    val coroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    lateinit var eventDispatch : Dispatch
    lateinit var eventDispatchList : List<Dispatch>

    var batching = spyk(Batching(batchSize = 1))
    val librarySettings = LibrarySettings(batching = batching)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        eventDispatch =  TealiumEvent("TestEvent")
        eventDispatchList = mutableListOf<Dispatch>(eventDispatch, eventDispatch)

        // Some default answers to get the Dispatch through the entire system.
        // Edit as required per-test to change route.
        coEvery { collector.collect() } returns mapOf("key" to "value")
        coEvery { collector.enabled } returns true
        coEvery { transformer.transform(eventDispatch) } answers {
            eventDispatch.addAll(mapOf("transformed" to "value"))
        }
        coEvery { transformer.enabled } returns true
        coEvery { validator.enabled } returns true
        coEvery { validator2.enabled } returns true
        coEvery { dispatcher.enabled } returns true
        coEvery { batchingValidator.enabled } returns true
        coEvery { connectivityValidator.enabled } returns true
        coEvery { validator.shouldDrop(eventDispatch) } returns false
        coEvery { validator.shouldQueue(eventDispatch) } returns false
        coEvery { validator2.shouldDrop(eventDispatch) } returns false
        coEvery { validator2.shouldQueue(eventDispatch) } returns false
        coEvery { batchingValidator.shouldDrop(eventDispatch) } returns false
        coEvery { batchingValidator.shouldQueue(eventDispatch) } returns false
        coEvery { connectivityValidator.shouldDrop(eventDispatch) } returns false
        coEvery { connectivityValidator.shouldQueue(eventDispatch) } returns false

        coEvery { librarySettingsManager.fetchLibrarySettings() } just Runs
        every { librarySettingsManager.librarySettings } returns librarySettings
        excludeRecords { librarySettingsManager.librarySettings }
        every { dispatchStore.dequeue(-1) } returns eventDispatchList

        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        every { consentManager.enabled } returns true
        every { consentManager.userConsentStatus } returns ConsentStatus.CONSENTED
        every { connectivity.isConnected() } returns true

        eventRouter = spyk(EventDispatcher())
        eventRouter.subscribe(dispatcher)

        dispatchRouter = DispatchRouter(coroutineDispatcher,
                setOf(collector),
                setOf(transformer),
                setOf(validator, validator2, batchingValidator, connectivityValidator),
                dispatchStore,
                librarySettingsManager,
                connectivity,
                consentManager,
                eventRouter)
    }

    @Test
    fun testIsCollected() {
        dispatchRouter.track(eventDispatch)

        coVerify (timeout = 1000) {
            collector.collect()
        }
        assertTrue(eventDispatch["key"] == "value")
    }

    @Test
    fun testIsNotCollected_WhenDisabled() {
        every { collector.enabled } returns false
        dispatchRouter.track(eventDispatch)

        coVerify(exactly = 0) {
            collector.collect()
        }
    }

    @Test
    fun testIsTransformed() {
        assertNull(eventDispatch["transformed"])
        dispatchRouter.track(eventDispatch)

        coVerify (timeout = 1000) {
            transformer.transform(eventDispatch)
        }
        assertTrue(eventDispatch["transformed"] == "value")
    }

    @Test
    fun testIsNotTransformed_WhenDisabled() {
        assertNull(eventDispatch["transformed"])
        every { transformer.enabled } returns false
        dispatchRouter.track(eventDispatch)

        coVerify(timeout = 1000, exactly = 0) {
            transformer.transform(eventDispatch)
        }
        // still null
        assertNull(eventDispatch["transformed"])
    }

    @Test
    fun testIsNotValidated_WhenDisabled() {
        every { validator.enabled } returns false
        dispatchRouter.track(eventDispatch)

        coVerify(timeout = 1000, exactly = 0) {
            validator.shouldQueue(eventDispatch)
            validator.shouldDrop(eventDispatch)
        }

        coVerify(exactly = 1, timeout = 1000) {
            validator2.shouldQueue(eventDispatch)
            validator2.shouldDrop(eventDispatch)
        }
    }

    @Test
    fun testIsNotDispatched_WhenDisabled() {
        every { dispatcher.enabled } returns false
        dispatchRouter.track(eventDispatch)

        coVerify(timeout = 1000, exactly = 0) {
            dispatcher.onDispatchSend(eventDispatch)
            dispatcher.onBatchDispatchSend(eventDispatchList)
        }
    }

    @Test
    fun testIsDroppedWhenBothReturnTrue() {
        every { validator.shouldDrop(eventDispatch) } returns true
        every { validator2.shouldDrop(eventDispatch) } returns true
        dispatchRouter.track(eventDispatch)

        coVerify (timeout = 1000) {
            collector.collect()
            transformer.transform(eventDispatch)
            validator.shouldDrop(eventDispatch)
            eventRouter.onDispatchDropped(eventDispatch)
        }

        verify(exactly = 0) {
            // no further routing should happen
            validator.shouldQueue(eventDispatch)
        }
    }

    @Test
    fun testIsDroppedWhenOneReturnsTrue() {
        every { validator.shouldDrop(eventDispatch) } returns true
        dispatchRouter.track(eventDispatch)

        coVerify(timeout = 1000) {
            collector.collect()
            transformer.transform(eventDispatch)
            validator.shouldDrop(eventDispatch)
            eventRouter.onDispatchDropped(eventDispatch)
        }

        verify(exactly = 0) {
            // no further routing should happen
            validator.shouldQueue(eventDispatch)
        }
    }

    @Test
    fun testDroppingSourceIsLogged() {
        mockkObject(Logger)
        every { validator.name } returns "mock_validator"
        every { validator.shouldDrop(eventDispatch) } returns true
        dispatchRouter.track(eventDispatch)

        verify(timeout = 1000) {
            Logger.qa(any(), match { it.contains("Dropping") && it.endsWith("mock_validator") })
        }
    }

    @Test
    fun testIsQueuedWhenBothReturnTrue() {
        every { validator.shouldQueue(eventDispatch) } returns true
        every { validator2.shouldQueue(eventDispatch) } returns true
        dispatchRouter.track(eventDispatch)

        coVerify(timeout = 1000) {
            collector.collect()
            transformer.transform(eventDispatch)
            validator.shouldDrop(eventDispatch)
            eventRouter.onDispatchReady(eventDispatch)
            validator.shouldQueue(eventDispatch)
            dispatchStore.enqueue(eventDispatch)
            eventRouter.onDispatchQueued(eventDispatch)
        }

        verify(exactly = 0) {
            // no further routing should happen
            dispatchRouter.dequeue(eventDispatch)
        }
    }

    @Test
    fun testIsQueuedWhenOneReturnsTrue() {
        every { validator.shouldQueue(eventDispatch) } returns true
        dispatchRouter.track(eventDispatch)

        coVerify(timeout = 1000) {
            collector.collect()
            transformer.transform(eventDispatch)
            validator.shouldDrop(eventDispatch)
            eventRouter.onDispatchReady(eventDispatch)
            validator.shouldQueue(eventDispatch)
            dispatchStore.enqueue(eventDispatch)
            eventRouter.onDispatchQueued(eventDispatch)
        }

        verify(exactly = 0) {
            // no further routing should happen
            dispatchRouter.dequeue(eventDispatch)
        }
    }

    @Test
    fun testQueueingSourceIsLogged() {
        mockkObject(Logger)
        every { validator.name } returns "mock_validator"
        every { validator.shouldQueue(eventDispatch) } returns true
        dispatchRouter.track(eventDispatch)

        verify(timeout = 1000) {
            Logger.qa(any(), match { it.contains("Queueing") && it.endsWith("mock_validator") })
        }
    }

    @Test
    fun testOverrideClassIgnoresQueueingResult() {
        every { batchingValidator.shouldQueue(null) } returns true
        assertTrue(dispatchRouter.shouldQueue(null))
        assertFalse(dispatchRouter.shouldQueue(null, BatchingValidator::class.java))

        // should only override a specific class
        every { connectivityValidator.shouldQueue(null) } returns true
        assertTrue(dispatchRouter.shouldQueue(null))
        assertTrue(dispatchRouter.shouldQueue(null, BatchingValidator::class.java))
    }

    @Test
    fun testIsDispatched() {
        every { batching.batchSize } returns 1
        dispatchRouter.track(eventDispatch)

        coVerify (timeout = 1000) {
            collector.collect()
            transformer.transform(eventDispatch)
            validator.shouldDrop(eventDispatch)
            eventRouter.onDispatchReady(eventDispatch)
            validator.shouldQueue(eventDispatch)
            eventRouter.onDispatchSend(eventDispatch)
            dispatcher.onDispatchSend(eventDispatch)
        }
    }

    @Test
    fun testIsDispatchedInBatches() {
        every { batching.batchSize } returns 5
        dispatchRouter.track(eventDispatch)

        coVerify(timeout = 1000) {
            collector.collect()
            transformer.transform(eventDispatch)
            validator.shouldQueue(eventDispatch)
            eventRouter.onDispatchReady(eventDispatch)
            validator.shouldQueue(eventDispatch)
            eventRouter.onBatchDispatchSend(eventDispatchList.plus(eventDispatch))
            dispatcher.onBatchDispatchSend(eventDispatchList.plus(eventDispatch))
        }
    }

    @Test
    fun testLibrarySettingsAreFetchedOnlyOnce() {
        val oneEvent = listOf<Dispatch>(eventDispatch)
        dispatchRouter.sendDispatches(oneEvent)

        every { batching.batchSize } returns 3
        val oneBatch = listOf<Dispatch>(eventDispatch, eventDispatch, eventDispatch)
        dispatchRouter.sendDispatches(oneBatch)
        val twoBatches = listOf<Dispatch>(eventDispatch, eventDispatch, eventDispatch, eventDispatch, eventDispatch)
        dispatchRouter.sendDispatches(twoBatches)

        coVerify(exactly = 3, timeout = 1000) {
            librarySettingsManager.fetchLibrarySettings()
        }
    }
}