package com.tealium.core.persistence

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.tealium.core.TealiumConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.io.File
import java.util.concurrent.Executors

/**
 * Creates a SQLiteOpenHelper with the database name unique to the account and profile name set in
 * the [config] parameter.
 * @param config - TealiumConfig item with
 */
internal class DatabaseHelper(config: TealiumConfig, databaseName: String? = databaseName(config))
    : SQLiteOpenHelper(config.application.applicationContext, databaseName,null, DATABASE_VERSION) {

    val dispatcher = databaseThread
    val scope = databaseScope

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SqlDataLayer.Companion.Sql.getCreateTableSql("datalayer"))
        db?.execSQL(SqlDataLayer.Companion.Sql.getCreateTableSql("dispatches"))
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // not implemented
    }

    companion object {
        const val DATABASE_VERSION = 1

        protected val databaseThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        protected val databaseScope = CoroutineScope(databaseThread)

        /**
         * Returns a String unique to the Tealium Account/Profile
         *
         */
        fun databaseName(config: TealiumConfig): String {
            return "${config.tealiumDirectory}${File.separatorChar}tealium-${config.accountName}-${config.profileName}.db"
        }
    }
}