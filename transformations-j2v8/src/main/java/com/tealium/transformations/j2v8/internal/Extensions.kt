package com.tealium.transformations.j2v8.internal

import com.tealium.core.TealiumContext
import com.tealium.transformations.internal.TransformationsAdapter
import com.tealium.transformations.internal.TransformationsAdapterFactory
import com.tealium.transformations.j2v8.J2v8TransformationsAdapter


val TransformationsAdapter.Factories.J2v8: TransformationsAdapterFactory
    get() = object : TransformationsAdapterFactory {
        override fun create(context: TealiumContext): TransformationsAdapter {
            return J2v8TransformationsAdapter(context)
        }
    }