package com.tealium.core.validation

import com.tealium.core.messaging.EventRouter
import com.tealium.core.settings.Batching
import com.tealium.core.settings.LibrarySettings
import com.tealium.core.persistence.DispatchStorage
import com.tealium.dispatcher.EventDispatch
import io.mockk.*
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BatchingValidatorTests {

    private val dispatch = EventDispatch("", emptyMap())
    private lateinit var mockStore: DispatchStorage
    private lateinit var mockSettings: LibrarySettings
    private lateinit var batchSettings: Batching
    private lateinit var mockEventRouter: EventRouter

    @Before
    fun setUp() {
        mockStore = mockk()
        mockSettings = mockk()
        batchSettings = mockk()
        mockEventRouter = mockk()
        every { mockSettings.batching } returns batchSettings
    }

    @Test
    fun testShouldQueueWhenBatchSizeNotReached() {
        every { mockStore.count() } returns 5
        every { batchSettings.batchSize } returns 10
        every { batchSettings.maxQueueSize } returns 15

        val batchingValidator = BatchingValidator(mockStore, mockSettings, mockEventRouter)
        assertTrue(batchingValidator.shouldQueue(dispatch))
        assertFalse(batchingValidator.shouldDrop(dispatch))
    }

    @Test
    fun testShouldNotQueueWhenBatchSizeHasBeenReached() {
        every { mockStore.count() } returns 10
        every { batchSettings.batchSize } returns 10
        every { batchSettings.maxQueueSize } returns 15

        val batchingValidator = BatchingValidator(mockStore, mockSettings, mockEventRouter)
        assertFalse(batchingValidator.shouldQueue(dispatch))
        assertFalse(batchingValidator.shouldDrop(dispatch))
    }

    @Test
    fun testShouldNotQueueWhenBatchSizeExceeded() {
        every { mockStore.count() } returns 25
        every { batchSettings.batchSize } returns 10
        every { batchSettings.maxQueueSize } returns 15

        val batchingValidator = BatchingValidator(mockStore, mockSettings, mockEventRouter)
        assertFalse(batchingValidator.shouldQueue(dispatch))
        assertFalse(batchingValidator.shouldDrop(dispatch))
    }

    @Test
    fun testShouldNotQueueWhenQueueSizeExceeded() {
        every { mockStore.count() } returns 15
        every { batchSettings.batchSize } returns 10
        every { batchSettings.maxQueueSize } returns 10

        val batchingValidator = BatchingValidator(mockStore, mockSettings, mockEventRouter)
        assertFalse(batchingValidator.shouldQueue(dispatch))
        assertFalse(batchingValidator.shouldDrop(dispatch))
    }

    @Test
    fun testBatchingValidatorRevalidatesOnBackgrounding() {
        every { mockStore.count() } returns 5
        every { batchSettings.batchSize } returns 10
        every { batchSettings.maxQueueSize } returns 10

        every { mockEventRouter.onRevalidate(any()) } just Runs

        val batchingValidator = BatchingValidator(mockStore, mockSettings, mockEventRouter)

        // simulates a 2 activity app
        batchingValidator.onActivityResumed(null)       // activity count = 1
        batchingValidator.onActivityResumed(null)       // activity count = 2
        batchingValidator.onActivityStopped(false, null)  // activity count = 1
        verify(exactly = 0) {
            mockEventRouter.onRevalidate(any())
        }

        batchingValidator.onActivityResumed(null)       // activity count = 2
        batchingValidator.onActivityStopped(false, null)  // activity count = 1
        batchingValidator.onActivityStopped(false, null)  // activity count = 0
        verify(exactly = 1) {
            mockEventRouter.onRevalidate(any())
        }
    }

    @Test
    fun testBatchingValidatorDoesNotRevalidateIfChangingConfiguration() {
        every { mockStore.count() } returns 5
        every { batchSettings.batchSize } returns 10
        every { batchSettings.maxQueueSize } returns 10

        every { mockEventRouter.onRevalidate(any()) } just Runs

        val batchingValidator = BatchingValidator(mockStore, mockSettings, mockEventRouter)

        // simulates a 2 activity app
        batchingValidator.onActivityResumed(null)       // activity count = 1
        batchingValidator.onActivityResumed(null)       // activity count = 2
        batchingValidator.onActivityStopped(false, null)  // activity count = 1
        verify(exactly = 0) {
            mockEventRouter.onRevalidate(any())
        }

        // change configuration (screen rotation) causes "stopped" to called prior to "resumed".
        batchingValidator.onActivityStopped(true, null)   // activity count = 0
        batchingValidator.onActivityResumed(null)       // activity count = 1
        verify(exactly = 0) {
            mockEventRouter.onRevalidate(any())
        }

        // backgrounding
        batchingValidator.onActivityStopped(false, null)  // activity count = 1
        verify(exactly = 1) {
            mockEventRouter.onRevalidate(any())
        }
    }
}