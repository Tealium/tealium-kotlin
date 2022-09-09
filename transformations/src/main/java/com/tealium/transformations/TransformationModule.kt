package com.tealium.transformations

import com.tealium.core.Module
import com.tealium.core.ModuleFactory
import com.tealium.core.Modules
import com.tealium.core.Tealium
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.Transformer
import com.tealium.dispatcher.Dispatch
import com.tealium.transformations.internal.TransformationsAdapter
import com.tealium.transformations.internal.TransformationsAdapterFactory

interface TransformationModule : Module, TransformationsAdapter {

    companion object Factory : ModuleFactory {
        const val MODULE_NAME = "Transformations"

        override fun create(context: TealiumContext): TransformationModule {
            val runtime = context.config.transformationsRuntime
            val adapterFactory = context.config.transformationsAdapter
            if (adapterFactory == null) throw IllegalArgumentException("TealiumConfig.transformationAdapter must be provided.")
            val transformationsAdapter = adapterFactory.create(context)
            return TransformationModuleImpl(
                transformationsAdapter
            )
        }
    }
}

private class TransformationModuleImpl(
//    private val context: TealiumContext,
    private val adapter: TransformationsAdapter
) :
    TransformationModule,
    Transformer, TransformationsAdapter by adapter {
    override val name: String = TransformationModule.MODULE_NAME
    override var enabled: Boolean = true

    init {
        adapter.init()
    }
}

fun interface Transformation {
    suspend fun transform(dispatch: Dispatch)
}

val Tealium.transformations: TransformationModule?
    get() = modules.getModule(TransformationModule::class.java)

val Modules.Transformations: ModuleFactory
    get() = TransformationModule

@Deprecated("Use ")
var TealiumConfig.transformationsRuntime: JavascriptRuntime
    get() = options["js_runtime"] as? JavascriptRuntime
        ?: JavascriptRuntime.QuickJS
    set(value) {
        options["js_runtime"] = value
    }

var TealiumConfig.transformationsAdapter: TransformationsAdapterFactory?
    get() = options["js_adapter"] as? TransformationsAdapterFactory
    set(value) {
        value?.let {
            options["js_adapter"] = it
        } ?: run {
            options.remove("js_adapter")
        }
    }