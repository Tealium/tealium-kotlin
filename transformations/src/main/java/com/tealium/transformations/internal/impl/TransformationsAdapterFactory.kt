package com.tealium.transformations.internal.impl

import com.tealium.core.TealiumContext
import com.tealium.transformations.JavascriptRuntime
import com.tealium.transformations.internal.TransformationsAdapter

object TransformationsAdapterFactory {

    fun create(runtime: JavascriptRuntime, context: TealiumContext): TransformationsAdapter {
        return when (runtime) {
            JavascriptRuntime.J2v8 -> {
               J2v8TransformationsAdapter(context)
            }
            JavascriptRuntime.QuickJS -> {
                QuickJsTransformationsAdapter(context)
            }
            JavascriptRuntime.Rhino -> {
                RhinoTransformationsAdapter(context)
            }
        }
    }
}