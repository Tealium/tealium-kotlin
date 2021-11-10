package com.tealium.core

import android.app.Application
import android.content.SharedPreferences
import com.tealium.core.messaging.EventRouter
import com.tealium.dispatcher.TealiumEvent
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SessionManagerTests {

    @MockK
    lateinit var context: Application

    @MockK
    lateinit var mockFile: File

    @MockK
    lateinit var sharedPreferences: SharedPreferences

    @MockK
    lateinit var editor: SharedPreferences.Editor

    @MockK
    lateinit var eventRouter: EventRouter

    lateinit var config: TealiumConfig
    lateinit var sessionManager: SessionManager
    val dispatch = TealiumEvent("test")

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { context.filesDir } returns mockFile
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.apply() } just Runs
        every { editor.putLong(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { eventRouter.onNewSession(any()) } just Runs
        every { eventRouter.onSessionStarted(any()) } just Runs

        // default that there was not a previous session stored.
        every { sharedPreferences.getLong(Session.KEY_SESSION_ID, any()) } returns Session.INVALID_SESSION_ID
        every { sharedPreferences.getLong(Session.KEY_SESSION_LAST_EVENT_TIME, any()) } returns 0L
        every { sharedPreferences.getInt(Session.KEY_SESSION_EVENT_COUNT, any()) } returns 0
        every { sharedPreferences.getBoolean(Session.KEY_SESSION_STARTED, any()) } returns false

        config = TealiumConfig(context, "test", "profile", Environment.QA)
    }

    @Test
    fun testNewSessionIsGeneratedWhenNothingStored() {
        sessionManager = SessionManager(config, eventRouter)

        val session = sessionManager.currentSession
        assertTrue(session.id > 0)
        assertEquals(0L, session.lastEventTime)
        assertEquals(0, session.eventCount)

        verify(exactly = 1) {
            eventRouter.onNewSession(any())
        }
    }

    @Test
    fun testNewSessionMethodProvidesNewSession() = runBlocking {
        sessionManager = SessionManager(config, eventRouter)

        val session = sessionManager.currentSession
        delay(100)
        val newSession = sessionManager.newSession()
        assertNotSame(session, newSession)
        assertNotEquals(session.id, newSession.id)

        // 2 calls; one for the initial session, and one from `newSession` call
        verify(exactly = 2) {
            eventRouter.onNewSession(any())
        }
    }

    @Test
    fun testValidPersistedSessionGetsContinued() {
        val timestamp = System.currentTimeMillis()
        every { sharedPreferences.getLong(Session.KEY_SESSION_ID, any()) } returns timestamp - 10000
        every { sharedPreferences.getLong(Session.KEY_SESSION_LAST_EVENT_TIME, any()) } returns timestamp - 5000
        every { sharedPreferences.getInt(Session.KEY_SESSION_EVENT_COUNT, any()) } returns 5
        every { sharedPreferences.getBoolean(Session.KEY_SESSION_STARTED, any()) } returns false
        sessionManager = SessionManager(config, eventRouter)

        val session = sessionManager.currentSession
        assertEquals(timestamp - 10000, session.id)
        assertEquals(timestamp - 5000, session.lastEventTime)
        assertEquals(5, session.eventCount)
        assertFalse(session.sessionStarted)

        verify(exactly = 0) {
            eventRouter.onNewSession(any())
        }
    }

    @Test
    fun testInvalidPersistedSessionGetsIgnored() {
        val timestamp = System.currentTimeMillis()
        every { sharedPreferences.getLong(Session.KEY_SESSION_ID, any()) } returns timestamp - SessionManager.SESSION_LENGTH_MS - 10000
        every { sharedPreferences.getLong(Session.KEY_SESSION_LAST_EVENT_TIME, any()) } returns timestamp - SessionManager.SESSION_LENGTH_MS - 5000
        every { sharedPreferences.getInt(Session.KEY_SESSION_EVENT_COUNT, any()) } returns 5
        every { sharedPreferences.getBoolean(Session.KEY_SESSION_STARTED, any()) } returns true
        sessionManager = SessionManager(config, eventRouter)

        val session = sessionManager.currentSession
        assertTrue(session.id >= timestamp)
        assertEquals(0, session.lastEventTime)
        assertEquals(0, session.eventCount)
        assertFalse(session.sessionStarted)

        verify(exactly = 1) {
            eventRouter.onNewSession(any())
        }
    }

    @Test
    fun testNoSessionStartOnSingleEvent() {
        sessionManager = SessionManager(config, eventRouter)
        val session = sessionManager.currentSession
        assertFalse(session.sessionStarted)
        assertEquals(0, session.eventCount)

        sessionManager.track(dispatch)

        assertFalse(session.sessionStarted)
        assertEquals(1, session.eventCount)

        verify(exactly = 0) {
            eventRouter.onSessionStarted(any())
        }
    }

    @Test
    fun testNoSessionStartOnTwoEventsOutsideOfWindow() = runBlocking {
        sessionManager = SessionManager(config, eventRouter)
        val session = sessionManager.currentSession
        assertFalse(session.sessionStarted)
        assertEquals(0, session.eventCount)

        sessionManager.track(dispatch)
        assertFalse(session.sessionStarted)
        assertEquals(1, session.eventCount)

        // Window 1
        delay(SessionManager.SESSION_START_WINDOW_LENGTH_MS.toLong() + 500L)
        sessionManager.track(dispatch)
        assertFalse(session.sessionStarted)
        assertEquals(2, session.eventCount)

        // Window 2
        delay(SessionManager.SESSION_START_WINDOW_LENGTH_MS.toLong() + 500L)
        sessionManager.track(dispatch)
        assertFalse(session.sessionStarted)
        assertEquals(3, session.eventCount)

        verify(exactly = 0) {
            eventRouter.onSessionStarted(any())
        }
    }

    @Test
    fun testSessionStartOnTwoEventsWithinWindow() = runBlocking {
        sessionManager = SessionManager(config, eventRouter)
        val session = sessionManager.currentSession
        assertFalse(session.sessionStarted)
        assertEquals(0, session.eventCount)

        sessionManager.track(dispatch)
        assertFalse(session.sessionStarted)
        assertEquals(1, session.eventCount)

        // Window 1
        delay(SessionManager.SESSION_START_WINDOW_LENGTH_MS - (SessionManager.SESSION_START_WINDOW_LENGTH_MS - 1000L))
        sessionManager.track(dispatch)
        assertTrue(session.sessionStarted)
        assertEquals(2, session.eventCount)

        verify(exactly = 1) {
            eventRouter.onSessionStarted(any())
        }
    }

    @Test
    fun testSessionStartsOnlyOnce() {
        sessionManager = SessionManager(config, eventRouter)
        val session = sessionManager.currentSession
        assertFalse(session.sessionStarted)
        assertEquals(0, session.eventCount)

        sessionManager.track(dispatch)
        assertFalse(session.sessionStarted)
        assertEquals(1, session.eventCount)

        repeat(5) {
            sessionManager.track(dispatch)
        }
        assertTrue(session.sessionStarted)
        assertEquals(6, session.eventCount)

        verify(exactly = 1) {
            eventRouter.onSessionStarted(any())
        }
    }

    @Test
    fun testMultipleInstancesHaveDifferentIds() = runBlocking {
        sessionManager = SessionManager(config, eventRouter)
        val session = sessionManager.currentSession

        val otherConfig = TealiumConfig(context, "test2", "main", Environment.QA)
        val otherSessionManager = SessionManager(otherConfig, eventRouter)
        val otherSession = otherSessionManager.currentSession

        delay(20)
        assertNotEquals(session.id, otherSession.id)
    }

    @Test
    fun testIsExpiredUtility() {
        val currentTime = System.currentTimeMillis()
        val expired1 = Session(currentTime - SessionManager.SESSION_LENGTH_MS - 1, // 1ms over the expiry
                currentTime - SessionManager.SESSION_LENGTH_MS - 1,
                10,
                true)
        val expired2 = Session(Session.INVALID_SESSION_ID, // default expired session
                0,
                10,
                true)
        val valid1 = Session(currentTime, // sessionId current but last event is not.
                0,
                10,
                true)
        val valid2 = Session(currentTime - SessionManager.SESSION_LENGTH_MS - 10,
                currentTime - 100, // sessionId expired, but last event was recent
                10,
                true)

        assertTrue(SessionManager.isExpired(expired1))
        assertTrue(SessionManager.isExpired(expired2))
        assertFalse(SessionManager.isExpired(valid1))
        assertFalse(SessionManager.isExpired(valid2))
    }
}