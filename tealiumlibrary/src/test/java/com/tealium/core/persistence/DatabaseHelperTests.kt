package com.tealium.core.persistence

import org.junit.Assert.*
import org.junit.Test

class DatabaseHelperTests {

    val upgrade2 = DatabaseUpgrade(2, {})
    val upgrade3 = DatabaseUpgrade(3, {})
    val upgrade4 = DatabaseUpgrade(4, {})
    val sampleUpgrades = listOf(upgrade2, upgrade3, upgrade4)

    @Test
    fun getDatabaseUpgrades_ReturnsAllSampleUpgrades() {
        val toApply = DatabaseHelper.getDatabaseUpgrades(1, sampleUpgrades)

        assertEquals(3, toApply.count())
        assertTrue(toApply.contains(upgrade2))
        assertTrue(toApply.contains(upgrade3))
        assertTrue(toApply.contains(upgrade4))
    }

    @Test
    fun getDatabaseUpgrades_ReturnsOnlyRequiredUpgrades() {
        val toApply = DatabaseHelper.getDatabaseUpgrades(2, sampleUpgrades)

        assertEquals(2, toApply.count())
        assertFalse(toApply.contains(upgrade2))
        assertTrue(toApply.contains(upgrade3))
        assertTrue(toApply.contains(upgrade4))
    }


    @Test
    fun getDatabaseUpgrades_ReturnsUpgradesSorted() {
        val toApply = DatabaseHelper.getDatabaseUpgrades(2, sampleUpgrades.reversed())

        assertEquals(2, toApply.count())
        assertFalse(toApply.contains(upgrade2))
        assertTrue(toApply.contains(upgrade3))
        assertTrue(toApply.contains(upgrade4))

        assertEquals(upgrade3, toApply.first())
    }

    @Test
    fun getDatabaseUpgrades_ReturnsAllRealUpgrades() {
        val toApply = DatabaseHelper.getDatabaseUpgrades(1)

        // no upgrade from v0 -> v1, so should be (DATABASE_VERSION - 1)
        assertEquals(DatabaseHelper.DATABASE_VERSION - 1, toApply.count())
    }
}