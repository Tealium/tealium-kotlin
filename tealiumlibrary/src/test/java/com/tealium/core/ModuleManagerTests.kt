package com.tealium.core

import com.tealium.core.validation.DispatchValidator
import com.tealium.dispatcher.Dispatcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import junit.framework.Assert.*
import org.junit.Before
import org.junit.Test

class ModuleManagerTests {

    private lateinit var moduleManager: ModuleManager

    private lateinit var mockMultiModule: MultiModule
    private lateinit var mockCollector: Collector
    private lateinit var mockValidator: DispatchValidator
    private lateinit var mockDispatcher: Dispatcher

    @Before
    fun setUp() {
        mockCollector = mockk()
        mockValidator = mockk()
        mockDispatcher = mockk()
        every { mockCollector.name } returns "mock_collector"
        every { mockValidator.name } returns "mock_validator"
        every { mockDispatcher.name } returns "mock_dispatcher"

        mockMultiModule = mockk()
        every { mockMultiModule.name } returns "mock_multimodule"

        moduleManager = spyk(ModuleManager(listOf(mockCollector, mockDispatcher, mockValidator, mockMultiModule)))
    }

    @Test
    fun testRetrieveModuleByClass() {
        val collector = moduleManager.getModule(Collector::class.java)
        assertNotNull(collector)
        assertSame(mockCollector, collector)

        val validator = moduleManager.getModule(DispatchValidator::class.java)
        assertNotNull(validator)
        assertSame(mockValidator, validator)

        val dispatcher = moduleManager.getModule(Dispatcher::class.java)
        assertNotNull(dispatcher)
        assertSame(mockDispatcher, dispatcher)

        val multiModule = moduleManager.getModule(MultiModule::class.java)
        assertNotNull(multiModule)
        assertSame(mockMultiModule, multiModule)
    }

    @Test
    fun testRetrieveMultiModuleByEachImplementedClass() {
        moduleManager = ModuleManager(listOf(mockMultiModule))

        val byClass = moduleManager.getModule(MultiModule::class.java)
        assertSame(mockMultiModule, byClass)

        val byCollector = moduleManager.getModule(Collector::class.java)
        assertSame(mockMultiModule, byCollector)

        val byValidator = moduleManager.getModule(DispatchValidator::class.java)
        assertSame(mockMultiModule, byValidator)
    }

    @Test
    fun testRetrieveModuleByName() {
        val collector = moduleManager.getModule("mock_collector")
        assertSame(mockCollector, collector)

        val validator = moduleManager.getModule("mock_validator")
        assertSame(mockValidator, validator)

        val dispatcher = moduleManager.getModule("mock_dispatcher")
        assertSame(mockDispatcher, dispatcher)

        val multiModule = moduleManager.getModule("mock_multimodule")
        assertSame(mockMultiModule, multiModule)
    }

    @Test
    fun testRetrieveAllModulesByClass() {
        val collectors = moduleManager.getModulesForType(Collector::class.java)
        assertEquals(2, collectors.size)
        assertTrue(collectors.contains(mockCollector))
        assertTrue(collectors.contains(mockMultiModule))

        val validators = moduleManager.getModulesForType(DispatchValidator::class.java)
        assertEquals(2, validators.size)
        assertTrue(validators.contains(mockValidator))
        assertTrue(validators.contains(mockMultiModule))

        val dispatchers = moduleManager.getModulesForType(Dispatcher::class.java)
        assertEquals(1, dispatchers.size)
        assertTrue(dispatchers.contains(mockDispatcher))

        val nonExistentModules = moduleManager.getModulesForType(NonExistentModule::class.java)
        assertEquals(0, nonExistentModules.size)
    }
}

private abstract class MultiModule: Collector, DispatchValidator
private abstract class NonExistentModule: Module