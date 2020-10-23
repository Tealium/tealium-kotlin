package com.tealium.installreferrer

import com.tealium.core.*
import kotlinx.coroutines.*
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import io.mockk.*
import junit.framework.TestCase.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class InstallReferrerTests {

    val account = "teal-account"
    val profile = "teal-profile"
    val environment = Environment.DEV
    val dataSource = "teal-data-source"
    val visitorId = "teal-visitor-id"
    lateinit var tealiumContext: TealiumContext
    lateinit var config: TealiumConfig
    lateinit var context: Context
    lateinit var tealium: Tealium

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        config = spyk(TealiumConfig(context.applicationContext as Application,
                account,
                profile,
                environment, dataSourceId = dataSource))
        tealium = Tealium.create("instance_1", config)
        tealiumContext = TealiumContext(config,
                visitorId,
                mockk(),
                tealium.dataLayer,
                mockk(),
                mockk(),
                tealium)
    }

    @Test
    fun installReferrerInfoAddedToDataLayer() = runBlocking {
        InstallReferrer(tealiumContext)
        val data = tealium.dataLayer.all()
        delay(500)
        assertNotNull(data[InstallReferrerConstants.KEY_INSTALL_REFERRER])
        assertNotNull(data[InstallReferrerConstants.KEY_INSTALL_REFERRER_BEGIN_TIMESTAMP])
        assertNotNull(data[InstallReferrerConstants.KEY_INSTALL_REFERRER_CLICK_TIMESTAMP])

        assertTrue(data[InstallReferrerConstants.KEY_INSTALL_REFERRER] is String)
        assertTrue(data[InstallReferrerConstants.KEY_INSTALL_REFERRER] is String)
        assertTrue(data[InstallReferrerConstants.KEY_INSTALL_REFERRER] is String)
    }

    @Test
    fun installReferrerInfoRemovedFromDataLayer() = runBlocking {
        InstallReferrer(tealiumContext)

        tealium.dataLayer.remove(InstallReferrerConstants.KEY_INSTALL_REFERRER)
        tealium.dataLayer.remove(InstallReferrerConstants.KEY_INSTALL_REFERRER_BEGIN_TIMESTAMP)
        tealium.dataLayer.remove(InstallReferrerConstants.KEY_INSTALL_REFERRER_CLICK_TIMESTAMP)

        val data = tealium.dataLayer.all()
        delay(500)
        assertNull(data[InstallReferrerConstants.KEY_INSTALL_REFERRER])
        assertNull(data[InstallReferrerConstants.KEY_INSTALL_REFERRER_BEGIN_TIMESTAMP])
        assertNull(data[InstallReferrerConstants.KEY_INSTALL_REFERRER_CLICK_TIMESTAMP])
    }
}