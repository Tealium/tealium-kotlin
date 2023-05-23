package com.tealium.core.persistence

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.tealium.core.TealiumConfig
import java.io.File

/**
 * Creates a SQLiteOpenHelper with the database name unique to the account and profile name set in
 * the [config] parameter.
 * @param config - TealiumConfig item with
 */
internal class DatabaseHelper(
        config: TealiumConfig,
        databaseName: String? = databaseName(config),
        private val databaseVersion: Int = DATABASE_VERSION
) : SQLiteOpenHelper(config.application.applicationContext, databaseName, null, databaseVersion) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SqlDataLayer.Sql.getCreateTableSql("datalayer"))
        db?.execSQL(SqlDataLayer.Sql.getCreateTableSql("dispatches"))
        // apply all necessary upgrades
        onUpgrade(db, 1, databaseVersion)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.let { database ->
            getDatabaseUpgrades(oldVersion).forEach {
                it.upgrade(database)
            }
        }
    }

    override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL(SqlDataLayer.Sql.getDeleteTableSql("datalayer"))
        db?.execSQL(SqlDataLayer.Sql.getDeleteTableSql("dispatches"))
        db?.execSQL(SqlDataLayer.Sql.getDeleteTableSql("visitors"))

        onCreate(db)
    }

    companion object {
        const val DATABASE_VERSION = 2

        private val databaseUpgrades = listOf<DatabaseUpgrade>(
            DatabaseUpgrade(version = 2) {
                it.execSQL(SqlDataLayer.Sql.getCreateTableSql("visitors"))
            }
        )

        fun getDatabaseUpgrades(oldVersion: Int, upgrades: List<DatabaseUpgrade> = databaseUpgrades) : List<DatabaseUpgrade> {
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