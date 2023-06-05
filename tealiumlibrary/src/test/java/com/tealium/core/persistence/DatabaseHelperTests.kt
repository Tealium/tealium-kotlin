package com.tealium.core.persistence

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import com.tealium.core.TealiumConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
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

    @Test
    fun db_ReturnsWritableDatabase_OrNull() {
        val config: TealiumConfig = mockk()
        val app: Application = mockk()
        every { config.application } returns app
        every { app.applicationContext } returns app

        mockkConstructor(DatabaseHelper::class)
        val mockDb: SQLiteDatabase = mockk()
        // return null db first, then mock
        every { anyConstructed<DatabaseHelper>().writableDatabase } returns null andThen mockDb
        // return read-only first, then writable
        every { mockDb.isReadOnly } returns true andThen false

        val dbHelper = DatabaseHelper(config, null)

        assertNull(dbHelper.db) // null db not allowed.
        assertNull(dbHelper.db) // read-only not allowed.
        assertNotNull(dbHelper.db)
        assertFalse(dbHelper.db!!.isReadOnly)
    }
}