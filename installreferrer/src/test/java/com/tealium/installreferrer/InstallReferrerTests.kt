package com.tealium.installreferrer

import InstallReferrerConstants
import android.app.Application
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.ReferrerDetails
import com.tealium.core.Environment
import com.tealium.core.Tealium
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.messaging.EventDispatcher
import com.tealium.core.messaging.ExternalListener
import com.tealium.core.messaging.MessengerService
import com.tealium.core.persistence.DataLayer
import com.tealium.core.persistence.Expiry
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class InstallReferrerTests {

    lateinit var mockReferrerClient: InstallReferrerClient
    lateinit var app: Application
    lateinit var dataLayer: DataLayer
    lateinit var tealium: Tealium
    lateinit var messengerService: MessengerService

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        messengerService = spyk(MessengerService(EventDispatcher(), CoroutineScope(Dispatchers.IO)))
        app = RuntimeEnvironment.getApplication()

        // default to initial launch
        dataLayer = mockk(relaxed = true)
        mockReferrerClient = mockk(relaxed = true)
        mockDataLayerContents(null, null, null)
    }

    @Test
    fun installReferrer_Info_Is_Null_On_Creation() {
        val installReferrer = createInstallReferrer()

        assertNull(installReferrer.referrer)
        assertNull(installReferrer.referrerBegin)
        assertNull(installReferrer.referrerClick)
    }

    @Test
    fun installReferrer_Info_Is_Saved_When_Referrer_Is_Not_Empty() {
        val installReferrer = createInstallReferrer()
        val referrerDetails = mockReferrerDetails("affiliate", 100L, 101L)

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
    fun installReferrer_Info_Is_Not_Saved_When_Referrer_Is_Empty() {
        val referrerDetails = mockReferrerDetails("", 100L, 101L)

        val installReferrer = createInstallReferrer()

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
    fun installReferrer_Starts_Connection_When_No_Referrer_Yet() {
        val installReferrer = createInstallReferrer()

        verify {
            messengerService.subscribe(any<ExternalListener>())
            mockReferrerClient.startConnection(any())
        }
    }

    @Test
    fun installReferrer_Does_Not_Start_Connection_When_Referrer_Already_Retrieved() {
        mockDataLayerContents("affiliate", 100, 200)
        val installReferrer = createInstallReferrer()

        verify(inverse = true) {
            mockReferrerClient.startConnection(any())
        }
    }

    @Test
    fun factory_Method_Returns_New_Instance() {
        val config = TealiumConfig(app, "test", "test", Environment.DEV)
        val tealiumContext = TealiumContext(
            config, "", mockk(), dataLayer, mockk(), messengerService, mockk()
        )
        val installReferrer = InstallReferrer.Companion.create(context = tealiumContext)
        assertNotNull(installReferrer)
    }

    private fun mockDataLayerContents(
        referrer: String? = null,
        referrerBegin: Long? = null,
        referrerClick: Long? = null,
    ) {
        every { dataLayer.getString(InstallReferrerConstants.KEY_INSTALL_REFERRER) } returns referrer
        every { dataLayer.getLong(InstallReferrerConstants.KEY_INSTALL_REFERRER_BEGIN_TIMESTAMP) } returns referrerBegin
        every { dataLayer.getLong(InstallReferrerConstants.KEY_INSTALL_REFERRER_CLICK_TIMESTAMP) } returns referrerClick
    }

    private fun mockReferrerDetails(
        referrer: String? = null,
        referrerBegin: Long = 0L, // Bundle default
        referrerClick: Long = 0L, // Bundle default
    ): ReferrerDetails {
        val referrerDetails = mockk<ReferrerDetails>()
        every { referrerDetails.installReferrer } returns referrer
        every { referrerDetails.installBeginTimestampSeconds } returns referrerBegin
        every { referrerDetails.referrerClickTimestampSeconds } returns referrerClick
        every { mockReferrerClient.installReferrer } returns referrerDetails
        return referrerDetails
    }

    private fun createInstallReferrer(
        app: Application = this.app,
        dataLayer: DataLayer = this.dataLayer,
        events: MessengerService = this.messengerService,
        referrerClient: InstallReferrerClient = this.mockReferrerClient
    ): InstallReferrer =
        InstallReferrer(app, dataLayer, events, referrerClient)
}