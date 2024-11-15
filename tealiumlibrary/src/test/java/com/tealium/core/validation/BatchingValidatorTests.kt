package com.tealium.core.validation

import com.tealium.core.messaging.ValidationChangedListener
import com.tealium.core.persistence.DispatchStorage
import com.tealium.core.settings.Batching
import com.tealium.core.settings.LibrarySettings
import com.tealium.dispatcher.TealiumEvent
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BatchingValidatorTests {

    private val dispatch = TealiumEvent("", emptyMap())
    private lateinit var mockStore: DispatchStorage
    private lateinit var mockSettings: LibrarySettings
    private lateinit var batchSettings: Batching
    private lateinit var onBackgrounding: () -> Unit

    @Before
    fun setUp() {
        mockStore = mockk()
        mockSettings = mockk()
        batchSettings = mockk()
        onBackgrounding = mockk(relaxed = true)
        every { mockSettings.batching } returns batchSettings
    }

    @Test
    fun testShouldQueueWhenBatchSizeNotReached() {
        every { mockStore.count() } returns 5
        every { batchSettings.batchSize } returns 10
        every { batchSettings.maxQueueSize } returns 15

        val batchingValidator = BatchingValidator(mockStore, mockSettings, onBackgrounding)
        assertTrue(batchingValidator.shouldQueue(dispatch))
        assertFalse(batchingValidator.shouldDrop(dispatch))
    }

    @Test
    fun testShouldNotQueueWhenBatchSizeHasBeenReached() {
        every { mockStore.count() } returns 10
        every { batchSettings.batchSize } returns 10
        every { batchSettings.maxQueueSize } returns 15

        val batchingValidator = BatchingValidator(mockStore, mockSettings, onBackgrounding)
        assertFalse(batchingValidator.shouldQueue(dispatch))
        assertFalse(batchingValidator.shouldDrop(dispatch))
    }

    @Test
    fun testShouldNotQueueWhenBatchSizeExceeded() {
        every { mockStore.count() } returns 25
        every { batchSettings.batchSize } returns 10
        every { batchSettings.maxQueueSize } returns 15

        val batchingValidator = BatchingValidator(mockStore, mockSettings, onBackgrounding)
        assertFalse(batchingValidator.shouldQueue(dispatch))
        assertFalse(batchingValidator.shouldDrop(dispatch))
    }

    @Test
    fun testShouldNotQueueWhenQueueSizeExceeded() {
        every { mockStore.count() } returns 15
        every { batchSettings.batchSize } returns 10
        every { batchSettings.maxQueueSize } returns 10

        val batchingValidator = BatchingValidator(mockStore, mockSettings, onBackgrounding)
        assertFalse(batchingValidator.shouldQueue(dispatch))
        assertFalse(batchingValidator.shouldDrop(dispatch))
    }

    @Test
    fun testBatchingValidatorRevalidatesOnBackgrounding() {
        every { mockStore.count() } returns 5
        every { batchSettings.batchSize } returns 10
        every { batchSettings.maxQueueSize } returns 10

        every { onBackgrounding.invoke() } just Runs

        val batchingValidator = BatchingValidator(mockStore, mockSettings, onBackgrounding)

        // simulates a 2 activity app
        batchingValidator.onActivityResumed()       // activity count = 1
        batchingValidator.onActivityResumed()       // activity count = 2
        batchingValidator.onActivityStopped(isChangingConfiguration = false)  // activity count = 1
        verify(exactly = 0) {
            onBackgrounding.invoke()
        }

        batchingValidator.onActivityResumed()       // activity count = 2
        batchingValidator.onActivityStopped(isChangingConfiguration = false)  // activity count = 1
        batchingValidator.onActivityStopped(isChangingConfiguration = false)  // activity count = 0
        verify(exactly = 1) {
            onBackgrounding.invoke()
        }
    }

    @Test
    fun testBatchingValidatorDoesNotRevalidateIfChangingConfiguration() {
        every { mockStore.count() } returns 5
        every { batchSettings.batchSize } returns 10
        every { batchSettings.maxQueueSize } returns 10

        every { onBackgrounding.invoke() } just Runs

        val batchingValidator = BatchingValidator(mockStore, mockSettings, onBackgrounding)

        // simulates a 2 activity app
        batchingValidator.onActivityResumed()       // activity count = 1
        batchingValidator.onActivityResumed()       // activity count = 2
        batchingValidator.onActivityStopped(isChangingConfiguration = false)  // activity count = 1
        verify(exactly = 0) {
            onBackgrounding.invoke()
        }

        // change configuration (screen rotation) causes "stopped" to called prior to "resumed".
        batchingValidator.onActivityStopped(isChangingConfiguration = true)   // activity count = 0
        batchingValidator.onActivityResumed()       // activity count = 1
        verify(exactly = 0) {
            onBackgrounding.invoke()
        }

        // backgrounding
        batchingValidator.onActivityStopped(isChangingConfiguration = false)  // activity count = 1
        verify(exactly = 1) {
            onBackgrounding.invoke()
        }
    }

    @Test
    fun testBatchingValidatorDoesNotRevalidateIfMissedResume() {
        every { mockStore.count() } returns 5
        every { batchSettings.batchSize } returns 10
        every { batchSettings.maxQueueSize } returns 10

        every { onBackgrounding.invoke() } just Runs

        val batchingValidator = BatchingValidator(mockStore, mockSettings, onBackgrounding)

        batchingValidator.onActivityStopped(isChangingConfiguration = false)  // activity count = "-1"
        verify(exactly = 1) {
            onBackgrounding.invoke()
        }
    }
}