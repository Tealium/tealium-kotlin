package com.tealium.core

import com.tealium.core.messaging.LibrarySettingsUpdatedListener
import com.tealium.core.settings.LibrarySettings
import com.tealium.core.validation.DispatchValidator
import com.tealium.dispatcher.Dispatcher
import org.json.JSONObject
import java.lang.Exception
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class MutableModuleManager(moduleList: List<Module>): ModuleManager(moduleList) {
    fun add(module: Module) {
        allModules[module.name] = module
    }

    fun remove(module: Module) {
        allModules.remove(module.name)
    }
}

/**
 * Provides a central repository for all modules used within a [Tealium] Instance.
 */
open class ModuleManager(moduleList: List<Module>): LibrarySettingsUpdatedListener {

    protected val allModules: MutableMap<String, Module> = Collections.synchronizedMap(
        moduleList.associateBy { it.name }.toMutableMap()
    )

    /**
     * Fetches all modules of the given type e.g. Collector/Dispatcher classes may have many different
     * implementations.
     *
     * @return all modules of the given type; else and empty set.
     */
    fun <T: Module> getModulesForType(clazz: Class<T>): Set<T> {
        return synchronized(allModules) {
            allModules.values.filterIsInstance(clazz).toSet()
        }
    }

    /**
     * Fetches the first module for the given class. Useful for modules that should only have a single
     * implementation (ConsentManagement/VisitorService etc).
     *
     * @return the first module for the given class.
     */
    fun <T: Module> getModule(clazz: Class<T>): T? {
        return synchronized(allModules) {
            getModulesForType(clazz).firstOrNull()
        }
    }


    /**
     * Fetches a module using the provided name.
     *
     * @return the Module for the given name; else null
     */
    fun getModule(name: String): Module? {
        return synchronized(allModules) { allModules[name] }
    }

    /**
     * Responds to an updated set of Library Settings.
     */
    override fun onLibrarySettingsUpdated(settings: LibrarySettings) {
        if (settings.disableLibrary) {
            synchronized(allModules) {
                allModules.forEach {
                    it.value.enabled = false
                }
            }
        }
        //TODO - finish the updating of enabled/disabled logic for things other than just the SDK Disable. i.e. Tagmanagement/Collect enable/disable.
    }

    /**
     * Prints a JSON representation of all the modules as well as their enabled/disabled status.
     */
    override fun toString(): String {
        val json = JSONObject()
        val types = listOf(Collector::class.java,
                DispatchValidator::class.java,
                Dispatcher::class.java)
        try {
            types.forEach { type ->
                val modules = getModulesForType(type)
                val obj = JSONObject()
                modules.forEach { m ->
                    obj.put(m.name, if (m.enabled) "enabled" else "disabled" )
                }
                json.put(type.simpleName, obj)
            }
        } catch (ignore: Exception) {

        }

        return json.toString(4)
    }
}