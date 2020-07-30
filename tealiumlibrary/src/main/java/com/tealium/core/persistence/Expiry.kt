package com.tealium.core.persistence

import java.util.concurrent.TimeUnit

sealed class Expiry {

    abstract fun expiryTime(): Long
    abstract fun timeRemaining(): Long

    private class Session: Expiry() {
        override fun expiryTime(): Long {
            return -2L
        }

        override fun timeRemaining(): Long {
            return -2L
        }
    }

    private class Forever: Expiry() {
        override fun expiryTime(): Long {
            return -1L
        }

        override fun timeRemaining(): Long {
            return -1L
        }
    }

    private class After internal constructor(private val timeDifference: Long,
                                     private val creationTime: Long = getTimestamp()): Expiry() {

        override fun expiryTime(): Long {
            return creationTime + timeDifference
        }

        override fun timeRemaining(): Long {
            return creationTime + timeDifference - getTimestamp()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Expiry) return false

        return this.expiryTime() == other.expiryTime()
    }

    override fun hashCode(): Int {
        var result = 7
        val expiryTime = expiryTime()
        result = 31 * result + (expiryTime.xor(expiryTime.shr(32))).toInt()
        result = 31 * result + (if (isExpired(this)) 1  else 0)
        return result
    }

    companion object  {

        @JvmField val SESSION: Expiry = Session()
        @JvmField val FOREVER: Expiry = Forever()

        /**
         * @return Sum of the value in Seconds and the current system time in seconds to give a valid
         * epoch time in seconds.
         */
        @JvmStatic fun afterTimeUnit(value: Long, unit: TimeUnit, fromTime: Long = getTimestamp()) : Expiry {
            val long =  unit.toSeconds(value)
            return After(long, fromTime)
        }

        /**
         * @return Sum of the given values (converted to Seconds) and the current system time in
         * seconds to give a valid epoch time in seconds
         */
        @JvmStatic fun afterDate(days: Int, hours: Int, minutes: Int, seconds: Int, fromTime: Long = getTimestamp()): Expiry {
            val long = TimeUnit.DAYS.toSeconds(days.toLong()) +
                    TimeUnit.HOURS.toSeconds(hours.toLong()) +
                    TimeUnit.MINUTES.toSeconds(minutes.toLong()) +
                    seconds.toLong()
            return After(long, fromTime)
        }

        /**
         * Allows you to set a specific Epoch time in seconds for which to expire.
         */
        @JvmStatic fun afterEpochTime(timeInSeconds: Long) : Expiry {
            return After(timeInSeconds, 0L)
        }

        /**
         * Creates an [Expiry] from a [Long] value. Long is assumed to be the Epoch Time in seconds,
         * unless negative.
         */
        @JvmStatic fun fromLongValue(long: Long) : Expiry {
            return when (long) {
                -2L -> SESSION
                -1L -> FOREVER
                else -> {
                    afterEpochTime(long)
                }
            }
        }

        /**
         * Helper to determine whether an [Expiry] object is classed as expired.
         * Note. [FOREVER] and [SESSION] will always be false.
         */
        @JvmStatic fun isExpired(expiry: Expiry?): Boolean {
            return when(expiry) {
                null -> false
                is Forever -> false
                is Session -> false
                else -> {
                    expiry.timeRemaining() < 0
                }
            }
        }
    }
}
