package com.tealium.core.dispatcher

import android.util.Log
import com.tealium.core.Collector
import com.tealium.core.Logger
import com.tealium.core.Transformer
import com.tealium.core.consent.ConsentManager
import com.tealium.core.messaging.DispatchRouter
import com.tealium.core.messaging.EventDispatcher
import com.tealium.core.messaging.EventRouter
import com.tealium.core.persistence.QueueingDao
import com.tealium.core.settings.Batching
import com.tealium.core.settings.LibrarySettings
import com.tealium.core.settings.LibrarySettingsManager
import com.tealium.core.validation.BatchingValidator
import com.tealium.core.validation.BatteryValidator
import com.tealium.core.validation.ConnectivityValidator
import com.tealium.core.validation.DispatchValidator
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.Dispatcher
import com.tealium.dispatcher.TealiumEvent
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.excludeRecords
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.LinkedList
import java.util.Queue
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
    private lateinit var batteryValidator: BatteryValidator

    @RelaxedMockK
    private lateinit var consentManager: ConsentManager

    @RelaxedMockK
    private lateinit var dispatcher: Dispatcher

    private lateinit var dispatchStore: QueueingDao<String, Dispatch>

    @MockK
    private lateinit var librarySettingsManager: LibrarySettingsManager

    val coroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    lateinit var eventDispatch: Dispatch
    lateinit var queuedEvents: List<Dispatch>

    var batching = spyk(Batching(batchSize = 1))
    val librarySettings = LibrarySettings(batching = batching)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        eventDispatch = TealiumEvent("TestEvent")
        queuedEvents = createDispatches(2)
        dispatchStore = spyk(VolatileDispatchStore(queuedEvents))

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
        coEvery { batteryValidator.enabled } returns true
        coEvery { consentManager.enabled } returns true
        coEvery { validator.shouldDrop(eventDispatch) } returns false
        coEvery { validator.shouldQueue(eventDispatch) } returns false
        coEvery { validator2.shouldDrop(eventDispatch) } returns false
        coEvery { validator2.shouldQueue(eventDispatch) } returns false
        coEvery { batchingValidator.shouldDrop(eventDispatch) } returns false
        coEvery { batchingValidator.shouldQueue(eventDispatch) } returns false
        coEvery { connectivityValidator.shouldDrop(eventDispatch) } returns false
        coEvery { connectivityValidator.shouldQueue(eventDispatch) } returns false
        coEvery { batteryValidator.shouldDrop(eventDispatch) } returns false
        coEvery { batteryValidator.shouldQueue(eventDispatch) } returns false
        coEvery { consentManager.shouldDrop(eventDispatch) } returns false
        coEvery { consentManager.shouldQueue(eventDispatch) } returns false

        coEvery { librarySettingsManager.fetchLibrarySettings() } just Runs
        every { librarySettingsManager.librarySettings } returns librarySettings
        excludeRecords { librarySettingsManager.librarySettings }


        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        eventRouter = spyk(EventDispatcher())
        eventRouter.subscribe(dispatcher)

        dispatchRouter = DispatchRouter(
            coroutineDispatcher,
            setOf(collector),
            setOf(transformer),
            setOf(validator, validator2, batchingValidator, connectivityValidator, consentManager),
            dispatchStore,
            librarySettingsManager,
            eventRouter
        )
    }

    @Test
    fun testIsCollected() = runBlocking {
        dispatchRouter.track(eventDispatch)

        coVerify(timeout = 1000) {
            collector.collect()
        }

        delay(10)
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

        coVerify(timeout = 1000) {
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
            dispatcher.onDispatchSend(any())
            dispatcher.onBatchDispatchSend(any())
        }
    }

    @Test
    fun testIsDroppedWhenBothReturnTrue() {
        every { validator.shouldDrop(eventDispatch) } returns true
        every { validator2.shouldDrop(eventDispatch) } returns true
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
    fun testIsDroppedWhenOneReturnsTrue() {
        every { validator.shouldDrop(eventDispatch) } returns true
        every { validator2.shouldDrop(eventDispatch) } returns false
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

        assertTrue(eventDispatch[Dispatch.Keys.WAS_QUEUED] == true)

        verify {
            // no further routing should happen
            dispatchStore.enqueue(eventDispatch)
        }
    }

    @Test
    fun testIsQueuedWhenOneReturnsTrue() {
        every { validator.shouldQueue(eventDispatch) } returns false
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

        assertTrue(eventDispatch[Dispatch.Keys.WAS_QUEUED] == true)

        verify {
            // no further routing should happen
            dispatchStore.enqueue(eventDispatch)
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
        assertTrue(dispatchRouter.shouldQueue(null).shouldQueue)
        assertFalse(dispatchRouter.shouldQueue(null, BatchingValidator::class.java).shouldQueue)

        // should only override a specific class
        every { connectivityValidator.shouldQueue(null) } returns true
        assertTrue(dispatchRouter.shouldQueue(null).shouldQueue)
        assertTrue(dispatchRouter.shouldQueue(null, BatchingValidator::class.java).shouldQueue)
    }

    @Test
    fun testIsDispatched() {
        every { batching.batchSize } returns 1
        dispatchRouter.track(eventDispatch)

        coVerify(timeout = 1500) {
            collector.collect()
            transformer.transform(eventDispatch)
            validator.shouldDrop(eventDispatch)
            eventRouter.onDispatchReady(eventDispatch)
            validator.shouldQueue(eventDispatch)
        }
        coVerify(timeout = 1000) {
            eventRouter.onDispatchSend(queuedEvents[0])
            eventRouter.onDispatchSend(queuedEvents[1])
            eventRouter.onDispatchSend(eventDispatch)
            dispatcher.onDispatchSend(queuedEvents[0])
            dispatcher.onDispatchSend(queuedEvents[1])
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
            eventRouter.onBatchDispatchSend(queuedEvents.plus(eventDispatch))
            dispatcher.onBatchDispatchSend(queuedEvents.plus(eventDispatch))
        }
    }

    @Test
    fun testIsDispatchedInMultipleBatches() {
        dispatchStore.clear()
        dispatchStore.enqueue(createDispatches(14))
        every { batching.batchSize } returns 5
        dispatchRouter.track(eventDispatch)

        coVerify(timeout = 1000, exactly = 3) {
            dispatcher.onBatchDispatchSend(match { it.size == 5 })
        }
    }

    @Test
    fun testSendsIncomingDispatchAloneWhenDoesntFitInLastBatch() {
        every { batching.batchSize } returns 2
        dispatchRouter.track(eventDispatch)

        coVerify(timeout = 1000, exactly = 1) {
            dispatcher.onBatchDispatchSend(match { batch ->
                batch[0].id == queuedEvents[0].id
                        && batch[1].id == queuedEvents[1].id
            })
            dispatcher.onDispatchSend(eventDispatch)
        }
    }

    @Test
    fun testSendsIncomingWithLastBatchWhenWithinBatchSize() {
        every { batching.batchSize } returns 3
        dispatchRouter.track(eventDispatch)

        coVerify(timeout = 1000, exactly = 1) {
            dispatcher.onBatchDispatchSend(match { batch ->
                batch[0].id == queuedEvents[0].id
                        && batch[1].id == queuedEvents[1].id
                        && batch[2].id == eventDispatch.id
            })
        }
    }

    @Test
    fun testSendsIncomingWhenNothingElseQueued() {
        every { batching.batchSize } returns 3
        dispatchStore.clear()
        dispatchRouter.track(eventDispatch)

        coVerify(timeout = 1000, exactly = 1) {
            dispatcher.onDispatchSend(eventDispatch)
        }
    }

    @Test
    fun shouldQueueCanProcessRemoteCommandsForBatteryLow() {
        every { batteryValidator.shouldQueue(any()) } returns true
        val result = dispatchRouter.shouldQueue(eventDispatch)

        assertTrue(result.shouldProcessRemoteCommand)
    }

    @Test
    fun shouldQueueCanProcessRemoteCommandsForNoConnectivity() {
        every { connectivityValidator.shouldQueue(any()) } returns true
        val result = dispatchRouter.shouldQueue(eventDispatch)

        assertTrue(result.shouldProcessRemoteCommand)
    }

    @Test
    fun shouldQueueCanProcessRemoteCommandsForBatchSizeNotReached() {
        every { batchingValidator.shouldQueue(any()) } returns true
        val result = dispatchRouter.shouldQueue(eventDispatch)

        assertTrue(result.shouldProcessRemoteCommand)
    }

    @Test
    fun shouldQueueCanNotProcessRemoteCommandsForCustomValidator() {
        every { validator.shouldQueue(any()) } returns true
        val result = dispatchRouter.shouldQueue(eventDispatch)

        assertFalse(result.shouldProcessRemoteCommand)
    }

    @Test
    fun shouldQueueCanNotProcessRemoteCommandsForNoConsent() {
        every { consentManager.shouldQueue(any()) } returns true
        val result = dispatchRouter.shouldQueue(eventDispatch)

        assertFalse(result.shouldProcessRemoteCommand)
    }

    @Test
    fun trackProcessesForRemoteCommandsWhenNotQueued() = runBlocking {
        dispatchRouter.track(eventDispatch)

        verify(timeout = 1000) {
            eventRouter.onProcessRemoteCommand(eventDispatch)
        }
    }

    @Test
    fun trackProcessesForRemoteCommandsWhenOnlyQueuedByAllowedValidators() = runBlocking {
        every { batteryValidator.shouldQueue(any()) } returns true
        every { batchingValidator.shouldQueue(any()) } returns true
        every { connectivityValidator.shouldQueue(any()) } returns true
        dispatchRouter.track(eventDispatch)

        verify(timeout = 1000) {
            eventRouter.onProcessRemoteCommand(eventDispatch)
        }
    }

    @Test
    fun trackDoesNotProcessForRemoteCommandsWhenQueued() = runBlocking {
        every { validator.shouldQueue(any()) } returns true
        dispatchRouter.track(eventDispatch)

        verify(inverse = true, timeout = 1000) {
            eventRouter.onProcessRemoteCommand(eventDispatch)
        }
    }

    @Test
    fun trackProcessesHistoricalEventsForRemoteCommandsWhenUnprocessed() = runBlocking {
        every { validator.shouldQueue(any()) } returns true
        dispatchRouter.track(eventDispatch) // queued

        verify(inverse = true, timeout = 1000) {
            eventRouter.onProcessRemoteCommand(eventDispatch)
        }

        every { validator.shouldQueue(any()) } returns false
        val newEvent = TealiumEvent("new")
        dispatchRouter.track(newEvent)

        verify(timeout = 1000) {
            eventRouter.onProcessRemoteCommand(eventDispatch)
            eventRouter.onProcessRemoteCommand(newEvent)
        }
    }

    @Test
    fun trackDoesNotProcessesHistoricalEventsForRemoteCommandsWhenNotUnprocessed() = runBlocking {
        dispatchRouter.track(eventDispatch)

        verify(inverse = true, timeout = 1000) {
            eventRouter.onProcessRemoteCommand(queuedEvents[0])
            eventRouter.onProcessRemoteCommand(queuedEvents[1])
        }
    }

    @Test
    fun sendQueuedEventsProcessesHistoricalEventsForRemoteCommandsWhenUnprocessed() = runBlocking {
        every { validator.shouldQueue(any()) } returns true
        dispatchRouter.track(eventDispatch)

        every { validator.shouldQueue(any()) } returns false
        dispatchRouter.sendQueuedEvents()

        verify(inverse = true, timeout = 1000) {
            eventRouter.onProcessRemoteCommand(queuedEvents[0])
            eventRouter.onProcessRemoteCommand(queuedEvents[1])
        }
        verify(timeout = 1000) {
            eventRouter.onProcessRemoteCommand(eventDispatch)
        }
    }

    @Test
    fun sendQueuedEventsDoesNotSendEventsIfShouldQueue() = runBlocking {
        every { validator.shouldQueue(any()) } returns true
        dispatchRouter.sendQueuedEvents()

        coVerify(inverse = true, timeout = 1000) {
            eventRouter.onProcessRemoteCommand(any())
            eventRouter.onBatchDispatchSend(any())
            eventRouter.onDispatchSend(any())
        }
    }

    @Test
    fun testLibrarySettingsAreFetchedOnlyOnce() {
        val oneEvent = listOf(eventDispatch)
        dispatchRouter.sendDispatches(oneEvent)

        every { batching.batchSize } returns 3
        val oneBatch = listOf(eventDispatch, eventDispatch, eventDispatch)
        dispatchRouter.sendDispatches(oneBatch)
        val twoBatches = listOf(
            eventDispatch,
            eventDispatch,
            eventDispatch,
            eventDispatch,
            eventDispatch
        )
        dispatchRouter.sendDispatches(twoBatches)

        coVerify(exactly = 3, timeout = 1000) {
            librarySettingsManager.fetchLibrarySettings()
        }
    }

    @Test
    fun testShouldQueueIsCalledAgainWithPreviouslyQueuedDispatches() {
        dispatchRouter.batchedDequeue(eventDispatch)

        verify {
            (queuedEvents + eventDispatch).forEach {
                validator.shouldQueue(it)
            }
        }
    }

    private fun createDispatches(count: Int): List<Dispatch> =
        (1 .. count).map {
            TealiumEvent("event_$it")
        }

    /**
     * In memory implementation for `DispatchStorage`
     *
     * Only methods relevant to testing are properly implemented
     */
    private class VolatileDispatchStore(initialList: List<Dispatch> = emptyList()) : QueueingDao<String, Dispatch> {
        private val queue: Queue<Dispatch> = LinkedList(initialList)

        override fun enqueue(item: Dispatch) {
            queue.add(item)
        }

        override fun enqueue(items: List<Dispatch>) {
            queue.addAll(items)
        }

        override fun dequeue(): Dispatch? = queue.poll()

        override fun dequeue(count: Int): List<Dispatch> {
            if (count < 0) return queue.toList()

            val dispatches = mutableListOf<Dispatch>()
            while (queue.isNotEmpty() && dispatches.size < count) {
                val dispatch = queue.poll()
                if (dispatch != null) {
                    dispatches.add(dispatch)
                }
            }
            return dispatches
        }

        override fun resize(size: Int) {
            if (size < 0) return
            while (queue.size > size) {
                queue.poll()
            }
        }

        override fun get(key: String): Dispatch? =
            queue.firstOrNull { it.id == key }

        override fun getAll(): Map<String, Dispatch> =
            queue.associateBy { it.id }

        override fun insert(item: Dispatch) = enqueue(item)

        override fun update(item: Dispatch) {
            TODO("Not yet implemented")
        }

        override fun delete(key: String) {
            TODO("Not yet implemented")
        }

        override fun upsert(item: Dispatch) {
            TODO("Not yet implemented")
        }

        override fun clear() = queue.clear()

        override fun keys(): List<String> = queue.map { it.id }

        override fun count(): Int = queue.count()

        override fun contains(key: String): Boolean = get(key) != null

        override fun purgeExpired() {
            // no-op
        }
    }
}