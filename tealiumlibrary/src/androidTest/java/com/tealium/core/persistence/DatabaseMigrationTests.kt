package com.tealium.core.persistence

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTests {

    @Test
    fun testUpgrade_1_2() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val config = TealiumConfig(context, "test", "profile", Environment.QA)

        val databaseHelper = DatabaseHelper(config, databaseVersion = 1)
        val db = SQLiteDatabase.create(null)

        databaseHelper.onCreate(db)

        databaseHelper.onUpgrade(db, 1, 2)

        // Verify all 3 tables created
        db.rawQuery("SELECT * FROM datalayer", emptyArray())
        db.rawQuery("SELECT * FROM dispatches", emptyArray())
        db.rawQuery("SELECT * FROM visitors", emptyArray())
    }

    @Test
    fun testDowngrade_3_2() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val config = TealiumConfig(context, "test", "profile", Environment.QA)

        val databaseHelper = DatabaseHelper(config, databaseVersion = 3)
        val db = SQLiteDatabase.create(null)

        databaseHelper.onCreate(db)

        databaseHelper.onDowngrade(db, 3, 2)

        // Verify all 3 tables created
        db.rawQuery("SELECT * FROM datalayer", emptyArray())
        db.rawQuery("SELECT * FROM dispatches", emptyArray())
        db.rawQuery("SELECT * FROM visitors", emptyArray())
    }
}