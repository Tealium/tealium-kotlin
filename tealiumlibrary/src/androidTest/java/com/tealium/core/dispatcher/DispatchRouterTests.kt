package com.tealium.core.dispatcher

import com.tealium.core.Collector
import com.tealium.core.settings.LibrarySettingsManager
import com.tealium.core.Logger
import com.tealium.core.messaging.DispatchRouter
import com.tealium.core.messaging.EventDispatcher
import com.tealium.core.messaging.EventRouter
import com.tealium.core.settings.Batching
import com.tealium.core.settings.LibrarySettings
import com.tealium.core.persistence.DispatchStorage
import com.tealium.core.validation.BatchingValidator
import com.tealium.core.validation.ConnectivityValidator
import com.tealium.core.validation.DispatchValidator
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.Dispatcher
import com.tealium.dispatcher.Event
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.asCoroutineDispatcher
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executors

class DispatchRouterTests {

    private lateinit var dispatchRouter: DispatchRouter
    private lateinit var eventRouter: EventRouter

    @RelaxedMockK
    private lateinit var collector: Collector

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

    val coroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    val eventDispatch = Event("TestEvent")
    val eventDispatchList = mutableListOf<Dispatch>(eventDispatch, eventDispatch)

    var batching = spyk(Batching(batchSize = 1))
    val librarySettings = LibrarySettings(batching = batching)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        // Some default answers to get the Dispatch through the entire system.
        // Edit as required per-test to change route.
        coEvery { collector.collect() } returns mapOf("key" to "value")
        coEvery { collector.enabled } returns true
        coEvery { validator.enabled } returns true
        coEvery { validator2.enabled } returns true
        coEvery { dispatcher.enabled } returns true
        coEvery { batchingValidator.enabled } returns true
        coEvery { connectivityValidator.enabled } returns true
        every { validator.shouldDrop(eventDispatch) } returns false
        every { validator.shouldQueue(eventDispatch) } returns false
        every { validator2.shouldDrop(eventDispatch) } returns false
        every { validator2.shouldQueue(eventDispatch) } returns false
        every { batchingValidator.shouldDrop(eventDispatch) } returns false
        every { batchingValidator.shouldQueue(eventDispatch) } returns false
        every { connectivityValidator.shouldDrop(eventDispatch) } returns false
        every { connectivityValidator.shouldQueue(eventDispatch) } returns false

        every { librarySettingsManager.librarySettings } returns librarySettings
        every { dispatchStore.dequeue(-1) } returns eventDispatchList

        eventRouter = spyk(EventDispatcher())
        eventRouter.subscribe(dispatcher)

        dispatchRouter = DispatchRouter(coroutineDispatcher,
                setOf(collector),
                setOf(validator, validator2, batchingValidator, connectivityValidator),
                dispatchStore,
                librarySettingsManager,
                eventRouter)
    }

    @Test
    fun testIsCollected() {
        dispatchRouter.track(eventDispatch)

        coVerify {
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
    fun testIsNotValidated_WhenDisabled() {
        every { validator.enabled } returns false
        dispatchRouter.track(eventDispatch)

        coVerify(exactly = 0) {
            validator.shouldQueue(eventDispatch)
            validator.shouldDrop(eventDispatch)
        }

        coVerify(exactly = 1) {
            validator2.shouldQueue(eventDispatch)
            validator2.shouldDrop(eventDispatch)
        }
    }

    @Test
    fun testIsNotDispatched_WhenDisabled() {
        every { dispatcher.enabled } returns false
        dispatchRouter.track(eventDispatch)

        coVerify(exactly = 0) {
            dispatcher.onDispatchSend(eventDispatch)
            dispatcher.onBatchDispatchSend(eventDispatchList)
        }
    }

    @Test
    fun testIsDroppedWhenBothReturnTrue() {
        every { validator.shouldDrop(eventDispatch) } returns true
        every { validator2.shouldDrop(eventDispatch) } returns true
        dispatchRouter.track(eventDispatch)

        coVerify {
            collector.collect()
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

        coVerify {
            collector.collect()
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
            validator.shouldDrop(eventDispatch)
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
            validator.shouldDrop(eventDispatch)
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

        coVerify {
            collector.collect()
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

        coVerify {
            collector.collect()
            validator.shouldQueue(eventDispatch)
            eventRouter.onDispatchReady(eventDispatch)
            validator.shouldQueue(eventDispatch)
            eventRouter.onBatchDispatchSend(eventDispatchList.plus(eventDispatch))
            dispatcher.onBatchDispatchSend(eventDispatchList.plus(eventDispatch))
        }
    }

    @Test
    fun testLibrarySettingsAreFetchedOnlyOnce() {
        coEvery { librarySettingsManager.fetchLibrarySettings() } just Runs

        val oneEvent = listOf<Dispatch>(eventDispatch)
        dispatchRouter.sendDispatches(oneEvent)

        every { batching.batchSize } returns 3
        val oneBatch = listOf<Dispatch>(eventDispatch, eventDispatch, eventDispatch)
        dispatchRouter.sendDispatches(oneBatch)
        val twoBatches = listOf<Dispatch>(eventDispatch, eventDispatch, eventDispatch, eventDispatch, eventDispatch)
        dispatchRouter.sendDispatches(twoBatches)

        coVerify(exactly = 3, timeout = 500) {
            librarySettingsManager.fetchLibrarySettings()
        }
    }
}