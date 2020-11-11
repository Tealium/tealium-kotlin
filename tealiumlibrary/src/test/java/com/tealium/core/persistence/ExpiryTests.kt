package com.tealium.core.persistence

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class ExpiryTests {

    @Test
    fun testExpiryValues() {

        assertEquals(-2, Expiry.SESSION.expiryTime())
        assertEquals(-1, Expiry.FOREVER.expiryTime())

        val timeInSeconds = System.currentTimeMillis() / 1000
        val secondsInTheFuture = 100L

        // timeUnit
        assertEquals(timeInSeconds + secondsInTheFuture,
                Expiry.afterTimeUnit(secondsInTheFuture, TimeUnit.SECONDS, timeInSeconds).expiryTime())
        assertEquals(timeInSeconds + secondsInTheFuture * 60 * 60 * 24,
                Expiry.afterTimeUnit(secondsInTheFuture, TimeUnit.DAYS, timeInSeconds).expiryTime())

        // date
        assertEquals(timeInSeconds + secondsInTheFuture,
                Expiry.afterDate(0, 0, 0, secondsInTheFuture.toInt(), timeInSeconds).expiryTime())
        assertEquals(timeInSeconds + secondsInTheFuture * 60 * 60 * 24,
                Expiry.afterDate(secondsInTheFuture.toInt(), 0, 0, 0, timeInSeconds).expiryTime())
        assertEquals(timeInSeconds + (1 * 60 * 60 * 24) + (1 * 60 * 60) + (1 * 60) + 1,
                Expiry.afterDate(1, 1, 1, 1, timeInSeconds).expiryTime())

        // epochTime
        assertEquals(timeInSeconds + secondsInTheFuture,
                Expiry.afterEpochTime(timeInSeconds + secondsInTheFuture).expiryTime())
    }

    @Test
    fun testFromLongValue() {
        val timeInSeconds = System.currentTimeMillis() / 1000
        val secondsInTheFuture = 100L

        assertEquals(timeInSeconds + secondsInTheFuture,
                Expiry.fromLongValue(timeInSeconds + secondsInTheFuture).expiryTime())
        assertEquals(Expiry.SESSION,
                Expiry.fromLongValue(-2))
        assertEquals(Expiry.FOREVER,
                Expiry.fromLongValue(-1))

        val expiry = Expiry.fromLongValue(timeInSeconds + secondsInTheFuture)
        assertEquals(timeInSeconds + secondsInTheFuture, expiry.expiryTime())
        assertTrue(expiry.timeRemaining() <= secondsInTheFuture)
    }

    @Test
    fun testEquality() {
        val timeInSeconds = System.currentTimeMillis() / 1000

        val session = Expiry.SESSION
        val session2 = Expiry.SESSION

        assertTrue(session == session2)
        assertEquals(session.hashCode(), session2.hashCode())

        val forever = Expiry.FOREVER
        val forever2 = Expiry.FOREVER

        assertTrue(forever == forever2)
        assertEquals(forever.hashCode(), forever2.hashCode())

        val after1 = Expiry.afterEpochTime(timeInSeconds)
        val after2 = Expiry.afterEpochTime(timeInSeconds)
        val after3 = Expiry.afterEpochTime(timeInSeconds + 100)
        assertTrue(after1 == after2)
        assertTrue(after1 != after3)
        assertTrue(after2 != after3)
        assertEquals(after1.hashCode(), after2.hashCode())
        assertNotEquals(after1.hashCode(), after3.hashCode())
        assertNotEquals(after2.hashCode(), after3.hashCode())


        assertEquals(after1.hashCode(), after1.hashCode())
        assertEquals(after2.hashCode(), after2.hashCode())
        assertEquals(after3.hashCode(), after3.hashCode())

        val after4 = Expiry.afterEpochTime(-2)
        assertNotEquals(after4.hashCode(), Expiry.SESSION.hashCode())

    }
}