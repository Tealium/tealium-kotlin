package com.tealium.visitorservice.momentsapi

import com.tealium.core.Module
import com.tealium.core.ModuleFactory
import com.tealium.core.Modules
import com.tealium.core.Tealium
import com.tealium.core.TealiumContext
import com.tealium.visitorservice.BuildConfig
import java.lang.Exception

class MomentsApiService @JvmOverloads constructor(
    private val context: TealiumContext,
    private val momentsApiManager: MomentsApiManager = MomentsManager(context)
) : Module {

    override var enabled: Boolean = true // change
    override val name: String = MODULE_NAME

    fun requestVisitorDataForEngine(engineId: String, responseListener: ResponseListener<EngineResponse>) {
        momentsApiManager.fetchEngineResponse(engineId, responseListener)
    }

    companion object : ModuleFactory {
        const val MODULE_NAME = "MomentsApi"
        const val MODULE_VERSION = BuildConfig.LIBRARY_VERSION

        override fun create(context: TealiumContext): Module {
            context.config.momentsApiRegion?.let {
                return MomentsApiService(context)
            }
                throw Exception("MomentsApi must have a region assigned. Ensure you have set one on TealiumConfig.")
        }
    }
}

val Modules.MomentsApi: ModuleFactory
    get() = MomentsApiService

/**
 * Returns the MomentsApi module for this Tealium instance.
 */
val Tealium.momentsApi: MomentsApiService?
    get() = modules.getModule(MomentsApiService.MODULE_NAME) as? MomentsApiService