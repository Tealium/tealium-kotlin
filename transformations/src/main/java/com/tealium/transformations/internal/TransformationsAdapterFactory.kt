package com.tealium.transformations.internal

import com.tealium.core.TealiumContext
import com.tealium.transformations.JavascriptRuntime
import com.tealium.transformations.internal.TransformationsAdapter

interface TransformationsAdapterFactory {
    fun create(context: TealiumContext) : TransformationsAdapter
}