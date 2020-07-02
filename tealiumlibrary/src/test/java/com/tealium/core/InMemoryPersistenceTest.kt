package com.tealium.core

import com.tealium.dispatcher.ViewDispatch
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

class InMemoryPersistenceTest {

    lateinit var persistence: InMemoryPersistence

    @Before
    fun setUp() {
        persistence = InMemoryPersistence()
    }

    @Test
    fun persistenceEmptyByDefault() {
        assertEquals(0, persistence.count)
    }

    @Test
    fun enqueueSingleDispatchSuccess() {
        val dispatch = ViewDispatch("test_view")
        persistence.enqueue(dispatch)
        assertEquals(1, persistence.count)
    }

    @Test
    fun enqueueMultipleDispatchSuccess() {
        val dispatch1 = ViewDispatch("test_view1")
        val dispatch2 = ViewDispatch("test_view2")
        val dispatch3 = ViewDispatch("test_view3")
        persistence.enqueue(listOf(dispatch1, dispatch2, dispatch3))
        assertEquals(3, persistence.count)
    }

    @Test
    fun dequeueSuccess() {
        val dispatch1 = ViewDispatch("test_view1")
        val dispatch2 = ViewDispatch("test_view2")
        val dispatch3 = ViewDispatch("test_view3")
        persistence.enqueue(listOf(dispatch1, dispatch2, dispatch3))
        val dispatches = persistence.dequeue()

        assertEquals(3, dispatches.count())
        assertEquals(dispatch1, dispatches[0])
        assertEquals(dispatch2, dispatches[1])
        assertEquals(dispatch3, dispatches[2])
    }
}