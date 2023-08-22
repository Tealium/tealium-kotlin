package com.tealium.core.messaging

import com.tealium.dispatcher.Dispatch
import io.mockk.*
import kotlinx.coroutines.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
class EventRouterTests {

    private lateinit var eventRouter: EventDispatcher
    private val fakeDispatch = mockk<Dispatch>()

    @Before
    fun setUp() {
        eventRouter = spyk(EventDispatcher())
    }

    @Test
    fun testIndividualListenerIsCalled() {
        val listener = mockk<DispatchReadyListener>()
        every { listener.onDispatchReady(fakeDispatch) } just Runs

        eventRouter.subscribe(listener)
        eventRouter.onDispatchReady(fakeDispatch)

        verify {
            listener.onDispatchReady(fakeDispatch)
        }
    }

    @Test
    fun testCorrectListenerIsCalled() {
        val listener = mockk<DispatchReadyListener>()
        val listenerShouldNotBeCalled = mockk<DispatchDroppedListener>()
        every { listener.onDispatchReady(fakeDispatch) } just Runs
        every { listenerShouldNotBeCalled.onDispatchDropped(fakeDispatch) } just Runs

        eventRouter.subscribe(listener)
        eventRouter.subscribe(listenerShouldNotBeCalled)
        eventRouter.onDispatchReady(fakeDispatch)

        verify {
            listener.onDispatchReady(fakeDispatch)
        }

        verify(exactly = 0) {
            listenerShouldNotBeCalled.onDispatchDropped(fakeDispatch)
        }
    }

    @Test
    fun testAllCorrectListenersAreCalled() {
        val listener = mockk<DispatchReadyListener>()
        val otherListener = mockk<DispatchReadyListener>()
        val listenerShouldNotBeCalled = mockk<DispatchDroppedListener>()
        every { listener.onDispatchReady(fakeDispatch) } just Runs
        every { otherListener.onDispatchReady(fakeDispatch) } just Runs
        every { listenerShouldNotBeCalled.onDispatchDropped(fakeDispatch) } just Runs

        eventRouter.subscribe(listener)
        eventRouter.subscribe(otherListener)
        eventRouter.subscribe(listenerShouldNotBeCalled)
        eventRouter.onDispatchReady(fakeDispatch)

        verify {
            listener.onDispatchReady(fakeDispatch)
            otherListener.onDispatchReady(fakeDispatch)
        }

        verify(exactly = 0) {
            listenerShouldNotBeCalled.onDispatchDropped(fakeDispatch)
        }
    }

    @Test
    fun testListenerMessengers() {
        val listener = mockk<ValidationChangedListener>()
        every { listener.onRevalidate(any()) } just Runs

        val validationChangedMessenger = ValidationChangedMessenger()

        eventRouter.subscribe(listener)
        eventRouter.send(validationChangedMessenger)

        verify {
            listener.onRevalidate(any())
        }
    }

    @Test
    fun testAddingListenerDuringIteration_DoesNotThrow() = runBlocking {
        val listeners: List<ActivityObserverListener> = (0..10).map {
            mockk<ActivityObserverListener>(relaxed = true).also { eventRouter.subscribe(it) }
        }
        val listener1 = listeners[0]
        val subUnsub = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher()).async {
            (0..100).forEach {
                if (it % 2 == 0) {
                    eventRouter.unsubscribe(listener1)
                } else {
                    eventRouter.subscribe(listener1)
                }
            }
        }
        val send = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher()).async {
            (0..100).forEach { _ ->
                eventRouter.onActivityResumed()
            }
        }

        val result = send.await()
        val subUnsubResult = subUnsub.await()

        verify (exactly = 101) {
            listeners[1].onActivityResumed()
        }
    }

    @Test
    fun testAddingListenerDuringIteration_DoesNotThrow_Send() = runBlocking {
        val listeners: List<ValidationChangedListener> = (0..10).map {
            mockk<ValidationChangedListener>(relaxed = true).also { eventRouter.subscribe(it) }
        }
        val listener1 = listeners[0]
        val subUnsub = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher()).async {
            (0..100).forEach {
                if (it % 2 == 0) {
                    eventRouter.unsubscribe(listener1)
                } else {
                    eventRouter.subscribe(listener1)
                }
            }
        }
        val send = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher()).async {
            (0..100).forEach { _ ->
                eventRouter.send(ValidationChangedMessenger())
            }
        }

        val result = send.await()
        val subUnsubResult = subUnsub.await()

        verify (exactly = 101) {
            listeners[1].onRevalidate(any())
        }
    }

    @Test
    fun testEventsAreQueuedUntilMarkedAsReady() {
        val listener = mockk<DispatchReadyListener>()
        every { listener.onDispatchReady(fakeDispatch) } just Runs
        eventRouter = EventDispatcher(isReady = false)

        eventRouter.subscribe(listener)
        eventRouter.onDispatchReady(fakeDispatch)

        verify(exactly = 0) {
            listener.onDispatchReady(fakeDispatch)
        }
    }

    @Test
    fun testEventsAreDequeuedWhenMarkedAsReady() {
        val readyListener = mockk<DispatchReadyListener>()
        val droppedListener = mockk<DispatchDroppedListener>()
        every { readyListener.onDispatchReady(fakeDispatch) } just Runs
        every { droppedListener.onDispatchDropped(fakeDispatch) } just Runs
        eventRouter = spyk(EventDispatcher(isReady = false))

        eventRouter.subscribe(readyListener)
        eventRouter.subscribe(droppedListener)
        eventRouter.onDispatchReady(fakeDispatch)
        eventRouter.onDispatchDropped(fakeDispatch)

        verify(exactly = 0) {
            readyListener.onDispatchReady(fakeDispatch)
            droppedListener.onDispatchDropped(fakeDispatch)
        }

        eventRouter.setReady()

        verifyOrder {
            readyListener.onDispatchReady(fakeDispatch)
            droppedListener.onDispatchDropped(fakeDispatch)
        }

        confirmVerified()
    }

    @Test
    fun testLateSubscribersReceiveEventsIfSubscribedWhenNotReady() {
        val listener = mockk<DispatchReadyListener>()
        every { listener.onDispatchReady(fakeDispatch) } just Runs
        eventRouter = spyk(EventDispatcher(isReady = false))

        eventRouter.onDispatchReady(fakeDispatch)
        eventRouter.subscribe(listener)

        verify(exactly = 0) {
            listener.onDispatchReady(fakeDispatch)
        }

        eventRouter.setReady()

        verify(exactly = 1) {
            listener.onDispatchReady(fakeDispatch)
        }

        confirmVerified()
    }
}