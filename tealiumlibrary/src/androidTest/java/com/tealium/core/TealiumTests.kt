package com.tealium.core

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.consent.ConsentCategory
import com.tealium.core.consent.ConsentPolicy
import com.tealium.core.consent.consentManagerPolicy
import com.tealium.core.messaging.ExternalListener
import com.tealium.core.messaging.Messenger
import com.tealium.core.messaging.NewSessionListener
import com.tealium.core.persistence.Expiry
import com.tealium.core.settings.LibrarySettings
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TealiumTests {

    lateinit var tealium: Tealium
    val application = ApplicationProvider.getApplicationContext<Application>()
    val configWithNoModules = TealiumConfig(
        application,
        "test",
        "test",
        Environment.DEV
    )

    @Before
    fun setUp() {
        tealium = Tealium.create("name", configWithNoModules)
    }

    @After
    fun tearDown() {
        Tealium.names().forEach {
            Tealium[it]?.dataLayer?.clear()
            Tealium.destroy(it)
        }
    }

    @Test
    fun testConfig_LogLevel_SetFromEnvironment() {
        Tealium.create(
            "loglevel", TealiumConfig(
                application,
                configWithNoModules.accountName,
                configWithNoModules.profileName,
                Environment.PROD
            )
        )
        assertEquals(Logger.logLevel, LogLevel.PROD)

        Tealium.create(
            "loglevel", TealiumConfig(
                application,
                configWithNoModules.accountName,
                configWithNoModules.profileName,
                Environment.QA
            )
        )
        assertEquals(Logger.logLevel, LogLevel.QA)

        Tealium.create(
            "loglevel", TealiumConfig(
                application,
                configWithNoModules.accountName,
                configWithNoModules.profileName,
                Environment.DEV
            )
        )
        assertEquals(Logger.logLevel, LogLevel.DEV)
    }

    @Test
    fun testConfig_LogLevel_OverridesEnvironment() {
        Tealium.create(
            "loglevel", TealiumConfig(
                application,
                configWithNoModules.accountName,
                configWithNoModules.profileName,
                Environment.PROD
            ).apply { logLevel = LogLevel.DEV }
        )
        assertEquals(Logger.logLevel, LogLevel.DEV)

        Tealium.create(
            "loglevel", TealiumConfig(
                application,
                configWithNoModules.accountName,
                configWithNoModules.profileName,
                Environment.QA
            ).apply { logLevel = LogLevel.PROD }
        )
        assertEquals(Logger.logLevel, LogLevel.PROD)

        Tealium.create(
            "loglevel", TealiumConfig(
                application,
                configWithNoModules.accountName,
                configWithNoModules.profileName,
                Environment.DEV
            ).apply { logLevel = LogLevel.QA }
        )
        assertEquals(Logger.logLevel, LogLevel.QA)
    }

    @Test
    fun testVisitorIdIsGenerated() {
        assertNotNull(tealium.visitorId)
        assertEquals(32, tealium.visitorId.length)
    }

    @Test
    fun testVisitorIdIsReset() = runBlocking {
        val vid = tealium.visitorId
        assertNotNull(vid)
        assertEquals(32, tealium.visitorId.length)
        assertEquals(tealium.visitorId, tealium.dataLayer.getString("tealium_visitor_id"))
        val storedVid = tealium.dataLayer.getString("tealium_visitor_id")

        val resetVid = tealium.resetVisitorId()
        delay(100) // reset is async
        val storedResetVid = tealium.dataLayer.getString("tealium_visitor_id")
        assertNotEquals(vid, resetVid)
        assertNotEquals(storedVid, storedResetVid)
        assertEquals(32, tealium.visitorId.length)
        assertEquals(tealium.visitorId, tealium.dataLayer.getString("tealium_visitor_id"))
    }

    @Test
    fun existingVisitorId() = runBlocking {
        val config = TealiumConfig(
            application,
            "testAccount",
            "testProfile",
            Environment.DEV
        )
        config.existingVisitorId = "testExistingVisitorId"
        deleteInstanceStorage(config)
        val test = awaitCreateTealium("tester", config)

        val vid = test.visitorId
        assertNotNull(vid)
        assertEquals("testExistingVisitorId", test.visitorId)
        assertEquals(test.visitorId, test.dataLayer.getString("tealium_visitor_id"))
    }

    @Test
    fun resetExistingVisitorId() = runBlocking {
        val config = TealiumConfig(
            application,
            "testAccount2",
            "testProfile2",
            Environment.DEV
        )
        config.existingVisitorId = "testExistingVisitorId"
        deleteInstanceStorage(config)
        val teal = awaitCreateTealium("tester2", config)

        val vid = teal.visitorId
        val storedVid = teal.dataLayer.getString("tealium_visitor_id")
        assertEquals("testExistingVisitorId", vid)

        val resetVid = teal.resetVisitorId()
        delay(100) // reset is async
        val storedResetVid = teal.dataLayer.getString("tealium_visitor_id")

        assertNotEquals(vid, resetVid)
        assertNotEquals(storedVid, storedResetVid)
        assertEquals(teal.visitorId, teal.dataLayer.getString("tealium_visitor_id"))
    }

    @Test
    fun retrieveDataLayerWithoutCollectors() = runBlocking {
        val config = TealiumConfig(
            application,
            "testAccount2",
            "testProfile2",
            Environment.DEV
        )
        deleteInstanceStorage(config)

        config.existingVisitorId = "testExistingVisitorId"

        val teal = awaitCreateTealium("tester2", config) {
            val data = gatherTrackData()
            val storedVid = data["tealium_visitor_id"]
            assertEquals("testExistingVisitorId", storedVid)

            val account = data["tealium_account"]
            val profile = data["tealium_profile"]

            assertEquals("testAccount2", account)
            assertEquals("testProfile2", profile)
        }
    }

    @Test
    fun testCallbackGetsExecuted() = runBlocking {
        var hasBeenCalled = false

        val tealium = awaitCreateTealium("name", configWithNoModules) {
            hasBeenCalled = true
        }

        assertTrue(hasBeenCalled)
    }

    @Test
    fun testCompanion_CreateAndGet() {
        val instance = Tealium.create("instance_1", configWithNoModules)
        assertSame(instance, Tealium["instance_1"])
        assertSame(instance, Tealium.get("instance_1"))
    }

    @Test
    fun testCompanion_CreateAndDestroy() {
        val instance = Tealium.create("instance_1", configWithNoModules)
        assertSame(instance, Tealium["instance_1"])
        assertSame(instance, Tealium.get("instance_1"))

        Tealium.destroy("instance_1")
        assertNull(Tealium["instance_1"])
        assertNull(Tealium.get("instance_1"))
    }

    @Test
    fun testCompanion_CreateMultipleWithSameConfig() {
        val instance = Tealium.create("instance_1", configWithNoModules)
        val instance2 = Tealium.create("instance_2", configWithNoModules)
        assertSame(instance, Tealium["instance_1"])
        assertSame(instance, Tealium.get("instance_1"))
        assertSame(instance2, Tealium["instance_2"])
        assertSame(instance2, Tealium.get("instance_2"))

        Tealium.destroy("instance_1")
        assertNull(Tealium["instance_1"])
        assertNull(Tealium.get("instance_1"))

        assertNotNull(Tealium["instance_2"])
        assertNotNull(Tealium.get("instance_2"))
    }

    @Test
    fun testCompanion_CreateMultipleWithDifferentConfig() {
        val instance = Tealium.create("instance_1", configWithNoModules)
        val instance2 = Tealium.create(
            "instance_2",
            TealiumConfig(application, "test2", "test2", Environment.DEV)
        )
        assertSame(instance, Tealium["instance_1"])
        assertSame(instance, Tealium.get("instance_1"))
        assertSame(instance2, Tealium["instance_2"])
        assertSame(instance2, Tealium.get("instance_2"))

        Tealium.destroy("instance_1")
        assertNull(Tealium["instance_1"])
        assertNull(Tealium.get("instance_1"))

        assertNotNull(Tealium["instance_2"])
        assertNotNull(Tealium.get("instance_2"))
    }

    @Test
    fun testCompanion_NamesReturnsAllNames() {
        Tealium.create("instance_1", configWithNoModules)
        Tealium.create("instance_2", TealiumConfig(application, "test2", "test2", Environment.DEV))
        val names = Tealium.names()

        assertTrue(names.contains("instance_1"))
        assertTrue(names.contains("instance_2"))
    }

    @Test
    fun testEvents_GetSubscribed_FromConfig() {
        val listener = mockk<TestListener>()
        every { listener.onListen(any()) } just Runs

        val result = true
        TestFactory.payload = result

        val config: TealiumConfig =
            TealiumConfig(
                application,
                "test2",
                "test2",
                Environment.DEV,
                collectors = mutableSetOf(TestFactory)
            ).apply {
                events.add(
                    listener
                )
            }
        Tealium.create("test", config)

        verify {
            listener.onListen(result)
        }
    }

    @Test
    fun testGatherTrackData() = runBlocking {
        val config = TealiumConfig(application, "", "", Environment.DEV, "")
        config.consentManagerPolicy = ConsentPolicy.GDPR
        config.collectors.add(
            collectorFactory(
                mapOf(
                    "String" to "string",
                    "Int" to 10,
                    "Double" to 10.0,
                    "Float" to 10.0f,
                    "Boolean" to true,
                    "JsonArray" to JSONArray().apply { put("string") },
                    "JsonObject" to JSONObject().apply { put("string", "string") },
                )
            )
        )
        val tealium = awaitCreateTealium("name", config)

        tealium.consentManager.userConsentCategories = mutableSetOf(
            ConsentCategory.AFFILIATES,
            ConsentCategory.ANALYTICS,
            ConsentCategory.BIG_DATA
        )

        val data = tealium.gatherTrackData()
        assertFalse(data.isEmpty())
        assertTrue(containsOnlyValidTypes(data.values))
    }

    @Test(expected = AssertionError::class)
    fun testGatherTrackDataFails() = runBlocking {
        val tealium = awaitCreateTealium(
            "name",
            TealiumConfig(application, "test", "test", Environment.DEV).apply {
                collectors.add(
                    collectorFactory(
                        mapOf(
                            "consent" to listOf<ConsentCategory>(
                                ConsentCategory.CDP,
                                ConsentCategory.CRM
                            ),
                            "policy" to ConsentPolicy.GDPR
                        )
                    )
                )
            })

        val data = tealium.gatherTrackData()
        assertFalse(data.isEmpty())
        assertTrue(containsOnlyValidTypes(data.values))
    }

    @Test
    fun test_NewSessionEvents_AreBufferedOnLaunch() = runBlocking {
        val newSessionListener = mockk<NewSessionListener>(relaxed = true)
        val config = TealiumConfig(application, "test", "test", Environment.DEV).apply {
            collectors.add(object : CollectorFactory {
                override fun create(context: TealiumContext): Collector {
                    return SessionListenerModule(newSessionListener)
                }
            })
            events.add(newSessionListener)
        }
        deleteSessionInfo(config)

        awaitCreateTealium(
            "name",
            config
        )

        verify(exactly = 2) {
            newSessionListener.onNewSession(any())
        }
    }

    @Test
    fun test_SessionScopedData_IsRemovedOnLaunch_WhenNewSession() = runBlocking {
        val config = TealiumConfig(application, "test", "test", Environment.DEV)

        var tealium: Tealium = awaitCreateTealium("name", config) {
            dataLayer.putInt("session_int", 10, Expiry.SESSION)
        }

        assertEquals(10, tealium.dataLayer.getInt("session_int"))
        assertEquals(Expiry.SESSION, tealium.dataLayer.getExpiry("session_int"))

        deleteSessionInfo(config)

        tealium = awaitCreateTealium("name", config)

        assertNull(tealium.dataLayer.getInt("session_int"))
        assertNull(tealium.dataLayer.getExpiry("session_int"))
    }

    @Test
    fun test_SessionScopedData_WrittenByModules_IsNotRemovedOnLaunch_WhenNewSession() =
        runBlocking {

            val config = TealiumConfig(application, "test", "test", Environment.DEV)
            deleteSessionInfo(config)

            config.modules.add(object : ModuleFactory {
                override fun create(context: TealiumContext): Module {
                    return DataWritingModule(context)
                }
            })
            val tealium: Tealium = awaitCreateTealium("name", config)

            assertEquals(10, tealium.dataLayer.getInt("session_int"))
            assertEquals(Expiry.SESSION, tealium.dataLayer.getExpiry("session_int"))
        }

    private fun containsOnlyValidTypes(data: Collection<*>): Boolean {
        return data.filterNotNull().fold(true) { initial, entry ->
            initial && when (entry) {
                is Long, is Boolean, is Int, is String, is Double, is Float, is JSONArray, is JSONObject -> true
                is Collection<*> -> containsOnlyValidTypes(entry)
                is Map<*, *> -> containsOnlyValidTypes(entry.values)
                else -> false
            }
        }
    }

    private fun deleteInstanceStorage(config: TealiumConfig) {
        try {
            config.tealiumDirectory.deleteRecursively()
        } catch (ignored: Exception) {
        }
    }

    private fun deleteSessionInfo(config: TealiumConfig) {
        application.getSharedPreferences(SessionManager.sharedPreferencesName(config), 0).edit()
            .clear()
            .commit()
    }

}

