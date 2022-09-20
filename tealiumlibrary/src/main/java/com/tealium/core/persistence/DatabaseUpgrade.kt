package com.tealium.core.persistence

import android.database.sqlite.SQLiteDatabase

/**
 * Defines an upgrade task to the Tealium Database
 *
 * @param version - The db version number being upgraded to
 * @param upgrade - The function that applies the update
 */
class DatabaseUpgrade(
    val version: Int,
    val upgrade: (SQLiteDatabase) -> Unit
)