package com.tealium.core

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.messaging.ExternalListener
import com.tealium.core.messaging.Messenger
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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
        assertEquals(tealium.visitorId, tealium.dataLayer.getString("tealium_visitor_id"))
    }

    @Test
    fun testVisitorIdIsReset() {
        val vid = tealium.visitorId
        assertNotNull(vid)
        assertEquals(32, tealium.visitorId.length)
        assertEquals(tealium.visitorId, tealium.dataLayer.getString("tealium_visitor_id"))
        val storedVid = tealium.dataLayer.getString("tealium_visitor_id")

        val resetVid = tealium.resetVisitorId()
        val storedResetVid = tealium.dataLayer.getString("tealium_visitor_id")
        assertNotEquals(vid, resetVid)
        assertNotEquals(storedVid, storedResetVid)
        assertEquals(32, tealium.visitorId.length)
        assertEquals(tealium.visitorId, tealium.dataLayer.getString("tealium_visitor_id"))
    }

    @Test
    fun existingVisitorId() {
        val config = TealiumConfig(
            application,
            "testAccount",
            "testProfile",
            Environment.DEV
        )
        config.existingVisitorId = "testExistingVisitorId"
        val test = Tealium.create("tester", config)

        val vid = test.visitorId
        assertNotNull(vid)
        assertEquals("testExistingVisitorId", test.visitorId)
        assertEquals(test.visitorId, test.dataLayer.getString("tealium_visitor_id"))
    }

    @Test
    fun resetExistingVisitorId() {
        val config = TealiumConfig(
            application,
            "testAccount2",
            "testProfile2",
            Environment.DEV
        )
        config.existingVisitorId = "testExistingVisitorId"
        val teal = Tealium.create("tester2", config)

        val vid = teal.visitorId
        val storedVid = teal.dataLayer.getString("tealium_visitor_id")
        assertEquals("testExistingVisitorId", vid)

        val resetVid = teal.resetVisitorId()
        val storedResetVid = teal.dataLayer.getString("tealium_visitor_id")

        assertNotEquals(vid, resetVid)
        assertNotEquals(storedVid, storedResetVid)
        assertEquals(teal.visitorId, teal.dataLayer.getString("tealium_visitor_id"))
    }

    @Test
    fun testCallbackGetsExecuted() = runBlocking {
        var hasBeenCalled = false

        val tealium = Tealium.create("name", configWithNoModules) {
            hasBeenCalled = true
        }
        if (!hasBeenCalled) {
            delay(1000)
            assertTrue(hasBeenCalled)
        }
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

    companion object: CollectorFactory {
        var payload: Any? = null
        override fun create(context: TealiumContext): Collector {
            return TestFactory(context, payload)
        }
    }
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