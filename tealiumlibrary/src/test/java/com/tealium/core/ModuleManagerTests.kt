package com.tealium.core

import com.tealium.core.settings.LibrarySettings
import com.tealium.core.validation.DispatchValidator
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.Dispatcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ModuleManagerTests {

    private lateinit var moduleManager: MutableModuleManager

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

        moduleManager = spyk(
            MutableModuleManager(
                listOf(
                    mockCollector,
                    mockDispatcher,
                    mockValidator,
                    mockMultiModule
                )
            )
        )
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
        moduleManager = MutableModuleManager(listOf(mockMultiModule))

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

    @Test
    fun testMultiThreadedAccessDoesNotCrash() {
        val mutableModuleManager = MutableModuleManager(
            listOf(
                mockCollector,
                mockDispatcher,
                mockValidator,
                mockMultiModule
            )
        )
        val repeats = 1000
        var error = false

        val getterThread = Thread {
            repeat(repeats) {
                try {
                    val module = mutableModuleManager.getModule(Module::class.java)
                    println("retrieved ${module?.name}")
                } catch (e: Exception) {
                    error = true
                }
            }
        }
        val creatorThread = Thread {
            repeat(repeats) {
                try {
                    val module = object : Module {
                        override val name: String = "mock_module_$it"
                        override var enabled: Boolean = true
                    }
                    println("adding ${module.name}")
                    mutableModuleManager.add(module)

                } catch (e: Exception) {
                    error = true
                }
            }
        }
        creatorThread.start()
        getterThread.start()

        getterThread.join()
        creatorThread.join()

        assertFalse(error)
    }

    @Test
    fun onLibrarySettingsUpdated_Disables_CollectDispatcher() {
        val collectDispatcher = mockDispatcher("Collect")
        moduleManager.add(collectDispatcher)

        moduleManager.onLibrarySettingsUpdated(LibrarySettings(collectDispatcherEnabled = false))

        verify {
            collectDispatcher.enabled = false
        }
    }

    @Test
    fun onLibrarySettingsUpdated_Disables_LegacyCollectDispatcher() {
        val legacyCollectDispatcher = mockDispatcher("COLLECT_DISPATCHER")
        moduleManager.add(legacyCollectDispatcher)

        moduleManager.onLibrarySettingsUpdated(LibrarySettings(collectDispatcherEnabled = false))

        verify {
            legacyCollectDispatcher.enabled = false
        }
    }

    @Test
    fun onLibrarySettingsUpdated_Enables_CollectDispatcher() {
        val collectDispatcher = mockDispatcher("Collect")
        moduleManager.add(collectDispatcher)

        moduleManager.onLibrarySettingsUpdated(LibrarySettings(collectDispatcherEnabled = true))

        verify {
            collectDispatcher.enabled = true
        }
    }

    @Test
    fun onLibrarySettingsUpdated_Enables_LegacyCollectDispatcher() {
        val legacyCollectDispatcher = mockDispatcher("COLLECT_DISPATCHER")
        moduleManager.add(legacyCollectDispatcher)

        moduleManager.onLibrarySettingsUpdated(LibrarySettings(collectDispatcherEnabled = true))

        verify {
            legacyCollectDispatcher.enabled = true
        }
    }

    @Test
    fun onLibrarySettingsUpdated_Disables_TagManagementDispatcher() {
        val tagManagementDispatcher = mockDispatcher("TagManagement")
        moduleManager.add(tagManagementDispatcher)

        moduleManager.onLibrarySettingsUpdated(LibrarySettings(tagManagementDispatcherEnabled = false))

        verify {
            tagManagementDispatcher.enabled = false
        }
    }

    @Test
    fun onLibrarySettingsUpdated_Disables_LegacyTagManagementDispatcher() {
        val legacyTagManagementDispatcher = mockDispatcher("TAG_MANAGEMENT_DISPATCHER")
        moduleManager.add(legacyTagManagementDispatcher)

        moduleManager.onLibrarySettingsUpdated(LibrarySettings(tagManagementDispatcherEnabled = false))

        verify {
            legacyTagManagementDispatcher.enabled = false
        }
    }

    @Test
    fun onLibrarySettingsUpdated_Enables_TagManagementDispatcher() {
        val tagManagementDispatcher = mockDispatcher("TagManagement")
        moduleManager.add(tagManagementDispatcher)

        moduleManager.onLibrarySettingsUpdated(LibrarySettings(tagManagementDispatcherEnabled = true))

        verify {
            tagManagementDispatcher.enabled = true
        }
    }

    @Test
    fun onLibrarySettingsUpdated_Enables_LegacyTagManagementDispatcher() {
        val legacyTagManagementDispatcher = mockDispatcher("TAG_MANAGEMENT_DISPATCHER")
        moduleManager.add(legacyTagManagementDispatcher)

        moduleManager.onLibrarySettingsUpdated(LibrarySettings(tagManagementDispatcherEnabled = true))

        verify {
            legacyTagManagementDispatcher.enabled = true
        }
    }

    @Test
    fun onLibrarySettingsUpdated_Disables_Collect_And_TagManagement_When_LibraryDisabled() {
        val collectDispatcher = mockDispatcher("Collect")
        val tagManagementDispatcher = mockDispatcher("TagManagement")
        moduleManager = MutableModuleManager(listOf(collectDispatcher, tagManagementDispatcher))

        moduleManager.onLibrarySettingsUpdated(
            LibrarySettings(
                disableLibrary = true,
                collectDispatcherEnabled = true,
                tagManagementDispatcherEnabled = true
            )
        )

        verify {
            collectDispatcher.enabled = false
            tagManagementDispatcher.enabled = false
        }
        verify(inverse = true) {
            collectDispatcher.enabled = true
            tagManagementDispatcher.enabled = true
        }
    }

    private fun mockDispatcher(name: String, enabled: Boolean = true): Dispatcher {
        val dispatcher = object : Dispatcher {
            override val name: String
                get() = name
            override var enabled: Boolean = enabled

            override suspend fun onDispatchSend(dispatch: Dispatch) {
            }

            override suspend fun onBatchDispatchSend(dispatches: List<Dispatch>) {
            }
        }

        return spyk(dispatcher)
    }
}

private abstract class MultiModule : Collector, DispatchValidator
private abstract class NonExistentModule : Module