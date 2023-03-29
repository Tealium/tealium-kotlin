package com.tealium.core.persistence

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import com.tealium.core.Logger
import com.tealium.core.TealiumConfig
import com.tealium.tealiumlibrary.BuildConfig
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Creates a SQLiteOpenHelper with the database name unique to the account and profile name set in
 * the [config] parameter.
 * @param config - TealiumConfig item with
 */
internal class DatabaseHelper(config: TealiumConfig, databaseName: String? = databaseName(config)) :
    SQLiteOpenHelper(config.application.applicationContext, databaseName, null, DATABASE_VERSION) {

    val db = writableDatabaseOrNull()
    private val queue = ConcurrentLinkedQueue<() -> Unit>() // we haven't used this before. use something else?

    private fun writableDatabaseOrNull(): SQLiteDatabase? {
        return try {
            return writableDatabase
        } catch (ex: SQLiteException) {
            Logger.dev(BuildConfig.TAG, "Error fetching database: ${ex.message}")
            null
        }
    }

    fun onDbReady(onReady: () -> Unit) {
        if (db == null || db.isReadOnly) {
            Logger.dev(BuildConfig.TAG, "Database is not in a writable state")
            //queue here?
            queue.add(onReady)
            return
        } else {
            if (queue.isNotEmpty()) {
                queue.forEach {
                    it.invoke()
                    queue.remove(it)
                }
            }
            onReady()
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SqlDataLayer.Sql.getCreateTableSql("datalayer"))
        db?.execSQL(SqlDataLayer.Sql.getCreateTableSql("dispatches"))
        // apply all necessary upgrades
        onUpgrade(db, 1, DATABASE_VERSION)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.let { database ->
            getDatabaseUpgrades(oldVersion).forEach {
                it.upgrade(database)
            }
        }
    }

    companion object {
        const val DATABASE_VERSION = 2

        private val databaseUpgrades = listOf<DatabaseUpgrade>(
            DatabaseUpgrade(version = 2) {
                it.execSQL(SqlDataLayer.Sql.getCreateTableSql("visitors"))
            }
        )

        fun getDatabaseUpgrades(
            oldVersion: Int,
            upgrades: List<DatabaseUpgrade> = databaseUpgrades
        ): List<DatabaseUpgrade> {
            return upgrades
                .filter { oldVersion < it.version }
                .sortedBy { it.version } // in case of upgrades added in incorrect order
        }

        /**
         * Returns a String unique to the Tealium Account/Profile
         *
         */
        fun databaseName(config: TealiumConfig): String {
            return "${config.tealiumDirectory}${File.separatorChar}tealium-${config.accountName}-${config.profileName}.db"
        }
    }
}