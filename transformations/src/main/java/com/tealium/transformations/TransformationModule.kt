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
import com.tealium.transformations.internal.impl.TransformationsAdapterFactory

interface TransformationModule : Module, TransformationsAdapter {

    companion object Factory : ModuleFactory {
        const val MODULE_NAME = "Transformations"

        override fun create(context: TealiumContext): TransformationModule {
            val runtime = context.config.transformationsRuntime
            return TransformationModuleImpl(
//                context,
                TransformationsAdapterFactory.create(runtime, context)
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

var TealiumConfig.transformationsRuntime: JavascriptRuntime
    get() = options["js_runtime"] as? JavascriptRuntime
        ?: JavascriptRuntime.QuickJS
    set(value) {
        options["js_runtime"] = value
    }
