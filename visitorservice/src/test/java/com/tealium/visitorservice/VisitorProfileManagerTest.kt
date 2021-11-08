package com.tealium.visitorservice

import android.app.Application
import com.tealium.core.Environment
import com.tealium.core.JsonLoader
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.messaging.MessengerService
import com.tealium.core.network.HttpClient
import com.tealium.core.network.ResourceRetriever
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlinx.coroutines.delay
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
class VisitorProfileManagerTest {

    @MockK
    private lateinit var mockApplication: Application

    @MockK
    private lateinit var mockContext: TealiumContext

    @RelaxedMockK
    private lateinit var mockConfig: TealiumConfig

    @RelaxedMockK
    private lateinit var mockHttpClient: HttpClient

    @MockK
    private lateinit var mockResourceRetriever: ResourceRetriever

    @MockK
    private lateinit var mockLoader: JsonLoader

    @MockK
    private lateinit var mockMessengerService: MessengerService

    private lateinit var directory: File

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        directory = File("test")
        directory.mkdir()

        mockkStatic("com.tealium.visitorservice.TealiumConfigVisitorServiceKt")
        every { mockContext.config } returns mockConfig
        every { mockContext.events } returns mockMessengerService
        every { mockContext.visitorId } returns "visitorId"
        every { mockContext.httpClient } returns mockHttpClient

        every { mockConfig.application } returns mockApplication
        every { mockConfig.accountName } returns "test-account"
        every { mockConfig.profileName } returns "test-profile"
        every { mockConfig.environment } returns Environment.DEV
        every { mockConfig.tealiumDirectory } returns directory
        every { mockConfig.overrideVisitorServiceUrl } returns null
        every { mockConfig.visitorServiceRefreshInterval } returns null

