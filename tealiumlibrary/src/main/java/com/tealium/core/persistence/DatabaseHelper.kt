package com.tealium.core.persistence

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import com.tealium.core.Logger
import com.tealium.core.TealiumConfig
import com.tealium.tealiumlibrary.BuildConfig
import com.tealium.test.OpenForTesting
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Creates a SQLiteOpenHelper with the database name unique to the account and profile name set in
 * the [config] parameter.
 * @param config - TealiumConfig item with
 */
@OpenForTesting
internal class DatabaseHelper(config: TealiumConfig, databaseName: String? = databaseName(config)) :
    SQLiteOpenHelper(config.application.applicationContext, databaseName, null, DATABASE_VERSION) {
    internal val queue = ConcurrentLinkedQueue<(SQLiteDatabase) -> Unit>()
    private var _db: SQLiteDatabase? = null
    val db
        get() = _db ?: writableDatabaseOrNull()?.apply {
            _db = this
        }

    private fun writableDatabaseOrNull(): SQLiteDatabase? {
        return try {
            return writableDatabase?.takeIf { !it.isReadOnly }
        } catch (ex: SQLiteException) {
            Logger.dev(BuildConfig.TAG, "Error fetching database: ${ex.message}")
            null
        }
    }

    fun onDbReady(onReady: (SQLiteDatabase) -> Unit) {
        val localDb = db
        if (localDb == null || localDb.isReadOnly) {
            Logger.dev(BuildConfig.TAG, "Database is not in a writable state")
            //queue here?
            queue.add(onReady)
            return
        } else {
            if (queue.isNotEmpty()) {
                queue.forEach {
                    it.invoke(localDb)
                    queue.remove(it)
                }
            }
            onReady(localDb)
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