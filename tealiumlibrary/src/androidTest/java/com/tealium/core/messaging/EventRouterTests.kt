package com.tealium.core.messaging

import com.tealium.dispatcher.Dispatch
import io.mockk.*
import org.junit.Before
import org.junit.Test

class EventRouterTests {

    private lateinit var eventRouter: EventRouter
    private val fakeDispatch = mockk<Dispatch>()

    @Before
    fun setUp() {
        eventRouter = spyk<EventDispatcher>()
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
}