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
internal class DatabaseHelper(config: TealiumConfig, databaseName: String? = databaseName(config))
    : SQLiteOpenHelper(config.application.applicationContext, databaseName,null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SqlDataLayer.Sql.getCreateTableSql("datalayer"))
        db?.execSQL(SqlDataLayer.Sql.getCreateTableSql("dispatches"))
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // not implemented
    }

    companion object {
        const val DATABASE_VERSION = 1

        /**
         * Returns a String unique to the Tealium Account/Profile
         *
         */
        fun databaseName(config: TealiumConfig): String {
            return "${config.tealiumDirectory}${File.separatorChar}tealium-${config.accountName}-${config.profileName}.db"
        }
    }
}