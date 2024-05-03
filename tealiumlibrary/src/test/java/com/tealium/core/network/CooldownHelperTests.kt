package com.tealium.core.network

import com.tealium.core.persistence.getTimestampMilliseconds
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CooldownHelperTests {

    @MockK
    lateinit var mockTimingProvider: () -> Long

    private val baseInterval = 100L
    private val maxInterval = 500L

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun initialState_IsNot_InCooldown() {
        val lastFetch = getTimestampMilliseconds() - 50
        val cooldown = CooldownHelper(maxInterval, baseInterval)

        assertFalse(cooldown.isInCooldown(lastFetch))
    }

    @Test
    fun afterFailure_IsInCooldown() {
        val lastFetch = getTimestampMilliseconds()
        val cooldown = CooldownHelper(maxInterval, baseInterval)
        cooldown.updateStatus(CooldownHelper.CooldownStatus.Failure)

        assertTrue(cooldown.isInCooldown(lastFetch))
        assertTrue(cooldown.isInCooldown(lastFetch - 50))
    }

    @Test
    fun afterSuccess_IsNot_InCooldown() {
        val cooldown = CooldownHelper(maxInterval, baseInterval)
        cooldown.updateStatus(CooldownHelper.CooldownStatus.Failure)
        cooldown.updateStatus(CooldownHelper.CooldownStatus.Success)

        assertFalse(cooldown.isInCooldown(getTimestampMilliseconds()))
        assertFalse(cooldown.isInCooldown(getTimestampMilliseconds() + 1000))
        assertFalse(cooldown.isInCooldown(getTimestampMilliseconds() - 1000))
    }

    @Test
    fun multipleFailures_Increase_Cooldown() {
        val lastFetch = 0L
        every { mockTimingProvider.invoke() } returnsMany listOf(0, 100, 200, 300, 400)

        val cooldown = CooldownHelper(maxInterval, baseInterval, mockTimingProvider)
        cooldown.updateStatus(CooldownHelper.CooldownStatus.Failure) // 100
        cooldown.updateStatus(CooldownHelper.CooldownStatus.Failure) // 200
        cooldown.updateStatus(CooldownHelper.CooldownStatus.Failure) // 300

        assertTrue(cooldown.isInCooldown(lastFetch)) // 0
        assertTrue(cooldown.isInCooldown(lastFetch)) // 100
        assertTrue(cooldown.isInCooldown(lastFetch)) // 200
        assertTrue(cooldown.isInCooldown(lastFetch)) // 300
        assertFalse(cooldown.isInCooldown(lastFetch)) // 400
    }

    @Test
    fun multipleFailures_LimitedTo_MaxInterval_Cooldown() {
        val lastFetch = 0L
        every { mockTimingProvider.invoke() } returnsMany listOf(0, 200, 400, 600)

        val cooldown = CooldownHelper(maxInterval, 200, mockTimingProvider)
        cooldown.updateStatus(CooldownHelper.CooldownStatus.Failure) // 200
        cooldown.updateStatus(CooldownHelper.CooldownStatus.Failure) // 400
        cooldown.updateStatus(CooldownHelper.CooldownStatus.Failure) // 500

        assertTrue(cooldown.isInCooldown(lastFetch)) // 0
        assertTrue(cooldown.isInCooldown(lastFetch)) // 200
        assertTrue(cooldown.isInCooldown(lastFetch)) // 400
        assertFalse(cooldown.isInCooldown(lastFetch)) // 600
    }
}