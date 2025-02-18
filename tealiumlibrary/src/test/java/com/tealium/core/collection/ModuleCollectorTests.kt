package com.tealium.core.collection

import com.tealium.core.Module
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ModuleCollectorTests {

    private lateinit var enabledModuleWithConstantVersion: Module
    private lateinit var enabledModuleWithVersion: Module
    private lateinit var enabledModuleWithoutVersion: Module
    private lateinit var disabledModuleWithConstantVersion: Module
    private lateinit var disabledModuleWithVersion: Module
    private lateinit var disabledModuleWithoutVersion: Module
    private lateinit var moduleList: List<Module>
    private lateinit var moduleCollector: ModuleCollector

    @Before
    fun setUp() {
        enabledModuleWithConstantVersion = ModuleWithConstantVersion("enabled-module-with-constant-version", true)
        enabledModuleWithVersion = ModuleWithVersion("enabled-module-with-version", true, "1.0.0")
        enabledModuleWithoutVersion = ModuleWithoutVersion("enabled-module-without-version", true)

        disabledModuleWithConstantVersion = ModuleWithConstantVersion("disabled-module-with-constant-version", false)
        disabledModuleWithVersion = ModuleWithVersion("disabled-module-with-version", false, "1.1.0")
        disabledModuleWithoutVersion = ModuleWithoutVersion("disabled-module-without-version", false)

        moduleList = listOf(
                enabledModuleWithConstantVersion,
                enabledModuleWithVersion,
                enabledModuleWithoutVersion,
                disabledModuleWithConstantVersion,
                disabledModuleWithVersion,
                disabledModuleWithoutVersion)

        moduleCollector = ModuleCollector(moduleList)
    }

    @Test
    @Suppress("unchecked_cast")
    fun collect_ReturnsOnly_EnabledModules() = runBlocking {
        val collected = moduleCollector.collect()
        val moduleNames = collected["enabled_modules"] as List<String>
        val moduleVersions = collected["enabled_modules_versions"] as List<String>

        assertEquals(moduleNames.size, moduleVersions.size)
        assertTrue(moduleNames.contains("enabled-module-with-constant-version"))
        assertTrue(moduleNames.contains("enabled-module-with-version"))
        assertTrue(moduleVersions.contains("1.2.3"))
        assertTrue(moduleVersions.contains("1.0.0"))

        assertNull(moduleNames.find { name -> name.contains("disabled") })
    }

    @Test
    @Suppress("unchecked_cast")
    fun collect_ReturnsOnly_ModulesWithVersions() = runBlocking {
        val collected = moduleCollector.collect()
        val moduleNames = collected["enabled_modules"] as List<String>
        val moduleVersions = collected["enabled_modules_versions"] as List<String>

        assertEquals(moduleNames.size, moduleVersions.size)
        assertTrue(moduleNames.contains("enabled-module-with-constant-version"))
        assertTrue(moduleNames.contains("enabled-module-with-version"))
        assertTrue(moduleVersions.contains("1.2.3"))
        assertTrue(moduleVersions.contains("1.0.0"))

        assertNull(moduleNames.find { name -> name.contains("without-version") })

    }

    @Test
    @Suppress("unchecked_cast")
    fun collect_Returns_SortedData() = runBlocking {
        moduleCollector = ModuleCollector(moduleList.asReversed())
        val collected = moduleCollector.collect()
        val moduleNames = collected["enabled_modules"] as List<String>
        val moduleVersions = collected["enabled_modules_versions"] as List<String>

        assertEquals(2, moduleNames.size)
        assertEquals(2, moduleVersions.size)
        assertEquals("enabled-module-with-constant-version", moduleNames[0])
        assertEquals("enabled-module-with-version", moduleNames[1])
        assertEquals("1.2.3", moduleVersions[0])
        assertEquals("1.0.0", moduleVersions[1])
    }
}

open class ModuleWithoutVersion(override val name: String, override var enabled: Boolean): Module

class ModuleWithConstantVersion(name: String, enabled: Boolean): ModuleWithoutVersion(name, enabled) {
    companion object {
        const val MODULE_VERSION = "1.2.3"
    }
}

class ModuleWithVersion(
        name: String,
        enabled: Boolean,
        @JvmField val MODULE_VERSION: String): ModuleWithoutVersion(name, enabled)