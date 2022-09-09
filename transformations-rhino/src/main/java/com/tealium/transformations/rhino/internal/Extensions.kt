package com.tealium.transformations.rhino.internal

import com.tealium.core.TealiumContext
import com.tealium.transformations.internal.TransformationsAdapter
import com.tealium.transformations.internal.TransformationsAdapterFactory
import com.tealium.transformations.internal.impl.RhinoTransformationsAdapter

val TransformationsAdapter.Factories.Rhino: TransformationsAdapterFactory
    get() = object : TransformationsAdapterFactory {
        override fun create(context: TealiumContext): TransformationsAdapter {
            return RhinoTransformationsAdapter(context)
        }
    }