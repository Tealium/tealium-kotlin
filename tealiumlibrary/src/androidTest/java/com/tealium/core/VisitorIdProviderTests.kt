package com.tealium.core

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.persistence.DataLayer
import com.tealium.core.persistence.DatabaseHelper
import com.tealium.core.persistence.DefaultVisitorStorage
import com.tealium.core.persistence.PersistentStorage
import com.tealium.core.persistence.VisitorStorage
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

class VisitorIdProviderTests {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var dataLayer: DataLayer
    private lateinit var visitorStorage: VisitorStorage

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = TealiumConfig(context as Application, "test", "test", Environment.DEV)
        dbHelper = DatabaseHelper(config, null) // in-memory

        dataLayer = PersistentStorage(dbHelper, "visitors", eventRouter = mockk(relaxed = true))
        visitorStorage = DefaultVisitorStorage(dbHelper)
    }

    @Test
    fun initial_VisitorId_GetsGenerated() {
        val visitorIdProvider = VisitorIdProvider()
    }

}