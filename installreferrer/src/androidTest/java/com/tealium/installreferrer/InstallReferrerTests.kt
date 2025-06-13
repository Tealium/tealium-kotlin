package com.tealium.installreferrer

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.ReferrerDetails
import com.tealium.core.Environment
import com.tealium.core.Tealium
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.messaging.EventDispatcher
import com.tealium.core.messaging.MessengerService
import com.tealium.core.persistence.DataLayer
import com.tealium.core.persistence.Expiry
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import junit.framework.TestCase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Test

class InstallReferrerTests {

    @RelaxedMockK
    private lateinit var mockReferrerClient: InstallReferrerClient

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
    lateinit var messengerService: MessengerService

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        messengerService = MessengerService(EventDispatcher(), CoroutineScope(Dispatchers.IO))
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
            messengerService,
            mockk()
        )
    }

    @Test
    fun installReferrerInfoIsNullOnCreation() {
        val installReferrer = InstallReferrer(tealiumContext, mockReferrerClient)

        assertNull(installReferrer.referrer)
        assertNull(installReferrer.referrerBegin)
        assertNull(installReferrer.referrerClick)

        verify(exactly = 0, timeout = 1000) {
            dataLayer.putString(InstallReferrerConstants.KEY_INSTALL_REFERRER, any())
            dataLayer.putLong(InstallReferrerConstants.KEY_INSTALL_REFERRER_BEGIN_TIMESTAMP, any())
            dataLayer.putLong(InstallReferrerConstants.KEY_INSTALL_REFERRER_CLICK_TIMESTAMP, any())
        }
    }

    @Test
    fun installReferrerInfoIsSaved_WhenReferrerIsNotEmpty() {
        val installReferrer = InstallReferrer(tealiumContext, mockReferrerClient)
        val referrerDetails = mockk<ReferrerDetails>()
        every { referrerDetails.installReferrer } returns "affiliate"
        every { referrerDetails.installBeginTimestampSeconds } returns 100L
        every { referrerDetails.referrerClickTimestampSeconds } returns 101L

        installReferrer.save(referrerDetails)

        verify(timeout = 1000) {
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
        val referrerDetails = mockk<ReferrerDetails>()
        every { referrerDetails.installReferrer } returns ""
        every { referrerDetails.installBeginTimestampSeconds } returns 100L
        every { referrerDetails.referrerClickTimestampSeconds } returns 101L
        every { mockReferrerClient.installReferrer } returns referrerDetails

        val installReferrer = InstallReferrer(tealiumContext, mockReferrerClient)

        installReferrer.save(referrerDetails)

        verify(exactly = 0, timeout = 1000) {
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