        coEvery { mockMessengerService.send(any<VisitorUpdatedMessenger>()) } just Runs
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
        unmockkObject(VisitorProfile.Companion)
    }

    @Test
    fun persistence_IsLoadedFromDiskWhenNotNull() {
        every { mockLoader.loadFromFile(any()) } returns "{}"
        val visitorProfileManager = VisitorManager(mockContext, loader = mockLoader)

        assertNotNull(visitorProfileManager.loadCachedProfile())
        assertNotNull(visitorProfileManager.visitorProfile)
    }

    @Test
    fun persistence_ReturnsNullWhenNoFile() {
        every { mockLoader.loadFromFile(any()) } returns null
        val visitorProfileManager = VisitorManager(mockContext, loader = mockLoader)

        assertNull(visitorProfileManager.loadCachedProfile())
        assertNotNull(visitorProfileManager.visitorProfile)
    }

    @Test
    fun fetching_ShouldNotFetchWhenAlreadyUpdating() {
        val visitorProfileManager = VisitorManager(mockContext)
        visitorProfileManager.isUpdating.set(true)

        runBlocking {
            visitorProfileManager.requestVisitorProfile()
        }
        coVerify(exactly = 0) {
            mockHttpClient.get(any())
        }
    }

    @Test
    fun fetching_ShouldNotFetchWhenIntervalNotReached() {
        coEvery { mockHttpClient.get(any()) } returns validExampleProfileString

        val visitorProfileManager = VisitorManager(mockContext)

        runBlocking {
            visitorProfileManager.onBatchDispatchSend(mockk())
            visitorProfileManager.onDispatchSend(mockk())
            visitorProfileManager.updateProfile()
        }
        coVerify(exactly = 1) {
            mockHttpClient.get(any())
        }
    }

    @Test
    fun fetching_ShouldRetryFiveTimesOnly() = runBlocking {
        coEvery { mockHttpClient.get(any()) } returnsMany listOf(null, null, "{}", "{}", validExampleProfileString, null)

        val visitorProfileManager = VisitorManager(mockContext)

        runBlocking {
            visitorProfileManager.requestVisitorProfile()
        }
        coVerify(exactly = 5) {
            mockHttpClient.get(any())
        }
    }

    @Test
    fun fetching_ShouldBreakWhenFoundValidUpdatedProfile() = runBlocking {
        coEvery { mockHttpClient.get(any()) } returnsMany listOf(null, "{}", validExampleProfileString, null)

        val visitorProfileManager = VisitorManager(mockContext)

        runBlocking {
            visitorProfileManager.requestVisitorProfile()
        }
        coVerify(exactly = 3) {
            mockHttpClient.get(any())
        }
    }

    @Test
    fun updated_ShouldNotSendUpdateWhenEventCountSame() {
        mockkObject(VisitorProfile.Companion)
        every { VisitorProfile.Companion.fromJson(any()) } returnsMany listOf(VisitorProfile(totalEventCount = 0), VisitorProfile(totalEventCount = 0))
        coEvery { mockHttpClient.get(any()) } returns validExampleProfileString
        val visitorProfileManager = VisitorManager(mockContext)

        runBlocking {
            assertEquals(0, visitorProfileManager.visitorProfile.totalEventCount)
            visitorProfileManager.requestVisitorProfile()
        }
        verify(exactly = 0) {
            mockMessengerService.send(any<VisitorUpdatedMessenger>())
        }
        assertEquals(0, visitorProfileManager.visitorProfile.totalEventCount)
    }

    @Test
    fun updated_ShouldSendUpdateWhenEventCountDifferent() {
        mockkObject(VisitorProfile.Companion)
        every { VisitorProfile.Companion.fromJson(any()) } returnsMany listOf(VisitorProfile(totalEventCount = 0), VisitorProfile(totalEventCount = 1))
        coEvery { mockHttpClient.get(any()) } returns validExampleProfileString
        val visitorProfileManager = VisitorManager(mockContext)
        assertEquals(0, visitorProfileManager.visitorProfile.totalEventCount)

        runBlocking {
            visitorProfileManager.requestVisitorProfile()
        }
        verify(exactly = 1) {
            mockMessengerService.send(any<VisitorUpdatedMessenger>())
        }
        assertEquals(1, visitorProfileManager.visitorProfile.totalEventCount)
    }

    @Test
    fun updated_ShouldNotSendUpdateWhenInvalidProfile() {
        coEvery { mockHttpClient.get(any()) } returns "{}"
        val visitorProfileManager = VisitorManager(mockContext)
        assertEquals(0, visitorProfileManager.visitorProfile.totalEventCount)
        runBlocking {
            visitorProfileManager.requestVisitorProfile()
        }
        verify(exactly = 0) {
            mockMessengerService.send(any<VisitorUpdatedMessenger>())
        }
        assertEquals(0, visitorProfileManager.visitorProfile.totalEventCount)
    }

    @Test
    fun config_ShouldUseConfiguredUrlWhenProvided() {
        every { mockConfig.overrideVisitorServiceUrl } returns "https://my.url.com"
        coEvery { mockHttpClient.get(any()) } returns validExampleProfileString
        val visitorProfileManager = VisitorManager(mockContext)

        runBlocking {
            visitorProfileManager.requestVisitorProfile()
        }
        coVerify {
            mockHttpClient.get("https://my.url.com")
        }
    }

    @Test
    fun config_ShouldUseConfiguredIntervalWhenProvided() {
        every { mockConfig.visitorServiceRefreshInterval } returns 3L
        coEvery { mockHttpClient.get(any()) } returns validExampleProfileString
        val visitorProfileManager = spyk(VisitorManager(mockContext))

        runBlocking {
            visitorProfileManager.onBatchDispatchSend(mockk())
            delay(1000)
            visitorProfileManager.onDispatchSend(mockk()) // should be skipped
            delay(4000)
            visitorProfileManager.updateProfile()
        }
        coVerify {
            visitorProfileManager.onBatchDispatchSend(any())
            visitorProfileManager.updateProfile()
            visitorProfileManager.requestVisitorProfile()

            visitorProfileManager.onDispatchSend(any())
            visitorProfileManager.updateProfile()

            visitorProfileManager.updateProfile()
            visitorProfileManager.requestVisitorProfile()
        }
    }

    @Test
    fun listener_IsCalled() {
        val visitorUpdatedListener: VisitorUpdatedListener = mockk()
        every { visitorUpdatedListener.onVisitorUpdated(any()) } just Runs

        val visitorProfile = VisitorProfile()
        val visitorUpdatedMessenger = VisitorUpdatedMessenger(visitorProfile)

        visitorUpdatedMessenger.deliver(visitorUpdatedListener)

        verify {
            visitorUpdatedListener.onVisitorUpdated(visitorProfile)
        }
    }

    @Test
    fun generateVisitorServiceUrl_VisitorIdUpdatesWithContext() {
        coEvery { mockHttpClient.get(any()) } returns "{}"
        val visitorProfileManager = VisitorManager(mockContext)
        assertEquals("https://visitor-service.tealiumiq.com/test-account/test-profile/visitorId", visitorProfileManager.generateVisitorServiceUrl())

        every { mockContext.visitorId } returns "newVisitor"
        runBlocking {
            visitorProfileManager.requestVisitorProfile()
        }
        assertEquals("https://visitor-service.tealiumiq.com/test-account/test-profile/newVisitor", visitorProfileManager.generateVisitorServiceUrl())
    }

    @Test
    fun generateVisitorServiceUrl_ReplacesPlaceholders_Default() {
        val visitorProfileManager = VisitorManager(mockContext)
        assertEquals("https://visitor-service.tealiumiq.com/test-account/test-profile/visitorId", visitorProfileManager.generateVisitorServiceUrl())
    }

    @Test
    fun generateVisitorServiceUrl_ReplacesPlaceholders_OverrideUrl() {
        every { mockConfig.overrideVisitorServiceUrl } returns "my.url/{{profile}}/{{visitorId}}"
        val visitorProfileManager = VisitorManager(mockContext)
        assertEquals("my.url/test-profile/visitorId", visitorProfileManager.generateVisitorServiceUrl())
    }

    @Test
    fun generateVisitorService_OverrideProfile() {
        every { mockConfig.visitorServiceOverrideProfile } returns "testingProfile"
        val visitorProfileManager = VisitorManager(mockContext)
        assertEquals("https://visitor-service.tealiumiq.com/test-account/testingProfile/visitorId", visitorProfileManager.generateVisitorServiceUrl())
    }

    @Test
    fun generateVisitorServiceUrl_ReplacesPlaceholders_OverrideProfile() {
        every { mockConfig.overrideVisitorServiceUrl } returns "my.url/{{profile}}/{{visitorId}}"
        every { mockConfig.visitorServiceOverrideProfile } returns "testingProfile"
        val visitorProfileManager = VisitorManager(mockContext)
        assertEquals("my.url/testingProfile/visitorId", visitorProfileManager.generateVisitorServiceUrl())
    }

    private val validExampleProfileString = """
        {
            "badges":{
                "5097":true
            },
            "dates":{
                "23":1598371096000,
                "24":1598371096000
            },
            "metrics":{
                "22":6,
                "5121":6,
                "15":1,
                "28":0,
                "21":1
            },
            "properties":{
                "profile":"android",
                "account":"tealiummobile",
                "17":"https:\\/\\/tags.tiqcdn.com\\/utag\\/tealiummobile\\/android\\/dev\\/mobile.html"
            },
            "current_visit":{
                "dates":{
                    "10":1598371096000
                },
                "flags":{
                    "14":true
                },
                "metrics":{
                    "7":6
                },
                "properties":{
                    "45":"Android",
                    "46":"Android",
                    "47":"mobile application"
                },
                "property_sets":{
                    "50":[
                        "Android"
                    ],
                    "51":[
                        "Android"
                    ],
                    "52":[
                        "mobile application"
                    ]
                }
                
            }
        }""".trimIndent()
}