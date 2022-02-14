package com.tealium.core.collection

import com.tealium.core.Collector
import com.tealium.core.Module
import com.tealium.dispatcher.Dispatch
import java.lang.Exception

internal class ModuleCollector(modules: List<Module>) : Collector {

    private val modulesWithVersions: MutableList<Module> = mutableListOf()
    private val moduleVersions: MutableMap<String, String> = mutableMapOf()

    init {
        modules.forEach { module ->
            val moduleClass = module::class.java
            try {
                // Backwards compatible until the Module interface can safely have a version
                // property enforced.
                val versionCodeField = moduleClass.getDeclaredField("MODULE_VERSION")
                val versionCode = try {
                    // try static prop first
                    versionCodeField.get(moduleClass) as? String
                } catch (ignored: Exception) {
                    // try instance prop last
                    versionCodeField.get(module) as? String
                }

                if (versionCode != null) {
                    modulesWithVersions.add(module)
                    moduleVersions[module.name] = versionCode
                }
            } catch (ignored: Exception) {
                // Could fail for several reasons, but we're only interested in Modules
                // that provide a version number
            }
        }
    }

    override suspend fun collect(): Map<String, Any> {
        val sortedEnabledModules = modulesWithVersions.filter { module -> module.enabled }
            .sortedBy { module -> module.name }

        return mapOf(
            Dispatch.Keys.ENABLED_MODULES to sortedEnabledModules.map { module -> module.name },
            Dispatch.Keys.ENABLED_MODULES_VERSIONS to sortedEnabledModules.map { module -> moduleVersions[module.name] }
        )
    }

    override val name: String
        get() = "ModuleCollector"
    override var enabled: Boolean = true
}