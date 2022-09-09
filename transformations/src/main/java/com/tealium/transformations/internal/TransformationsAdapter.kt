package com.tealium.transformations.internal

import com.tealium.dispatcher.Dispatch
import com.tealium.transformations.Transformation
import kotlinx.coroutines.Deferred

interface TransformationsAdapter {
    fun init(): Deferred<Unit>?
    fun executeJavascript(js: String): Any?
    suspend fun transform(dispatch: Dispatch)
    fun getTransformation(scope: String): Transformation

    object Factories { }
}