suspend fun awaitCreateTealium(
    name: String,
    config: TealiumConfig,
    onReady: Tealium.() -> Unit = {}
): Tealium {
    return suspendCancellableCoroutine { cont ->
        Tealium.create(name, config) {
            onReady.invoke(this)
            cont.resume(this, null)
        }
    }
}

private class TestFactory(context: TealiumContext, payload: Any?) : Collector {

    init {
        context.events.send(
            TestMessenger(payload)
        )
    }

    override suspend fun collect(): Map<String, Any> {
        return mapOf()
    }

    override val name: String = "Test"
    override var enabled: Boolean = true

    companion object : CollectorFactory {
        var payload: Any? = null
        override fun create(context: TealiumContext): Collector {
            return TestFactory(context, payload)
        }
    }
}

private class SessionListenerModule(
    private val listener: NewSessionListener
) : Collector, NewSessionListener by listener {

    override suspend fun collect(): Map<String, Any> {
        return mapOf()
    }

    override val name: String = "TestListenerFactory"
    override var enabled: Boolean = true
}

private class DataWritingModule(
    context: TealiumContext
) : Module {

    init {
        context.dataLayer.putInt("session_int", 10, Expiry.SESSION)
    }

    override val name: String = "TestListenerFactory"
    override var enabled: Boolean = true
}

private interface TestListener : ExternalListener {
    fun onListen(result: Any?)
}

private class TestMessenger(private val result: Any?) :
    Messenger<TestListener>(TestListener::class) {
    override fun deliver(listener: TestListener) {
        listener.onListen(result = result)
    }
}

private fun collectorFactory(
    returnData: Map<String, Any>,
    enabled: Boolean = true,
    name: String = "test-collector"
): CollectorFactory {
    return object : CollectorFactory, Collector {
        override suspend fun collect(): Map<String, Any> {
            return returnData
        }

        override fun create(context: TealiumContext): Collector {
            return this
        }

        override val name: String
            get() = name
        override var enabled: Boolean = enabled
    }
}