package com.tealium.installreferrer

import com.tealium.core.*
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.installreferrer.api.ReferrerDetails
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.persistence.DataLayer
import com.tealium.core.persistence.Expiry
import io.mockk.*
import junit.framework.TestCase.*
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
    lateinit var dataLayer: DataLayer
    lateinit var tealium: Tealium

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        config = spyk(
            TealiumConfig(
                context.applicationContext as Application,
                account,
                profile,
                environment, dataSourceId = dataSource,
                collectors = mutableSetOf()
            )
        )

        dataLayer = mockk(relaxed = true)
        tealiumContext = TealiumContext(
            config,
            visitorId,
            mockk(),
            dataLayer,
            mockk(),
            mockk(),
            mockk()
        )
    }

    @Test
    fun installReferrerInfoIsNullOnCreation() {
        val installReferrer = InstallReferrer(tealiumContext)

        assertNull(installReferrer.referrer)
        assertNull(installReferrer.referrerBegin)
        assertNull(installReferrer.referrerClick)

        verify(exactly = 0) {
            dataLayer.putString(InstallReferrerConstants.KEY_INSTALL_REFERRER, any())
            dataLayer.putLong(InstallReferrerConstants.KEY_INSTALL_REFERRER_BEGIN_TIMESTAMP, any())
            dataLayer.putLong(InstallReferrerConstants.KEY_INSTALL_REFERRER_CLICK_TIMESTAMP, any())
        }
    }

    @Test
    fun installReferrerInfoIsSaved_WhenReferrerIsNotEmpty() {
        val installReferrer = InstallReferrer(tealiumContext)
        val referrerDetails = mockk<ReferrerDetails>()
        every { referrerDetails.installReferrer } returns "affiliate"
        every { referrerDetails.installBeginTimestampSeconds } returns 100L
        every { referrerDetails.referrerClickTimestampSeconds } returns 101L

        installReferrer.save(referrerDetails)

        verify {
            dataLayer.putString(
                InstallReferrerConstants.KEY_INSTALL_REFERRER,
                "affiliate",
                Expiry.FOREVER
            )
            dataLayer.putLong(
                InstallReferrerConstants.KEY_INSTALL_REFERRER_BEGIN_TIMESTAMP,
                100L,
                Expiry.FOREVER
            )
            dataLayer.putLong(
                InstallReferrerConstants.KEY_INSTALL_REFERRER_CLICK_TIMESTAMP,
                101L,
                Expiry.FOREVER
            )
        }

        assertEquals(installReferrer.referrer, "affiliate")
        assertEquals(installReferrer.referrerBegin, 100L)
        assertEquals(installReferrer.referrerClick, 101L)
    }

    @Test
    fun installReferrerInfoIsNotSaved_WhenReferrerIsEmpty() {
        val installReferrer = InstallReferrer(tealiumContext)
        val referrerDetails = mockk<ReferrerDetails>()
        every { referrerDetails.installReferrer } returns ""
        every { referrerDetails.installBeginTimestampSeconds } returns 100L
        every { referrerDetails.referrerClickTimestampSeconds } returns 101L

        installReferrer.save(referrerDetails)

        verify(exactly = 0) {
            dataLayer.putString(InstallReferrerConstants.KEY_INSTALL_REFERRER, any())
            dataLayer.putLong(InstallReferrerConstants.KEY_INSTALL_REFERRER_BEGIN_TIMESTAMP, any())
            dataLayer.putLong(InstallReferrerConstants.KEY_INSTALL_REFERRER_CLICK_TIMESTAMP, any())
        }

        assertNull(installReferrer.referrer)
        assertNull(installReferrer.referrerClick)
        assertNull(installReferrer.referrerBegin)
    }

    @Test
    fun factoryMethodReturnsNewInstance() {
        val installReferrer = InstallReferrer.create(context = tealiumContext)
        assertNotNull(installReferrer)
    }
}